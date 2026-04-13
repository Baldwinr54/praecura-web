package com.baldwin.praecura.service;

import com.baldwin.praecura.entity.AuditLog;
import com.baldwin.praecura.entity.CashSession;
import com.baldwin.praecura.entity.FiscalSequence;
import com.baldwin.praecura.entity.Invoice;
import com.baldwin.praecura.entity.InvoiceStatus;
import com.baldwin.praecura.repository.AuditLogRepository;
import com.baldwin.praecura.repository.CashSessionRepository;
import com.baldwin.praecura.repository.FiscalSequenceRepository;
import com.baldwin.praecura.repository.InvoiceRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
public class OperationalComplianceService {

  private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
  private static final List<String> CRITICAL_ACTIONS = List.of(
      "PAYMENT_REFUNDED",
      "INVOICE_VOIDED",
      "CREDIT_NOTE_CREATED",
      "NCF_ASSIGNED",
      "CASH_SESSION_CLOSED",
      "FISCAL_SEQUENCE_CREATED",
      "FISCAL_SEQUENCE_DEACTIVATED"
  );

  private final AuditLogRepository auditLogRepository;
  private final InvoiceRepository invoiceRepository;
  private final CashSessionRepository cashSessionRepository;
  private final FiscalSequenceRepository fiscalSequenceRepository;

  @Value("${praecura.compliance.overdue-balance-days:30}")
  private int overdueBalanceDays;

  @Value("${praecura.compliance.cash-open-max-hours:16}")
  private int cashOpenMaxHours;

  @Value("${praecura.compliance.fiscal-expiry-warning-days:30}")
  private int fiscalExpiryWarningDays;

  @Value("${praecura.compliance.fiscal-remaining-warning:25}")
  private long fiscalRemainingWarning;

  @Value("${praecura.compliance.findings-sample:25}")
  private int findingsSample;

  public OperationalComplianceService(AuditLogRepository auditLogRepository,
                                      InvoiceRepository invoiceRepository,
                                      CashSessionRepository cashSessionRepository,
                                      FiscalSequenceRepository fiscalSequenceRepository) {
    this.auditLogRepository = auditLogRepository;
    this.invoiceRepository = invoiceRepository;
    this.cashSessionRepository = cashSessionRepository;
    this.fiscalSequenceRepository = fiscalSequenceRepository;
  }

  public ComplianceSnapshot load(LocalDate fromDate,
                                 LocalDate toDate,
                                 String username,
                                 int page,
                                 int size) {
    return load(fromDate, toDate, username, page, size, findingsSample);
  }

