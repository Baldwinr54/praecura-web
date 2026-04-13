package com.baldwin.praecura.controller;

import com.baldwin.praecura.entity.CashSession;
import com.baldwin.praecura.security.SecurityRoleUtils;
import com.baldwin.praecura.service.CashSessionService;
import com.baldwin.praecura.service.BillingSupervisorAccessService;
import com.baldwin.praecura.service.AuditService;
import com.baldwin.praecura.service.TabularExportService;
import com.baldwin.praecura.repository.AppointmentRepository;
import com.baldwin.praecura.repository.InvoiceRepository;
import com.baldwin.praecura.repository.PaymentRepository;
import com.baldwin.praecura.entity.InvoiceStatus;
import com.baldwin.praecura.entity.Payment;
import jakarta.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

@Controller
@RequestMapping("/billing/cash")
public class CashSessionController {

  private final CashSessionService cashSessionService;
  private final AppointmentRepository appointmentRepository;
  private final InvoiceRepository invoiceRepository;
  private final PaymentRepository paymentRepository;
  private final BillingSupervisorAccessService billingSupervisorAccessService;
  private final AuditService auditService;
  private final TabularExportService tabularExportService;

  public CashSessionController(CashSessionService cashSessionService,
                               AppointmentRepository appointmentRepository,
                               InvoiceRepository invoiceRepository,
                               PaymentRepository paymentRepository,
                               BillingSupervisorAccessService billingSupervisorAccessService,
                               AuditService auditService,
                               TabularExportService tabularExportService) {
    this.cashSessionService = cashSessionService;
    this.appointmentRepository = appointmentRepository;
    this.invoiceRepository = invoiceRepository;
    this.paymentRepository = paymentRepository;
    this.billingSupervisorAccessService = billingSupervisorAccessService;
    this.auditService = auditService;
    this.tabularExportService = tabularExportService;
  }

  @GetMapping
  public String index(@RequestParam(required = false) String q, Model model) {
    var active = cashSessionService.findActive().orElse(null);
    model.addAttribute("session", active);
    model.addAttribute("summary", cashSessionService.buildSummary(active));
    model.addAttribute("reconciliation", cashSessionService.buildReconciliation(active));
    model.addAttribute("movements", active != null
        ? paymentRepository.findByCashSessionIdOrderByCreatedAtDesc(active.getId())
        : List.of());

    LocalDate today = LocalDate.now();
    LocalDateTime from = today.atStartOfDay();
    LocalDateTime to = today.plusDays(1).atStartOfDay();

    var appts = appointmentRepository.findCashDeskRows(
        from,
        to,
        InvoiceStatus.VOID,
        PageRequest.of(0, 50, Sort.by("scheduledAt").ascending())
    );

    List<CashDeskRow> rows = new ArrayList<>();
    for (var a : appts.getContent()) {
      BigDecimal price = a.getServicePrice();
      BigDecimal balance = a.getInvoiceBalance() != null ? a.getInvoiceBalance() : price;
      rows.add(new CashDeskRow(
          a.getAppointmentId(),
          a.getScheduledAt(),
          a.getPatientId(),
          a.getPatientName(),
          a.getDoctorName(),
          a.getServiceName(),
          price,
          a.getInvoiceId(),
          balance,
          a.getInvoiceStatus()
      ));
    }
    long pendingInvoiceCount = rows.stream()
        .filter(r -> r.invoiceId() == null)
        .count();
    long pendingCollectionCount = rows.stream()
        .filter(r -> r.invoiceId() != null
            && r.status() != InvoiceStatus.VOID
            && r.balance() != null
            && r.balance().compareTo(BigDecimal.ZERO) > 0)
        .count();
    long paidCount = rows.size() - pendingInvoiceCount - pendingCollectionCount;

    model.addAttribute("cashRows", rows);
    model.addAttribute("pendingInvoiceCount", pendingInvoiceCount);
    model.addAttribute("pendingCollectionCount", pendingCollectionCount);
    model.addAttribute("paidCount", paidCount);
    String query = normalizeQuery(q);
    model.addAttribute("cashSearch", query == null ? "" : query);
    model.addAttribute("pendingInvoices", invoiceRepository.searchPendingForCash(
        query,
        InvoiceStatus.VOID,
        PageRequest.of(0, 25, Sort.by(Sort.Direction.DESC, "createdAt"))
    ).getContent());
    model.addAttribute("recentSessions", cashSessionService.listRecentSnapshots(15));
    return "billing/cash";
  }

  @PostMapping("/open")
  public String open(@RequestParam BigDecimal openingAmount,
                     @RequestParam(required = false) String notes,
                     RedirectAttributes ra) {
    try {
      CashSession session = cashSessionService.openSession(openingAmount, notes);
      auditService.log("CASH_SESSION_OPENED",
          "CashSession",
          session.getId(),
          "openingAmount=" + session.getOpeningAmount());
      ra.addFlashAttribute("success", "Caja abierta correctamente.");
    } catch (Exception ex) {
      ra.addFlashAttribute("error", ex.getMessage());
    }
    return "redirect:/billing/cash";
  }

