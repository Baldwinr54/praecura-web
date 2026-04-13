package com.baldwin.praecura.service;

import com.baldwin.praecura.entity.Invoice;
import com.baldwin.praecura.entity.InvoiceStatus;
import com.baldwin.praecura.entity.PaymentLink;
import com.baldwin.praecura.entity.PaymentLinkProvider;
import com.baldwin.praecura.entity.PaymentLinkStatus;
import com.baldwin.praecura.entity.PaymentChannel;
import com.baldwin.praecura.entity.PaymentMethod;
import com.baldwin.praecura.repository.InvoiceRepository;
import com.baldwin.praecura.repository.PaymentLinkRepository;
import com.baldwin.praecura.repository.PaymentRepository;
import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentLinkService {

  private static final Pattern SENSITIVE_CARD_KEYWORDS = Pattern.compile(
      "\\b(cvv|cvc|cvv2|security\\s*code|codigo\\s*de\\s*seguridad|card\\s*number|numero\\s*de\\s*tarjeta|pan)\\b",
      Pattern.CASE_INSENSITIVE
  );

  private final PaymentLinkRepository paymentLinkRepository;
  private final InvoiceRepository invoiceRepository;
  private final PaymentRepository paymentRepository;
  private final BillingService billingService;
  private final CardNetClient cardNetClient;
  private final AuditService auditService;

  @Value("${praecura.public-base-url:}")
  private String publicBaseUrl;

  public PaymentLinkService(PaymentLinkRepository paymentLinkRepository,
                            InvoiceRepository invoiceRepository,
                            PaymentRepository paymentRepository,
                            BillingService billingService,
                            CardNetClient cardNetClient,
                            AuditService auditService) {
    this.paymentLinkRepository = paymentLinkRepository;
    this.invoiceRepository = invoiceRepository;
    this.paymentRepository = paymentRepository;
    this.billingService = billingService;
    this.cardNetClient = cardNetClient;
    this.auditService = auditService;
  }

  public List<PaymentLink> listByInvoice(Long invoiceId) {
    return paymentLinkRepository.findByInvoiceIdOrderByCreatedAtDesc(invoiceId);
  }

  @Transactional
  public PaymentLink createAzulLink(Long invoiceId, String url, String reference, String notes) {
    Invoice invoice = getInvoice(invoiceId);
    String normalizedUrl = normalizeExternalUrl(url);
    String safeReference = sanitizeField("Referencia de link", reference, 120);
    String safeNotes = sanitizeField("Notas del link", notes, 500);

    PaymentLink link = new PaymentLink();
    link.setInvoice(invoice);
    link.setProvider(PaymentLinkProvider.AZUL_LINK);
    link.setStatus(PaymentLinkStatus.CREATED);
    link.setAmount(invoice.getBalance());
    link.setCurrency(invoice.getCurrency());
    link.setUrl(normalizedUrl);
    link.setExternalId(safeReference);
    link.setNotes(safeNotes);
    link.setCreatedBy(currentUsername());
    link.setUpdatedAt(LocalDateTime.now());
    paymentLinkRepository.save(link);

    auditService.log("PAYMENT_LINK_CREATED",
        "PaymentLink",
        link.getId(),
        "provider=AZUL, invoiceId=" + invoiceId + ", amount=" + invoice.getBalance());

    return link;
  }

  @Transactional
  public PaymentLink createCardNetSession(Long invoiceId) {
    Invoice invoice = getInvoice(invoiceId);
    BigDecimal amount = invoice.getBalance();
    if (amount.compareTo(BigDecimal.ZERO) <= 0) {
      throw new IllegalArgumentException("La factura no tiene balance pendiente.");
    }

    String base = resolvePublicBaseUrl();
    String transactionId = cardNetClient.generateTransactionId(invoiceId);
    String returnUrl = base + "/billing/cardnet/return";
    String cancelUrl = base + "/billing/cardnet/cancel";

    CardNetClient.SessionResponse session = cardNetClient.createSession(new CardNetClient.SessionRequest(
        amount,
        BigDecimal.ZERO,
        String.valueOf(invoiceId),
        String.valueOf(invoiceId),
        transactionId,
        returnUrl,
        cancelUrl,
        "Santo Domingo",
        "Distrito Nacional",
        "DO"
    ));

    PaymentLink link = new PaymentLink();
    link.setInvoice(invoice);
    link.setProvider(PaymentLinkProvider.CARDNET_BOTON);
    link.setStatus(PaymentLinkStatus.CREATED);
    link.setAmount(amount);
    link.setCurrency(invoice.getCurrency());
    link.setSessionId(session.sessionId());
    link.setSessionKey(session.sessionKey());
    link.setCreatedBy(currentUsername());
    link.setUpdatedAt(LocalDateTime.now());
    paymentLinkRepository.save(link);

    auditService.log("PAYMENT_LINK_CREATED",
        "PaymentLink",
        link.getId(),
        "provider=CARDNET, invoiceId=" + invoiceId + ", amount=" + amount);

    return link;
  }

  @Transactional
  public PaymentLink markPaid(Long linkId, PaymentMethod method, String reference) {
    PaymentLink link = paymentLinkRepository.findById(linkId)
        .orElseThrow(() -> new IllegalArgumentException("El link no existe."));

    Invoice invoice = link.getInvoice();
    if (invoice == null) {
      throw new IllegalArgumentException("El link no tiene factura asociada.");
    }

    if (link.getStatus() == PaymentLinkStatus.PAID) {
      return link;
    }

    billingService.recordPayment(invoice.getId(), new BillingService.PaymentRequest(
        link.getAmount(),
        null,
        null,
        method,
        PaymentChannel.ONLINE_LINK,
        link.getProvider().name(),
        reference,
        null,
        null,
        null,
        null,
        null,
        null,
        "Pago registrado desde link " + link.getProvider().label()
    ));

    link.setStatus(PaymentLinkStatus.PAID);
    link.setUpdatedAt(LocalDateTime.now());
    paymentLinkRepository.save(link);

    return link;
  }

  @Transactional
  public PaymentLink handleCardNetReturn(String sessionId) {
    PaymentLink link = paymentLinkRepository.findBySessionId(sessionId)
        .orElseThrow(() -> new IllegalArgumentException("No se encontró link para la sesión."));

    CardNetClient.SessionStatusResponse status = cardNetClient.querySession(sessionId, link.getSessionKey());
    String responseCode = status.responseCode();

    if ("00".equals(responseCode)) {
      String externalId = status.retrievalReference();
      if (externalId != null && paymentRepository.existsByExternalId(externalId)) {
        link.setStatus(PaymentLinkStatus.PAID);
      } else {
        billingService.recordPayment(link.getInvoice().getId(), new BillingService.PaymentRequest(
            link.getAmount(),
            null,
            null,
            PaymentMethod.CARD,
            PaymentChannel.ONLINE_LINK,
            "CARDNET",
            externalId,
            status.approvalCode(),
            last4(status.maskedCard()),
            null,
            null,
            null,
            status.retrievalReference(),
            "Pago CardNET aprobado"
        ));
        link.setStatus(PaymentLinkStatus.PAID);
      }
    } else {
      link.setStatus(PaymentLinkStatus.FAILED);
    }

    link.setUpdatedAt(LocalDateTime.now());
    paymentLinkRepository.save(link);
    return link;
  }

  @Transactional
  public PaymentLink cancelCardNet(String sessionId) {
    PaymentLink link = paymentLinkRepository.findBySessionId(sessionId)
        .orElseThrow(() -> new IllegalArgumentException("No se encontró link para la sesión."));
    link.setStatus(PaymentLinkStatus.CANCELLED);
    link.setUpdatedAt(LocalDateTime.now());
    paymentLinkRepository.save(link);
    return link;
  }

  private Invoice getInvoice(Long invoiceId) {
    Invoice invoice = invoiceRepository.findWithItems(invoiceId)
        .orElseThrow(() -> new IllegalArgumentException("La factura no existe."));
    if (invoice.getStatus() == InvoiceStatus.VOID) {
      throw new IllegalArgumentException("La factura está anulada.");
    }
    return invoice;
  }

  private String resolvePublicBaseUrl() {
    if (publicBaseUrl != null && !publicBaseUrl.isBlank()) {
      return publicBaseUrl.replaceAll("/+$", "");
    }
    throw new IllegalStateException("Configura praecura.public-base-url para recibir callbacks de CardNET.");
  }

  private String currentUsername() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth != null && auth.isAuthenticated()) {
      return auth.getName();
    }
    return null;
  }

  private String last4(String masked) {
    if (masked == null) return null;
    String digits = masked.replaceAll("\\D", "");
    if (digits.length() <= 4) return digits;
    return digits.substring(digits.length() - 4);
  }

  private String sanitizeField(String label, String value, int maxLength) {
    String trimmed = trimToNull(value);
    if (trimmed == null) return null;
    if (trimmed.length() > maxLength) {
      throw new IllegalArgumentException(label + " excede longitud máxima (" + maxLength + ").");
    }
    if (SENSITIVE_CARD_KEYWORDS.matcher(trimmed).find()) {
      throw new IllegalArgumentException(label + " contiene términos sensibles de tarjeta y no puede almacenarse.");
    }
    if (containsPotentialPan(trimmed)) {
      throw new IllegalArgumentException(label + " parece contener un número de tarjeta completo.");
    }
    return trimmed;
  }

  private String normalizeExternalUrl(String url) {
    String normalized = trimToNull(url);
    if (normalized == null) {
      throw new IllegalArgumentException("La URL del link es obligatoria.");
    }
    if (normalized.length() > 4000) {
      throw new IllegalArgumentException("La URL del link excede longitud máxima.");
    }
    URI uri;
    try {
      uri = URI.create(normalized);
    } catch (Exception ex) {
      throw new IllegalArgumentException("La URL del link no es válida.");
    }
    String scheme = uri.getScheme() != null ? uri.getScheme().toLowerCase() : "";
    if (!"https".equals(scheme) && !"http".equals(scheme)) {
      throw new IllegalArgumentException("La URL del link debe usar http o https.");
    }
    if (uri.getHost() == null || uri.getHost().isBlank()) {
      throw new IllegalArgumentException("La URL del link debe incluir un dominio válido.");
    }
    return normalized;
  }

  private String trimToNull(String value) {
    if (value == null) return null;
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }

  private boolean containsPotentialPan(String value) {
    String digits = value.replaceAll("\\D", "");
    if (digits.length() < 13 || digits.length() > 19) {
      return false;
    }
    return luhnValid(digits);
  }

  private boolean luhnValid(String digits) {
    int sum = 0;
    boolean alternate = false;
    for (int i = digits.length() - 1; i >= 0; i--) {
      int n = digits.charAt(i) - '0';
      if (alternate) {
        n *= 2;
        if (n > 9) n -= 9;
      }
      sum += n;
      alternate = !alternate;
    }
    return sum % 10 == 0;
  }
}
