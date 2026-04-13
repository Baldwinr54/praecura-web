package com.baldwin.praecura.service;

import com.baldwin.praecura.entity.Invoice;
import com.baldwin.praecura.entity.ElectronicFiscalDocument;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class BillingPresentationService {

  public String normalizePrintLayout(String layout) {
    if (layout == null) return "ticket";
    String normalized = layout.trim().toLowerCase();
    if ("a4".equals(normalized)) return "a4";
    return "ticket";
  }

  public String resolveFiscalDocumentNumber(Invoice invoice, ElectronicFiscalDocument ecf) {
    if (ecf != null && normalizeText(ecf.getENcf()) != null) {
      return normalizeText(ecf.getENcf());
    }
    return normalizeText(invoice != null ? invoice.getNcf() : null);
  }

  public String resolveFiscalVerificationUrl(ElectronicFiscalDocument ecf) {
    if (ecf != null && ecf.getVerificationUrl() != null && !ecf.getVerificationUrl().isBlank()) {
      return ecf.getVerificationUrl().trim();
    }
    // Si no existe URL oficial de verificación, no renderizamos QR para evitar
    // mostrar códigos no fiscales o datos internos en el comprobante del paciente.
    return null;
  }

  public String buildQrImageUrl(String payload) {
    if (payload == null || payload.isBlank()) return null;
    String encoded = URLEncoder.encode(payload, StandardCharsets.UTF_8);
    return "https://api.qrserver.com/v1/create-qr-code/?size=180x180&margin=0&ecc=M&data=" + encoded;
  }

  public String normalizeQuery(String q) {
    if (q == null) return null;
    String trimmed = q.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }

  public AccountsReceivableSummary buildAccountsReceivableSummary(List<Invoice> invoices, LocalDateTime now) {
    List<Invoice> safeInvoices = invoices != null ? invoices : List.of();
    BigDecimal totalBalance = BigDecimal.ZERO;
    long totalInvoices = 0;

    long count0To30 = 0;
    long count31To60 = 0;
    long count61To90 = 0;
    long count91Plus = 0;

    BigDecimal amount0To30 = BigDecimal.ZERO;
    BigDecimal amount31To60 = BigDecimal.ZERO;
    BigDecimal amount61To90 = BigDecimal.ZERO;
    BigDecimal amount91Plus = BigDecimal.ZERO;

    for (Invoice invoice : safeInvoices) {
      if (invoice == null) continue;
      BigDecimal balance = safeAmount(invoice.getBalance());
      if (balance.compareTo(BigDecimal.ZERO) <= 0) continue;

      totalInvoices++;
      totalBalance = totalBalance.add(balance);

      int age = ageDays(invoice, now);
      if (age <= 30) {
        count0To30++;
        amount0To30 = amount0To30.add(balance);
      } else if (age <= 60) {
        count31To60++;
        amount31To60 = amount31To60.add(balance);
      } else if (age <= 90) {
        count61To90++;
        amount61To90 = amount61To90.add(balance);
      } else {
        count91Plus++;
        amount91Plus = amount91Plus.add(balance);
      }
    }

    List<ReceivableBucket> buckets = List.of(
        new ReceivableBucket("0_30", "0-30 días", count0To30, amount0To30),
        new ReceivableBucket("31_60", "31-60 días", count31To60, amount31To60),
        new ReceivableBucket("61_90", "61-90 días", count61To90, amount61To90),
        new ReceivableBucket("91_plus", "91+ días", count91Plus, amount91Plus)
    );

    return new AccountsReceivableSummary(totalInvoices, totalBalance, buckets);
  }

  public int ageDays(Invoice invoice, LocalDateTime now) {
    if (invoice == null || invoice.getCreatedAt() == null) return 0;
    LocalDateTime base = now != null ? now : LocalDateTime.now();
    long days = Duration.between(invoice.getCreatedAt(), base).toDays();
    return (int) Math.max(0, days);
  }

  public String agingLabel(int ageDays) {
    if (ageDays <= 30) return "0-30 días";
    if (ageDays <= 60) return "31-60 días";
    if (ageDays <= 90) return "61-90 días";
    return "91+ días";
  }

  public String commitmentTimingLabel(LocalDateTime promisedDate, LocalDateTime now) {
    if (promisedDate == null) return "Sin fecha";
    LocalDateTime base = now != null ? now : LocalDateTime.now();
    long days = Duration.between(base.toLocalDate().atStartOfDay(), promisedDate.toLocalDate().atStartOfDay()).toDays();
    if (days == 0) return "Vence hoy";
    if (days > 0) return "Vence en " + days + " día(s)";
    return "Atrasado " + Math.abs(days) + " día(s)";
  }

  private BigDecimal safeAmount(BigDecimal value) {
    return value != null ? value : BigDecimal.ZERO;
  }

  private String normalizeText(String value) {
    if (value == null) return null;
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }

  public record AccountsReceivableSummary(
      long totalInvoices,
      BigDecimal totalBalance,
      List<ReceivableBucket> buckets
  ) {
  }

  public record ReceivableBucket(
      String code,
      String label,
      long count,
      BigDecimal amount
  ) {
  }
}
