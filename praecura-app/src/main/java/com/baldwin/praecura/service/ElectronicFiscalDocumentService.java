package com.baldwin.praecura.service;

import com.baldwin.praecura.entity.EcfStatus;
import com.baldwin.praecura.entity.ElectronicFiscalDocument;
import com.baldwin.praecura.entity.Invoice;
import com.baldwin.praecura.repository.ElectronicFiscalDocumentRepository;
import com.baldwin.praecura.repository.InvoiceRepository;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ElectronicFiscalDocumentService {

  private static final String SECURITY_CHARS = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ";

  private final ElectronicFiscalDocumentRepository repository;
  private final InvoiceRepository invoiceRepository;
  private final DgiiEcfClient dgiiEcfClient;
  private final SystemBrandingService systemBrandingService;
  private final SecureRandom secureRandom = new SecureRandom();

  @Value("${praecura.dgii.enabled:false}")
  private boolean dgiiEnabled;

  @Value("${praecura.dgii.verify-base-url:https://ecf.dgii.gov.do/ecf/ConsultaTimbre}")
  private String dgiiVerifyBaseUrl;

  @Value("${praecura.dgii.max-retries:5}")
  private int maxRetries;

  @Value("${praecura.dgii.retry-base-minutes:5}")
  private int retryBaseMinutes;

  public ElectronicFiscalDocumentService(ElectronicFiscalDocumentRepository repository,
                                         InvoiceRepository invoiceRepository,
                                         DgiiEcfClient dgiiEcfClient,
                                         SystemBrandingService systemBrandingService) {
    this.repository = repository;
    this.invoiceRepository = invoiceRepository;
    this.dgiiEcfClient = dgiiEcfClient;
    this.systemBrandingService = systemBrandingService;
  }

  public Optional<ElectronicFiscalDocument> findByInvoiceId(Long invoiceId) {
    if (invoiceId == null) return Optional.empty();
    return repository.findByInvoiceId(invoiceId);
  }

  public Page<ElectronicFiscalDocument> list(EcfStatus status, Pageable pageable) {
    if (status != null) {
      return repository.findByStatusOrderByCreatedAtDesc(status, pageable);
    }
    return repository.findAllByOrderByCreatedAtDesc(pageable);
  }

  public boolean isEnabled() {
    return dgiiEnabled;
  }

  @Transactional
  public ElectronicFiscalDocument prepareForInvoice(Long invoiceId) {
    Invoice invoice = invoiceRepository.findById(invoiceId)
        .orElseThrow(() -> new IllegalArgumentException("La factura no existe."));

    ElectronicFiscalDocument doc = repository.findByInvoiceId(invoiceId)
        .orElseGet(ElectronicFiscalDocument::new);

    if (doc.getId() == null) {
      doc.setInvoice(invoice);
      doc.setCreatedAt(LocalDateTime.now());
      doc.setCreatedBy(currentUsername());
    }

    String eNcf = normalize(invoice.getNcf());
    if (eNcf != null) {
      doc.setENcf(eNcf);
    }
    if (normalize(doc.getSecurityCode()) == null) {
      doc.setSecurityCode(generateSecurityCode(12));
    }
    if (normalize(doc.getSignedXml()) == null) {
      doc.setSignedXml(buildSignedXml(invoice, doc.getENcf(), doc.getSecurityCode()));
    }
    doc.setVerificationUrl(buildVerificationUrl(invoice, doc.getENcf(), doc.getSecurityCode()));

    if (doc.getStatus() == null) {
      doc.setStatus(EcfStatus.PENDING);
    }

    doc.setLastCheckedAt(LocalDateTime.now());
    doc.setUpdatedAt(LocalDateTime.now());
    doc.setUpdatedBy(currentUsername());
    repository.save(doc);
    return doc;
  }

  @Transactional
  public ElectronicFiscalDocument sendToDgii(Long invoiceId) {
    ensureEnabled();
    ElectronicFiscalDocument doc = prepareForInvoice(invoiceId);

    if (doc.getStatus() == EcfStatus.ACCEPTED
        || doc.getStatus() == EcfStatus.ACCEPTED_WITH_OBSERVATION
        || doc.getStatus() == EcfStatus.VOIDED) {
      return doc;
    }

    if (normalize(doc.getENcf()) == null) {
      throw new IllegalArgumentException("La factura no tiene e-NCF/NCF para enviar a DGII.");
    }

    doc.setStatus(EcfStatus.SIGNED);
    registerAttempt(doc);

    try {
      DgiiEcfClient.SubmitResult result = dgiiEcfClient.submit(doc);
      applySubmitResult(doc, result);
    } catch (Exception ex) {
      registerError(doc, "DGII_SEND_ERROR", ex.getMessage());
    }

    doc.setUpdatedAt(LocalDateTime.now());
    doc.setUpdatedBy(currentUsername());
    repository.save(doc);
    return doc;
  }

  @Transactional
  public ElectronicFiscalDocument syncStatus(Long invoiceId) {
    ensureEnabled();
    ElectronicFiscalDocument doc = prepareForInvoice(invoiceId);
    return syncStatus(doc);
  }

  @Transactional
  public ElectronicFiscalDocument updateStatus(Long invoiceId,
                                               EcfStatus status,
                                               String trackId,
                                               String statusCode,
                                               String message) {
    if (status == null) {
      throw new IllegalArgumentException("Debes indicar estado e-CF.");
    }
    ElectronicFiscalDocument doc = prepareForInvoice(invoiceId);

    doc.setStatus(status);
    doc.setDgiiTrackId(normalize(trackId));
    doc.setDgiiStatusCode(normalize(statusCode));
    doc.setDgiiMessage(normalize(message));
    doc.setLastCheckedAt(LocalDateTime.now());
    if (status == EcfStatus.SENT) {
      doc.setSentAt(LocalDateTime.now());
      doc.setNextRetryAt(LocalDateTime.now().plusMinutes(2));
    }
    if (status == EcfStatus.ACCEPTED || status == EcfStatus.ACCEPTED_WITH_OBSERVATION) {
      doc.setAcceptedAt(LocalDateTime.now());
      doc.setNextRetryAt(null);
      doc.setLastError(null);
    }
    if (status == EcfStatus.REJECTED || status == EcfStatus.VOIDED) {
      doc.setNextRetryAt(null);
    }

    doc.setUpdatedAt(LocalDateTime.now());
    doc.setUpdatedBy(currentUsername());
    repository.save(doc);
    return doc;
  }

  @Transactional
  public ProcessResult processAutomaticSync(int limit) {
    if (!dgiiEnabled) {
      return new ProcessResult(0, 0);
    }
    int synced = processPendingStatuses(limit);
    int retried = processRetryQueue(limit);
    return new ProcessResult(synced, retried);
  }

  @Transactional
  public int processPendingStatuses(int limit) {
    if (!dgiiEnabled) return 0;
    int take = sanitizeLimit(limit);
    List<ElectronicFiscalDocument> docs = repository.findTop200ByStatusInOrderByUpdatedAtAsc(List.of(EcfStatus.SENT));

    int processed = 0;
    for (ElectronicFiscalDocument doc : docs) {
      if (processed >= take) break;
      if (normalize(doc.getDgiiTrackId()) == null) continue;
      syncStatus(doc);
      processed++;
    }
    return processed;
  }

  @Transactional
  public int processRetryQueue(int limit) {
    if (!dgiiEnabled) return 0;
    int take = sanitizeLimit(limit);

    List<ElectronicFiscalDocument> docs = repository.findRetryable(
        List.of(EcfStatus.PENDING, EcfStatus.SIGNED, EcfStatus.ERROR),
        LocalDateTime.now(),
        PageRequest.of(0, take)
    );

    int processed = 0;
    for (ElectronicFiscalDocument doc : docs) {
      if (processed >= take) break;
      if (doc.getAttemptCount() >= maxRetries && doc.getStatus() == EcfStatus.ERROR) {
        continue;
      }
      if (doc.getInvoice() == null || doc.getInvoice().getId() == null) {
        continue;
      }
      sendToDgii(doc.getInvoice().getId());
      processed++;
    }
    return processed;
  }

  private ElectronicFiscalDocument syncStatus(ElectronicFiscalDocument doc) {
    if (doc == null) {
      throw new IllegalArgumentException("Documento e-CF no encontrado.");
    }
    if (normalize(doc.getDgiiTrackId()) == null) {
      throw new IllegalArgumentException("El documento e-CF no tiene TrackId para consulta.");
    }

    try {
      DgiiEcfClient.TrackResult result = dgiiEcfClient.track(doc);
      applyTrackResult(doc, result);
    } catch (Exception ex) {
      registerError(doc, "DGII_TRACK_ERROR", ex.getMessage());
    }

    doc.setUpdatedAt(LocalDateTime.now());
    doc.setUpdatedBy(currentUsername());
    repository.save(doc);
    return doc;
  }

  private void ensureEnabled() {
    if (!dgiiEnabled) {
      throw new IllegalArgumentException("La integración DGII está desactivada. Activa PRAECURA_DGII_ENABLED=true.");
    }
  }

  private void registerAttempt(ElectronicFiscalDocument doc) {
    doc.setAttemptCount(Math.max(0, doc.getAttemptCount()) + 1);
    doc.setLastAttemptAt(LocalDateTime.now());
  }

  private void applySubmitResult(ElectronicFiscalDocument doc, DgiiEcfClient.SubmitResult result) {
    if (result == null) {
      registerError(doc, "DGII_NO_RESPONSE", "DGII no devolvió respuesta al envío.");
      return;
    }

    doc.setDgiiTrackId(normalize(result.trackId()));
    EcfStatus status = result.status() != null ? result.status() : EcfStatus.ERROR;
    applyStatus(doc, status, result.statusCode(), result.message());
    if (status == EcfStatus.SENT) {
      doc.setSentAt(LocalDateTime.now());
    }
  }

  private void applyTrackResult(ElectronicFiscalDocument doc, DgiiEcfClient.TrackResult result) {
    if (result == null) {
      registerError(doc, "DGII_NO_TRACK_RESPONSE", "DGII no devolvió respuesta de estado.");
      return;
    }
    EcfStatus status = result.status() != null ? result.status() : EcfStatus.ERROR;
    applyStatus(doc, status, result.statusCode(), result.message());
  }

  private void applyStatus(ElectronicFiscalDocument doc,
                           EcfStatus status,
                           String statusCode,
                           String message) {
    doc.setStatus(status);
    doc.setDgiiStatusCode(normalize(statusCode));
    doc.setDgiiMessage(normalize(message));
    doc.setLastCheckedAt(LocalDateTime.now());

    if (status == EcfStatus.ACCEPTED || status == EcfStatus.ACCEPTED_WITH_OBSERVATION) {
      doc.setAcceptedAt(LocalDateTime.now());
      doc.setNextRetryAt(null);
      doc.setLastError(null);
      return;
    }

    if (status == EcfStatus.REJECTED || status == EcfStatus.VOIDED) {
      doc.setNextRetryAt(null);
      doc.setLastError(normalize(message));
      return;
    }

    if (status == EcfStatus.SENT) {
      doc.setNextRetryAt(LocalDateTime.now().plusMinutes(2));
      doc.setLastError(null);
      return;
    }

    if (status == EcfStatus.ERROR || status == EcfStatus.SIGNED || status == EcfStatus.PENDING) {
      scheduleRetry(doc, normalize(message));
    }
  }

  private void registerError(ElectronicFiscalDocument doc, String code, String message) {
    doc.setStatus(EcfStatus.ERROR);
    doc.setDgiiStatusCode(normalize(code));
    doc.setDgiiMessage(normalize(message));
    doc.setLastCheckedAt(LocalDateTime.now());
    scheduleRetry(doc, message);
  }

  private void scheduleRetry(ElectronicFiscalDocument doc, String error) {
    String normalizedError = normalize(error);
    doc.setLastError(normalizedError);

    int attempts = Math.max(1, doc.getAttemptCount());
    if (attempts >= Math.max(1, maxRetries)) {
      doc.setNextRetryAt(null);
      return;
    }

    int base = Math.max(1, retryBaseMinutes);
    int multiplier = 1 << Math.min(10, attempts - 1);
    int minutes = Math.min(24 * 60, base * multiplier);
    doc.setNextRetryAt(LocalDateTime.now().plusMinutes(minutes));
  }

  private int sanitizeLimit(int limit) {
    if (limit <= 0) return 30;
    return Math.min(limit, 200);
  }

  private String buildSignedXml(Invoice invoice, String eNcf, String securityCode) {
    if (invoice == null) return null;
    String invoiceId = invoice.getId() != null ? String.valueOf(invoice.getId()) : "";
    String total = invoice.getTotal() != null ? invoice.getTotal().toPlainString() : "0.00";
    String issuedAt = invoice.getIssuedAt() != null ? invoice.getIssuedAt().toString() : LocalDateTime.now().toString();
    String ncf = normalize(eNcf) != null ? normalize(eNcf) : "";
    String sec = normalize(securityCode) != null ? normalize(securityCode) : "";

    return "<eCF invoiceId=\"" + invoiceId + "\" eNCF=\"" + ncf + "\" issuedAt=\"" + issuedAt + "\" total=\"" + total + "\" securityCode=\"" + sec + "\"/>";
  }

  private String buildVerificationUrl(Invoice invoice, String eNcf, String securityCode) {
    String base = normalize(dgiiVerifyBaseUrl);
    if (base == null) return null;
    String encfValue = normalize(eNcf);
    String sec = normalize(securityCode);
    if (encfValue == null || sec == null) return base;

    // When key fiscal fields are available, generate a DGII-style verification URL.
    String issuerRnc = normalize(systemBrandingService.load().companyRnc());
    String legacy = base + "?encf=" + url(encfValue) + "&sc=" + url(sec);
    if (invoice == null || issuerRnc == null || invoice.getCreatedAt() == null || invoice.getTotal() == null || invoice.getTax() == null) {
      return legacy;
    }

    DateTimeFormatter issueFmt = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    DateTimeFormatter signFmt = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
    String issueDate = invoice.getCreatedAt().toLocalDate().format(issueFmt);
    LocalDateTime signDate = invoice.getIssuedAt() != null ? invoice.getIssuedAt() : invoice.getCreatedAt();
    String securityShort = sec.length() > 6 ? sec.substring(0, 6) : sec;

    return base
        + "?RncEmisor=" + url(issuerRnc)
        + "&eNCF=" + url(encfValue)
        + "&FechaEmision=" + url(issueDate)
        + "&MontoTotal=" + url(formatAmount(invoice.getTotal()))
        + "&ImpuestoTotal=" + url(formatAmount(invoice.getTax()))
        + "&FechaFirma=" + url(signDate.format(signFmt))
        + "&CodigoSeguridad=" + url(securityShort);
  }

  private String formatAmount(java.math.BigDecimal amount) {
    if (amount == null) return "0.00";
    DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
    DecimalFormat df = new DecimalFormat("0.00", symbols);
    return df.format(amount.setScale(2, RoundingMode.HALF_UP));
  }

  private String url(String value) {
    if (value == null) return "";
    return URLEncoder.encode(value, StandardCharsets.UTF_8);
  }

  private String generateSecurityCode(int length) {
    int size = Math.max(6, Math.min(length, 32));
    StringBuilder sb = new StringBuilder(size);
    for (int i = 0; i < size; i++) {
      int idx = secureRandom.nextInt(SECURITY_CHARS.length());
      sb.append(SECURITY_CHARS.charAt(idx));
    }
    return sb.toString();
  }

  private String normalize(String value) {
    if (value == null) return null;
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }

  private String currentUsername() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth != null && auth.isAuthenticated()) {
      return auth.getName();
    }
    return "system";
  }

  public record ProcessResult(int syncedCount, int retriedCount) {
  }
}