  public ComplianceSnapshot load(LocalDate fromDate,
                                 LocalDate toDate,
                                 String username,
                                 int page,
                                 int size,
                                 int findingLimit) {
    LocalDate today = LocalDate.now();
    LocalDate from = (fromDate != null) ? fromDate : today.minusDays(30);
    LocalDate to = (toDate != null) ? toDate : today;
    if (to.isBefore(from)) {
      LocalDate swap = from;
      from = to;
      to = swap;
    }

    String usernameFilter = normalize(username);
    int safePage = Math.max(0, page);
    int safeSize = clamp(size, 20, 1, 200);
    int safeFindingLimit = clamp(findingLimit, 25, 1, 250);
    int overdueDays = Math.max(1, overdueBalanceDays);
    int openHours = Math.max(1, cashOpenMaxHours);
    int expiryDays = Math.max(1, fiscalExpiryWarningDays);
    long remainingWarning = Math.max(1, fiscalRemainingWarning);

    LocalDateTime fromDt = from.atStartOfDay();
    LocalDateTime toDt = to.plusDays(1).atStartOfDay();
    LocalDateTime now = LocalDateTime.now();
    LocalDateTime overdueBefore = now.minusDays(overdueDays);
    LocalDateTime openBefore = now.minusHours(openHours);

    Page<AuditLog> criticalEventsPage = auditLogRepository.searchByActions(
        CRITICAL_ACTIONS,
        usernameFilter,
        fromDt,
        toDt,
        PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"))
    );

    long criticalActionsCount = criticalEventsPage.getTotalElements();
    long pendingNcfCount = invoiceRepository.countPendingNcf(InvoiceStatus.VOID);
    long overdueBalanceCount = invoiceRepository.countOverdueBalance(InvoiceStatus.VOID, overdueBefore);
    long collectedWithoutNcfCount = invoiceRepository.countCollectedWithoutNcf(InvoiceStatus.VOID);

    List<Invoice> pendingNcf = invoiceRepository.findPendingNcf(InvoiceStatus.VOID, PageRequest.of(0, safeFindingLimit));
    List<Invoice> overdueBalances = invoiceRepository.findOverdueBalances(InvoiceStatus.VOID, overdueBefore, PageRequest.of(0, safeFindingLimit));
    List<CashSession> longOpenSessions = cashSessionRepository.findOpenBefore(openBefore);
    List<FiscalSequence> activeSequences = fiscalSequenceRepository.findByActiveTrueOrderByTypeCodeAsc();

    List<ComplianceFinding> findings = new ArrayList<>();

    for (Invoice invoice : pendingNcf) {
      boolean collected = invoice.getStatus() == InvoiceStatus.PAID || invoice.getStatus() == InvoiceStatus.PARTIALLY_PAID;
      findings.add(new ComplianceFinding(
          collected ? "ALTA" : "MEDIA",
          "Facturación",
          collected ? "COBRADA_SIN_NCF" : "NCF_PENDIENTE",
          "Factura #" + invoice.getId() + " sin NCF (" + invoiceStatus(invoice.getStatus()) + ").",
          "Creada " + formatDateTime(invoice.getCreatedAt()),
          "/billing/invoices/" + invoice.getId()
      ));
    }

    for (Invoice invoice : overdueBalances) {
      long ageDays = invoice.getCreatedAt() == null
          ? overdueDays
          : Math.max(0L, ChronoUnit.DAYS.between(invoice.getCreatedAt().toLocalDate(), today));
      findings.add(new ComplianceFinding(
          ageDays >= (overdueDays * 2L) ? "ALTA" : "MEDIA",
          "Cobros",
          "BALANCE_VENCIDO",
          "Factura #" + invoice.getId() + " con balance pendiente " + ageDays + " días.",
          "Balance " + invoice.getBalance(),
          "/billing/invoices/" + invoice.getId()
      ));
    }

    int longOpenCashCount = longOpenSessions.size();
    for (CashSession session : limit(longOpenSessions, safeFindingLimit)) {
      long hours = session.getOpenedAt() == null
          ? openHours
          : Math.max(0L, ChronoUnit.HOURS.between(session.getOpenedAt(), now));
      findings.add(new ComplianceFinding(
          hours >= (openHours * 2L) ? "ALTA" : "MEDIA",
          "Caja",
          "CAJA_ABIERTA_EXTENDIDA",
          "Caja #" + session.getId() + " abierta por " + hours + " horas.",
          "Abierta " + formatDateTime(session.getOpenedAt()) + " por " + withDefault(session.getOpenedBy()),
          "/billing/cash"
      ));
    }

    int fiscalExpiringCount = 0;
    int fiscalNearExhaustionCount = 0;
    for (FiscalSequence seq : activeSequences) {
      String typeCode = withDefault(seq.getTypeCode());
      if (seq.getExpiresAt() != null) {
        long days = ChronoUnit.DAYS.between(today, seq.getExpiresAt());
        if (days <= expiryDays) {
          fiscalExpiringCount++;
          findings.add(new ComplianceFinding(
              days < 0 ? "ALTA" : "MEDIA",
              "NCF",
              days < 0 ? "SECUENCIA_VENCIDA" : "SECUENCIA_POR_VENCER",
              "Secuencia " + typeCode + (days < 0 ? " vencida." : " vence en " + days + " días."),
              "Fecha límite " + seq.getExpiresAt(),
              "/billing/fiscal"
          ));
        }
      }

      if (seq.getEndNumber() != null) {
        long remaining = seq.getEndNumber() - seq.getNextNumber() + 1;
        if (remaining <= remainingWarning) {
          fiscalNearExhaustionCount++;
          findings.add(new ComplianceFinding(
              remaining < 0 ? "ALTA" : "MEDIA",
              "NCF",
              remaining < 0 ? "SECUENCIA_AGOTADA" : "SECUENCIA_CASI_AGOTADA",
              "Secuencia " + typeCode + (remaining < 0 ? " agotada." : " con " + remaining + " NCF restantes."),
              "Rango " + seq.getStartNumber() + "-" + seq.getEndNumber(),
              "/billing/fiscal"
          ));
        }
      }
    }

    findings.sort(
        Comparator.comparingInt((ComplianceFinding f) -> severityRank(f.level()))
            .thenComparing(ComplianceFinding::category)
            .thenComparing(ComplianceFinding::code)
    );

    return new ComplianceSnapshot(
        from,
        to,
        usernameFilter,
        criticalActionsCount,
        pendingNcfCount,
        overdueBalanceCount,
        collectedWithoutNcfCount,
        longOpenCashCount,
        fiscalExpiringCount,
        fiscalNearExhaustionCount,
        findings,
        criticalEventsPage,
        CRITICAL_ACTIONS,
        overdueDays,
        openHours,
        expiryDays,
        remainingWarning
    );
  }

