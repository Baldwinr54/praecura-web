package com.baldwin.praecura.service;

import com.baldwin.praecura.entity.EcfStatus;
import com.baldwin.praecura.entity.ElectronicFiscalDocument;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@ConditionalOnProperty(prefix = "praecura.dgii", name = "mode", havingValue = "live")
public class DgiiEcfLiveClient implements DgiiEcfClient {

  private final RestClient restClient;

  @Value("${praecura.dgii.submit-url:}")
  private String submitUrl;

  @Value("${praecura.dgii.track-url:}")
  private String trackUrl;

  @Value("${praecura.dgii.api-key:}")
  private String apiKey;

  public DgiiEcfLiveClient(RestClient.Builder builder) {
    this.restClient = builder.build();
  }

  @Override
  public SubmitResult submit(ElectronicFiscalDocument document) {
    if (isBlank(submitUrl)) {
      throw new IllegalStateException("Configura praecura.dgii.submit-url para el modo live.");
    }

    Map<String, Object> payload = new HashMap<>();
    payload.put("invoiceId", document.getInvoice() != null ? document.getInvoice().getId() : null);
    payload.put("eNcf", document.getENcf());
    payload.put("securityCode", document.getSecurityCode());
    payload.put("signedXml", document.getSignedXml());
    payload.put("total", document.getInvoice() != null ? document.getInvoice().getTotal() : BigDecimal.ZERO);

    DgiiApiResponse response = restClient.post()
        .uri(submitUrl)
        .headers(h -> {
          if (!isBlank(apiKey)) h.set("X-API-Key", apiKey);
        })
        .body(payload)
        .retrieve()
        .body(DgiiApiResponse.class);

    if (response == null) {
      return new SubmitResult(null, EcfStatus.ERROR, "NO_RESPONSE", "DGII no devolvió respuesta.");
    }

    return new SubmitResult(
        safe(response.trackId()),
        parseStatus(response.status()),
        safe(response.statusCode()),
        safe(response.message())
    );
  }

  @Override
  public TrackResult track(ElectronicFiscalDocument document) {
    if (isBlank(trackUrl)) {
      throw new IllegalStateException("Configura praecura.dgii.track-url para el modo live.");
    }
    if (document == null || isBlank(document.getDgiiTrackId())) {
      throw new IllegalArgumentException("El documento no tiene TrackId para consultar.");
    }

    String url = trackUrl;
    if (url.contains("{trackId}")) {
      url = url.replace("{trackId}", document.getDgiiTrackId());
    } else {
      String sep = url.contains("?") ? "&" : "?";
      url = url + sep + "trackId=" + document.getDgiiTrackId();
    }

    DgiiApiResponse response = restClient.get()
        .uri(url)
        .headers(h -> {
          if (!isBlank(apiKey)) h.set("X-API-Key", apiKey);
        })
        .retrieve()
        .body(DgiiApiResponse.class);

    if (response == null) {
      return new TrackResult(EcfStatus.ERROR, "NO_RESPONSE", "DGII no devolvió respuesta.");
    }

    return new TrackResult(
        parseStatus(response.status()),
        safe(response.statusCode()),
        safe(response.message())
    );
  }

  private EcfStatus parseStatus(String raw) {
    String value = safe(raw);
    if (value == null) return EcfStatus.ERROR;
    try {
      return EcfStatus.valueOf(value.toUpperCase());
    } catch (Exception ex) {
      return EcfStatus.ERROR;
    }
  }

  private String safe(String value) {
    if (value == null) return null;
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }

  private boolean isBlank(String value) {
    return safe(value) == null;
  }

  private record DgiiApiResponse(String trackId, String status, String statusCode, String message) {
  }
}