  @PostMapping("/{id}/close")
  public String close(@PathVariable Long id,
                      @RequestParam BigDecimal closingAmount,
                      @RequestParam(required = false) String notes,
                      Authentication authentication,
                      RedirectAttributes ra) {
    if (!isSupervisor(authentication)) {
      ra.addFlashAttribute("error", "No tienes permiso para cierre de caja. Requiere supervisor financiero.");
      return "redirect:/billing/cash";
    }
    try {
      CashSession session = cashSessionService.closeSession(id, closingAmount, notes);
      auditService.log("CASH_SESSION_CLOSED",
          "CashSession",
          session.getId(),
          "closingAmount=" + session.getClosingAmount());
      ra.addFlashAttribute("success", "Caja cerrada correctamente.");
    } catch (Exception ex) {
      ra.addFlashAttribute("error", ex.getMessage());
    }
    return "redirect:/billing/cash";
  }

  @GetMapping(value = "/export/sessions.csv", produces = "text/csv")
  public void exportSessionsCsv(HttpServletResponse response) throws Exception {
    List<CashSessionService.CashSessionSnapshot> sessions = cashSessionService.listRecentSnapshots(200);
    String filename = "praecura_cajas_historico.csv";
    prepareCsvResponse(response, filename);
    try (PrintWriter out = csvWriter(response)) {
      writeBom(out);
      out.println(String.join(",", "session_id", "status", "opened_at", "closed_at", "opened_by", "closed_by",
          "opening_amount", "cash_in", "cash_out", "expected", "closing_amount", "difference", "notes"));
      for (var s : sessions) {
        writeCsvLine(out,
            s.id(),
            s.status() != null ? s.status().name() : null,
            s.openedAt(),
            s.closedAt(),
            s.openedBy(),
            s.closedBy(),
            s.openingAmount(),
            s.cashIn(),
            s.cashOut(),
            s.expected(),
            s.closingAmount(),
            s.difference(),
            s.notes());
      }
      out.flush();
    }
  }

