package com.baldwin.praecura.service;

import com.baldwin.praecura.entity.AlertSeverity;
import com.baldwin.praecura.entity.AlertStatus;
import com.baldwin.praecura.entity.DailyClosingStatus;
import com.baldwin.praecura.entity.DailyOperationalClosing;
import com.baldwin.praecura.entity.EcfStatus;
import com.baldwin.praecura.entity.InvoiceStatus;
import com.baldwin.praecura.entity.OperationalAlert;
import com.baldwin.praecura.entity.PaymentStatus;
import com.baldwin.praecura.repository.AppointmentRepository;
import com.baldwin.praecura.repository.DailyOperationalClosingRepository;
import com.baldwin.praecura.repository.ElectronicFiscalDocumentRepository;
import com.baldwin.praecura.repository.InvoiceRepository;
import com.baldwin.praecura.repository.OperationalAlertRepository;
import com.baldwin.praecura.repository.PaymentRepository;
import com.baldwin.praecura.repository.ReceivableCommitmentRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExecutiveOpsService {

  private final AppointmentRepository appointmentRepository;
  private final PaymentRepository paymentRepository;
  private final InvoiceRepository invoiceRepository;
  private final ElectronicFiscalDocumentRepository ecfRepository;
  private final OperationalAlertRepository operationalAlertRepository;
  private final DailyOperationalClosingRepository dailyOperationalClosingRepository;
  private final ReceivableCommitmentRepository receivableCommitmentRepository;
  private final PharmacyService pharmacyService;
  private final AuditService auditService;

  public ExecutiveOpsService(AppointmentRepository appointmentRepository,
                             PaymentRepository paymentRepository,
                             InvoiceRepository invoiceRepository,
                             ElectronicFiscalDocumentRepository ecfRepository,
                             OperationalAlertRepository operationalAlertRepository,
                             DailyOperationalClosingRepository dailyOperationalClosingRepository,
                             ReceivableCommitmentRepository receivableCommitmentRepository,
                             PharmacyService pharmacyService,
                             AuditService auditService) {
    this.appointmentRepository = appointmentRepository;
    this.paymentRepository = paymentRepository;
    this.invoiceRepository = invoiceRepository;
    this.ecfRepository = ecfRepository;
    this.operationalAlertRepository = operationalAlertRepository;
    this.dailyOperationalClosingRepository = dailyOperationalClosingRepository;
    this.receivableCommitmentRepository = receivableCommitmentRepository;
    this.pharmacyService = pharmacyService;
    this.auditService = auditService;
  }

  @Transactional(readOnly = true)
  public ExecutiveSummary summary(LocalDate from, LocalDate to) {
    LocalDate startDate = from != null ? from : LocalDate.now().minusDays(30);
    LocalDate endDate = to != null ? to : LocalDate.now();

    LocalDateTime start = startDate.atStartOfDay();
    LocalDateTime end = endDate.plusDays(1).atStartOfDay();

    long appointments = appointmentRepository.countByScheduledAtBetweenAndActiveTrue(start, end);
    long completed = appointmentRepository.countByScheduledAtBetweenAndStatusAndActiveTrue(
        start,
        end,
        com.baldwin.praecura.entity.AppointmentStatus.COMPLETADA
    );
    long noShow = appointmentRepository.countByScheduledAtBetweenAndStatusAndActiveTrue(
        start,
        end,
        com.baldwin.praecura.entity.AppointmentStatus.NO_ASISTIO
    );

    BigDecimal collected = paymentRepository.sumByStatusBetween(PaymentStatus.CAPTURED, start, end);
    BigDecimal openBalance = invoiceRepository.findAllOpenBalances(InvoiceStatus.VOID).stream()
        .map(i -> i.getBalance() != null ? i.getBalance() : BigDecimal.ZERO)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal refunds = paymentRepository.findByRefundedAtGreaterThanEqualAndRefundedAtLessThanOrderByRefundedAtAsc(start, end).stream()
        .map(p -> p.getRefundedAmount() != null ? p.getRefundedAmount() : BigDecimal.ZERO)
        .reduce(BigDecimal.ZERO, BigDecimal::add);

    long pendingEcf = ecfRepository.findTop200ByStatusInOrderByUpdatedAtAsc(
        List.of(EcfStatus.PENDING, EcfStatus.SIGNED, EcfStatus.SENT, EcfStatus.ERROR)
    ).size();

    long overdueCommitments = receivableCommitmentRepository.countByStatusAndPromisedDateBefore(
        com.baldwin.praecura.entity.ReceivableCommitmentStatus.PENDING,
        LocalDateTime.now()
    );

    long openAlerts = operationalAlertRepository.countByStatus(AlertStatus.OPEN);

    return new ExecutiveSummary(
        appointments,
        completed,
        noShow,
        collected != null ? collected : BigDecimal.ZERO,
        openBalance,
        refunds,
        pendingEcf,
        overdueCommitments,
        openAlerts
    );
  }

  @Transactional
  public List<OperationalAlert> generateAlerts(LocalDate day) {
    LocalDate date = day != null ? day : LocalDate.now();
    LocalDateTime start = date.atStartOfDay();
    LocalDateTime end = date.plusDays(1).atStartOfDay();

    List<OperationalAlert> created = new ArrayList<>();

    long pendingEcf = ecfRepository.findTop200ByStatusInOrderByUpdatedAtAsc(
        List.of(EcfStatus.PENDING, EcfStatus.SIGNED, EcfStatus.SENT, EcfStatus.ERROR)
    ).size();
    if (pendingEcf > 20) {
      created.add(openAlert("ECF_BACKLOG", AlertSeverity.CRITICAL,
          "Cola e-CF elevada",
          "Existen " + pendingEcf + " comprobantes e-CF pendientes. Revisa DGII y cola automática.",
          "{\"pendingEcf\":" + pendingEcf + "}"));
    }

    long overdueAr = invoiceRepository.countOverdueBalance(InvoiceStatus.VOID, LocalDateTime.now().minusDays(30));
    if (overdueAr > 25) {
      created.add(openAlert("AR_OVERDUE", AlertSeverity.WARNING,
          "Cartera vencida en aumento",
          "Hay " + overdueAr + " facturas con balance vencido > 30 días.",
          "{\"overdueInvoices\":" + overdueAr + "}"));
    }

    long noShow = appointmentRepository.countByScheduledAtBetweenAndStatusAndActiveTrue(
        start, end, com.baldwin.praecura.entity.AppointmentStatus.NO_ASISTIO);
    long total = appointmentRepository.countByScheduledAtBetweenAndActiveTrue(start, end);
    double noShowRate = total > 0 ? ((double) noShow / (double) total) * 100.0 : 0.0;
    if (noShowRate >= 20.0) {
      created.add(openAlert("NO_SHOW_SPIKE", AlertSeverity.WARNING,
          "No-show alto",
          String.format("No-show del %.2f%% en %s. Reforzar confirmaciones.", noShowRate, date),
          "{\"noShowRate\":" + String.format("%.2f", noShowRate) + "}"));
    }

    var lowStockItems = pharmacyService.lowStockItems();
    if (!lowStockItems.isEmpty()) {
      created.add(openAlert("LOW_STOCK", AlertSeverity.CRITICAL,
          "Inventario crítico",
          "Hay " + lowStockItems.size() + " ítems en o por debajo del mínimo.",
          "{\"lowStockItems\":" + lowStockItems.size() + "}"));
    }

    return created;
  }

  @Transactional
  public DailyOperationalClosing generateDailyClosing(LocalDate day, String username) {
    LocalDate date = day != null ? day : LocalDate.now();
    DailyOperationalClosing closing = dailyOperationalClosingRepository.findByClosingDate(date)
        .orElseGet(DailyOperationalClosing::new);

    LocalDateTime start = date.atStartOfDay();
    LocalDateTime end = date.plusDays(1).atStartOfDay();

    long appointments = appointmentRepository.countByScheduledAtBetweenAndActiveTrue(start, end);
    long completed = appointmentRepository.countByScheduledAtBetweenAndStatusAndActiveTrue(
        start,
        end,
        com.baldwin.praecura.entity.AppointmentStatus.COMPLETADA
    );

    BigDecimal collected = paymentRepository.sumByStatusBetween(PaymentStatus.CAPTURED, start, end);
    BigDecimal refunds = paymentRepository.findByRefundedAtGreaterThanEqualAndRefundedAtLessThanOrderByRefundedAtAsc(start, end).stream()
        .map(p -> p.getRefundedAmount() != null ? p.getRefundedAmount() : BigDecimal.ZERO)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal pending = invoiceRepository.findAllOpenBalances(InvoiceStatus.VOID).stream()
        .map(i -> i.getBalance() != null ? i.getBalance() : BigDecimal.ZERO)
        .reduce(BigDecimal.ZERO, BigDecimal::add);

    closing.setClosingDate(date);
    if (closing.getStatus() == null) {
      closing.setStatus(DailyClosingStatus.DRAFT);
    }
    closing.setTotalAppointments(appointments);
    closing.setCompletedAppointments(completed);
    closing.setTotalCollected(collected != null ? collected : BigDecimal.ZERO);
    closing.setTotalRefunds(refunds);
    closing.setTotalPending(pending);
    closing.setOpenAlerts(operationalAlertRepository.countByStatus(AlertStatus.OPEN));
    closing.setGeneratedAt(LocalDateTime.now());
    closing.setGeneratedBy(username);
    dailyOperationalClosingRepository.save(closing);

    auditService.log("DAILY_CLOSING_GENERATED", "DailyOperationalClosing", closing.getId(), "date=" + date);
    return closing;
  }

  @Transactional
  public void finalizeClosing(Long closingId, String notes, String username) {
    DailyOperationalClosing closing = dailyOperationalClosingRepository.findById(closingId)
        .orElseThrow(() -> new IllegalArgumentException("Cierre diario no existe."));
    closing.setStatus(DailyClosingStatus.FINALIZED);
    if (notes != null && !notes.isBlank()) {
      closing.setNotes(notes.trim());
    }
    closing.setGeneratedBy(username);
    closing.setGeneratedAt(LocalDateTime.now());
    dailyOperationalClosingRepository.save(closing);

    auditService.log("DAILY_CLOSING_FINALIZED", "DailyOperationalClosing", closingId, "date=" + closing.getClosingDate());
  }

  @Transactional(readOnly = true)
  public List<OperationalAlert> listAlerts() {
    return operationalAlertRepository.findTop100ByOrderByDetectedAtDesc();
  }

  @Transactional(readOnly = true)
  public List<DailyOperationalClosing> listClosings() {
    return dailyOperationalClosingRepository.findTop30ByOrderByClosingDateDesc();
  }

  @Transactional
  public void resolveAlert(Long alertId) {
    OperationalAlert alert = operationalAlertRepository.findById(alertId)
        .orElseThrow(() -> new IllegalArgumentException("Alerta no existe."));
    alert.setStatus(AlertStatus.RESOLVED);
    alert.setResolvedAt(LocalDateTime.now());
    operationalAlertRepository.save(alert);

    auditService.log("OP_ALERT_RESOLVED", "OperationalAlert", alertId, alert.getAlertType());
  }

  private OperationalAlert openAlert(String type,
                                     AlertSeverity severity,
                                     String title,
                                     String message,
                                     String metadataJson) {
    OperationalAlert alert = new OperationalAlert();
    alert.setAlertType(type);
    alert.setSeverity(severity);
    alert.setTitle(title);
    alert.setMessage(message);
    alert.setStatus(AlertStatus.OPEN);
    alert.setMetadataJson(metadataJson);
    alert.setDetectedAt(LocalDateTime.now());
    operationalAlertRepository.save(alert);

    auditService.log("OP_ALERT_CREATED", "OperationalAlert", alert.getId(), type + " | " + title);
    return alert;
  }

  public record ExecutiveSummary(
      long appointments,
      long completed,
      long noShow,
      BigDecimal collected,
      BigDecimal pending,
      BigDecimal refunds,
      long pendingEcf,
      long overdueCommitments,
      long openAlerts
  ) {}
}