  public List<AuditLog> loadCriticalEventsForExport(LocalDate fromDate,
                                                    LocalDate toDate,
                                                    String username) {
    LocalDate today = LocalDate.now();
    LocalDate from = (fromDate != null) ? fromDate : today.minusDays(30);
    LocalDate to = (toDate != null) ? toDate : today;
    if (to.isBefore(from)) {
      LocalDate swap = from;
      from = to;
      to = swap;
    }
    return auditLogRepository.searchByActionsForExport(
        CRITICAL_ACTIONS,
        normalize(username),
        from.atStartOfDay(),
        to.plusDays(1).atStartOfDay()
    );
  }

  private static <T> List<T> limit(List<T> list, int max) {
    if (list == null || list.isEmpty()) return List.of();
    if (list.size() <= max) return list;
    return list.subList(0, max);
  }

  private static int clamp(int value, int fallback, int min, int max) {
    int v = value <= 0 ? fallback : value;
    return Math.max(min, Math.min(max, v));
  }

  private static int severityRank(String level) {
    if ("ALTA".equalsIgnoreCase(level)) return 0;
    if ("MEDIA".equalsIgnoreCase(level)) return 1;
    return 2;
  }

  private static String withDefault(String value) {
    if (value == null || value.isBlank()) return "N/D";
    return value;
  }

  private static String normalize(String value) {
    if (value == null) return "";
    String trimmed = value.trim();
    return trimmed.isEmpty() ? "" : trimmed;
  }

  private static String formatDateTime(LocalDateTime value) {
    if (value == null) return "N/D";
    return DATE_TIME.format(value);
  }

  private static String invoiceStatus(InvoiceStatus status) {
    if (status == null) return "N/D";
    return status.label();
  }

  public record ComplianceSnapshot(
      LocalDate fromDate,
      LocalDate toDate,
      String usernameFilter,
      long criticalActionsCount,
      long pendingNcfCount,
      long overdueBalanceCount,
      long collectedWithoutNcfCount,
      int longOpenCashCount,
      int fiscalExpiringCount,
      int fiscalNearExhaustionCount,
      List<ComplianceFinding> findings,
      Page<AuditLog> criticalEventsPage,
      List<String> criticalActions,
      int overdueBalanceDays,
      int cashOpenMaxHours,
      int fiscalExpiryWarningDays,
      long fiscalRemainingWarning
  ) {}

  public record ComplianceFinding(
      String level,
      String category,
      String code,
      String detail,
      String reference,
      String href
  ) {}
}
