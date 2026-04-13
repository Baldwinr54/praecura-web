package com.baldwin.praecura.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpMethod;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class CardNetClient {

  private final RestTemplate restTemplate;

  @Value("${praecura.cardnet.enabled:false}")
  private boolean enabled;

  @Value("${praecura.cardnet.base-url:}")
  private String baseUrl;

  @Value("${praecura.cardnet.authorize-url:}")
  private String authorizeUrl;

  @Value("${praecura.cardnet.merchant-number:}")
  private String merchantNumber;

  @Value("${praecura.cardnet.merchant-terminal:}")
  private String merchantTerminal;

  @Value("${praecura.cardnet.acquiring-institution-code:}")
  private String acquiringInstitutionCode;

  @Value("${praecura.cardnet.merchant-name:}")
  private String merchantName;

  @Value("${praecura.cardnet.merchant-type:}")
  private String merchantType;

  @Value("${praecura.cardnet.currency-code:214}")
  private String currencyCode;

  @Value("${praecura.cardnet.page-language:ESP}")
  private String pageLanguage;

  public CardNetClient(
      @Value("${praecura.cardnet.connect-timeout-ms:4000}") int connectTimeoutMs,
      @Value("${praecura.cardnet.read-timeout-ms:10000}") int readTimeoutMs) {
    SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
    factory.setConnectTimeout(connectTimeoutMs);
    factory.setReadTimeout(readTimeoutMs);
    this.restTemplate = new RestTemplate(factory);
  }

  public boolean isEnabled() {
    return enabled;
  }

  public SessionResponse createSession(SessionRequest request) {
    requireConfigured();

    String sessionEndpoint = baseUrl.endsWith("/") ? baseUrl + "sessions" : baseUrl + "/sessions";

    Map<String, Object> body = new HashMap<>();
    body.put("MerchantName", merchantName);
    body.put("MerchantNumber", merchantNumber);
    body.put("MerchantTerminal", merchantTerminal);
    body.put("MerchantType", merchantType);
    body.put("AcquiringInstitutionCode", acquiringInstitutionCode);
    body.put("TransactionType", "0200");
    body.put("CurrencyCode", currencyCode);
    body.put("TransactionId", request.transactionId());
    body.put("Amount", toCardNetAmount(request.amount()));
    body.put("Taxes", toCardNetAmount(request.taxes()));
    body.put("OrderId", request.orderId());
    body.put("ReturnUrl", request.returnUrl());
    body.put("CancelUrl", request.cancelUrl());
    body.put("PageLanguaje", pageLanguage);
    body.put("InvoiceNumber", request.invoiceNumber());
    body.put("MerchantCity", request.merchantCity());
    body.put("MerchantCountry", request.merchantCountry());
    body.put("MerchantState", request.merchantState());

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
    ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
        URI.create(sessionEndpoint),
        HttpMethod.POST,
        entity,
        new ParameterizedTypeReference<>() {}
    );

    Map<String, Object> payload = response.getBody();
    if (payload == null) {
      throw new IllegalStateException("Respuesta vacía de CardNET.");
    }

    Object session = payload.get("SESSION");
    Object sessionKey = payload.get("SESSION_KEY");
    if (session == null || sessionKey == null) {
      throw new IllegalStateException("CardNET no devolvió SESSION/SESSION_KEY.");
    }

    return new SessionResponse(String.valueOf(session), String.valueOf(sessionKey));
  }

  public SessionStatusResponse querySession(String sessionId, String sessionKey) {
    requireConfigured();

    String sessionEndpoint = baseUrl.endsWith("/") ? baseUrl + "sessions/" + sessionId
        : baseUrl + "/sessions/" + sessionId;

    URI uri = URI.create(sessionEndpoint + "?sk=" + sessionKey);
    ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
        uri,
        HttpMethod.GET,
        HttpEntity.EMPTY,
        new ParameterizedTypeReference<>() {}
    );
    Map<String, Object> payload = response.getBody();
    if (payload == null) {
      throw new IllegalStateException("Respuesta vacía de CardNET.");
    }

    return new SessionStatusResponse(payload);
  }

  public String buildAuthorizeUrl() {
    if (authorizeUrl == null || authorizeUrl.isBlank()) {
      return baseUrl.endsWith("/") ? baseUrl + "authorize" : baseUrl + "/authorize";
    }
    return authorizeUrl;
  }

  public String generateTransactionId(Long invoiceId) {
    String base = String.valueOf(invoiceId == null ? 0 : invoiceId);
    String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 6);
    String merged = base + suffix;
    return merged.substring(0, Math.min(6, merged.length()));
  }

  private String toCardNetAmount(BigDecimal amount) {
    if (amount == null) return "000000000000";
    BigDecimal scaled = amount.setScale(2, RoundingMode.HALF_UP);
    String cents = scaled.movePointRight(2).setScale(0, RoundingMode.HALF_UP).toPlainString();
    return String.format("%012d", Long.parseLong(cents));
  }

  private void requireConfigured() {
    if (!enabled) {
      throw new IllegalStateException("CardNET no está habilitado.");
    }
    if (baseUrl == null || baseUrl.isBlank()) {
      throw new IllegalStateException("CardNET base-url no configurado.");
    }
    if (merchantNumber == null || merchantNumber.isBlank() || merchantTerminal == null || merchantTerminal.isBlank()) {
      throw new IllegalStateException("CardNET merchant-number/merchant-terminal no configurados.");
    }
  }

  public record SessionRequest(BigDecimal amount,
                               BigDecimal taxes,
                               String orderId,
                               String invoiceNumber,
                               String transactionId,
                               String returnUrl,
                               String cancelUrl,
                               String merchantCity,
                               String merchantState,
                               String merchantCountry) {}

  public record SessionResponse(String sessionId, String sessionKey) {}

  public record SessionStatusResponse(Map<String, Object> payload) {
    public String responseCode() {
      Object v = payload.get("ResponseCode");
      return v == null ? null : String.valueOf(v);
    }

    public String approvalCode() {
      Object v = payload.get("ApprovalCode");
      return v == null ? null : String.valueOf(v);
    }

    public String retrievalReference() {
      Object v = payload.get("RetrievalReferenceNumber");
      return v == null ? null : String.valueOf(v);
    }

    public String maskedCard() {
      Object v = payload.get("CreditCardNumber");
      return v == null ? null : String.valueOf(v);
    }
  }
}