  @GetMapping(value = "/export/sessions.xlsx", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
  public void exportSessionsXlsx(HttpServletResponse response) throws Exception {
    List<CashSessionService.CashSessionSnapshot> sessions = cashSessionService.listRecentSnapshots(200);
    List<List<Object>> rows = new ArrayList<>();
    for (var s : sessions) {
      rows.add(Arrays.asList(
          s.id(),
          s.status() != null ? s.status().name() : null,
          s.openedAt(),
          s.closedAt(),
          s.openedBy(),
          s.closedBy(),
          s.openingAmount(),
          s.cashIn(),
          s.cashOut(),
          s.expected(),
          s.closingAmount(),
          s.difference(),
          s.notes()
      ));
    }
    tabularExportService.writeXlsx(
        response,
        "praecura_cajas_historico.xlsx",
        "Cajas",
        List.of(
            "session_id", "status", "opened_at", "closed_at", "opened_by", "closed_by",
            "opening_amount", "cash_in", "cash_out", "expected", "closing_amount", "difference", "notes"
        ),
        rows,
        true
    );
  }

  @GetMapping(value = "/sessions/{id}/export.csv", produces = "text/csv")
  public void exportSessionMovementsCsv(@PathVariable Long id, HttpServletResponse response) throws Exception {
    CashSession session;
    try {
      session = cashSessionService.getSession(id);
    } catch (IllegalArgumentException ex) {
      response.sendError(404, ex.getMessage());
      return;
    }

    List<CashMovementRow> movements = buildSessionMovements(id);
    String filename = "praecura_caja_" + id + "_movimientos.csv";
    prepareCsvResponse(response, filename);
    try (PrintWriter out = csvWriter(response)) {
      writeBom(out);
      writeCsvLine(out, "session_id", id);
      writeCsvLine(out, "status", session.getStatus() != null ? session.getStatus().name() : "");
      writeCsvLine(out, "opened_at", session.getOpenedAt());
      writeCsvLine(out, "closed_at", session.getClosedAt());
      out.println();
      out.println(String.join(",", "date_time", "type", "payment_id", "invoice_id", "patient", "method", "amount", "reference", "username"));
      for (CashMovementRow m : movements) {
        writeCsvLine(out,
            m.occurredAt(),
            m.type(),
            m.paymentId(),
            m.invoiceId(),
            m.patientName(),
            m.method(),
            m.amount(),
            m.reference(),
            m.username());
      }
      out.flush();
    }
  }

  @GetMapping(value = "/sessions/{id}/reconciliation.csv", produces = "text/csv")
  public void exportSessionReconciliationCsv(@PathVariable Long id, HttpServletResponse response) throws Exception {
    CashSession session;
    try {
      session = cashSessionService.getSession(id);
    } catch (IllegalArgumentException ex) {
      response.sendError(404, ex.getMessage());
      return;
    }

    CashSessionService.CashSessionReconciliation reconciliation = cashSessionService.buildReconciliation(session);
    String filename = "praecura_caja_" + id + "_conciliacion.csv";
    prepareCsvResponse(response, filename);
    try (PrintWriter out = csvWriter(response)) {
      writeBom(out);
      writeCsvLine(out, "session_id", id);
      writeCsvLine(out, "opened_at", session.getOpenedAt());
      writeCsvLine(out, "closed_at", session.getClosedAt());
      writeCsvLine(out, "window_from", reconciliation.from());
      writeCsvLine(out, "window_to", reconciliation.to());
      writeCsvLine(out, "total_captured", reconciliation.totalCaptured());
      writeCsvLine(out, "total_refunded", reconciliation.totalRefunded());
      writeCsvLine(out, "total_net", reconciliation.totalNet());
      out.println();

      out.println("METODOS");
      out.println(String.join(",", "method", "captured_count", "captured_amount", "refund_count", "refunded_amount", "net_amount"));
      for (var m : reconciliation.methods()) {
        writeCsvLine(out,
            m.method() != null ? m.method().name() : null,
            m.capturedCount(),
            m.capturedAmount(),
            m.refundCount(),
            m.refundedAmount(),
            m.netAmount());
      }
      out.println();

      out.println("CANALES");
      out.println(String.join(",", "channel", "count", "amount"));
      for (var c : reconciliation.channels()) {
        writeCsvLine(out,
            c.channel() != null ? c.channel().name() : null,
            c.count(),
            c.amount());
      }
      out.flush();
    }
  }

  private List<CashMovementRow> buildSessionMovements(Long sessionId) {
    List<CashMovementRow> rows = new ArrayList<>();

    for (Payment payment : paymentRepository.findByCashSessionIdOrderByCreatedAtDesc(sessionId)) {
      rows.add(new CashMovementRow(
          payment.getCreatedAt(),
          "IN",
          payment.getId(),
          payment.getInvoice() != null ? payment.getInvoice().getId() : null,
          payment.getPatient() != null ? payment.getPatient().getFullName() : null,
          payment.getMethod() != null ? payment.getMethod().name() : null,
          payment.getAmount(),
          payment.getExternalId(),
          payment.getUsername()
      ));
    }

    for (Payment payment : paymentRepository.findByRefundSessionIdOrderByCreatedAtDesc(sessionId)) {
      rows.add(new CashMovementRow(
          payment.getRefundedAt() != null ? payment.getRefundedAt() : payment.getCreatedAt(),
          "OUT",
          payment.getId(),
          payment.getInvoice() != null ? payment.getInvoice().getId() : null,
          payment.getPatient() != null ? payment.getPatient().getFullName() : null,
          payment.getRefundMethod() != null ? payment.getRefundMethod().name() : null,
          payment.getRefundedAmount() != null ? payment.getRefundedAmount().negate() : BigDecimal.ZERO,
          payment.getRefundReference(),
          payment.getUsername()
      ));
    }

    rows.sort(Comparator.comparing(CashMovementRow::occurredAt, Comparator.nullsLast(Comparator.naturalOrder())));
    return rows;
  }

  private boolean isSupervisor(Authentication authentication) {
    boolean isAdmin = SecurityRoleUtils.hasAdminAuthority(authentication);
    if (isAdmin) return true;
    if (authentication == null || !authentication.isAuthenticated()) return false;
    return billingSupervisorAccessService.isSupervisor(authentication.getName());
  }

  private String normalizeQuery(String q) {
    if (q == null) return null;
    String trimmed = q.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }

  private void prepareCsvResponse(HttpServletResponse response, String filename) {
    tabularExportService.prepareCsvResponse(response, filename);
  }

  private PrintWriter csvWriter(HttpServletResponse response) throws Exception {
    return tabularExportService.csvWriter(response);
  }

  private void writeBom(PrintWriter out) {
    tabularExportService.writeBom(out);
  }

  private void writeCsvLine(PrintWriter out, Object... values) {
    tabularExportService.writeCsvLine(out, true, values);
  }

  public record CashDeskRow(Long appointmentId,
                            LocalDateTime scheduledAt,
                            Long patientId,
                            String patientName,
                            String doctorName,
                            String serviceName,
                            BigDecimal servicePrice,
                            Long invoiceId,
                            BigDecimal balance,
                            InvoiceStatus status) {}

  private record CashMovementRow(LocalDateTime occurredAt,
                                 String type,
                                 Long paymentId,
                                 Long invoiceId,
                                 String patientName,
                                 String method,
                                 BigDecimal amount,
                                 String reference,
                                 String username) {}
}
