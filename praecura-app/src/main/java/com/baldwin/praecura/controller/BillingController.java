package com.baldwin.praecura.controller;

import com.baldwin.praecura.entity.Invoice;
import com.baldwin.praecura.entity.InvoiceStatus;
import com.baldwin.praecura.entity.BillingCharge;
import com.baldwin.praecura.entity.BillingChargeCategory;
import com.baldwin.praecura.entity.BillingChargeStatus;
import com.baldwin.praecura.entity.EcfStatus;
import com.baldwin.praecura.entity.PaymentChannel;
import com.baldwin.praecura.entity.PaymentMethod;
import com.baldwin.praecura.entity.PaymentStatus;
import com.baldwin.praecura.entity.PaymentLink;
import com.baldwin.praecura.entity.PaymentLinkProvider;
import com.baldwin.praecura.entity.PaymentLinkStatus;
import com.baldwin.praecura.entity.ReceivableCommitment;
import com.baldwin.praecura.repository.InvoiceRepository;
import com.baldwin.praecura.repository.PaymentRepository;
import com.baldwin.praecura.security.SecurityRoleUtils;
import com.baldwin.praecura.service.BillingService;
import com.baldwin.praecura.service.CardNetClient;
import com.baldwin.praecura.service.CashSessionService;
import com.baldwin.praecura.service.CriticalActionApprovalService;
import com.baldwin.praecura.service.ElectronicFiscalDocumentService;
import com.baldwin.praecura.service.BillingPresentationService;
import com.baldwin.praecura.service.PaymentLinkService;
import com.baldwin.praecura.service.ReceivableCommitmentService;
import com.baldwin.praecura.service.SystemBrandingService;
import com.baldwin.praecura.service.TabularExportService;
import jakarta.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/billing")
public class BillingController {

  private static final int EXPORT_BATCH_SIZE = 500;

  private final BillingService billingService;
  private final InvoiceRepository invoiceRepository;
  private final PaymentRepository paymentRepository;
  private final PaymentLinkService paymentLinkService;
  private final CardNetClient cardNetClient;
  private final com.baldwin.praecura.service.FiscalSequenceService fiscalSequenceService;
  private final CashSessionService cashSessionService;
  private final com.baldwin.praecura.service.BillingSupervisorAccessService billingSupervisorAccessService;
  private final SystemBrandingService systemBrandingService;
  private final ElectronicFiscalDocumentService electronicFiscalDocumentService;
  private final ReceivableCommitmentService receivableCommitmentService;
  private final CriticalActionApprovalService criticalActionApprovalService;
  private final BillingPresentationService billingPresentationService;
  private final TabularExportService tabularExportService;

  public BillingController(BillingService billingService,
                           InvoiceRepository invoiceRepository,
                           PaymentRepository paymentRepository,
                           PaymentLinkService paymentLinkService,
                           CardNetClient cardNetClient,
                           com.baldwin.praecura.service.FiscalSequenceService fiscalSequenceService,
                           CashSessionService cashSessionService,
                           com.baldwin.praecura.service.BillingSupervisorAccessService billingSupervisorAccessService,
                           SystemBrandingService systemBrandingService,
                           ElectronicFiscalDocumentService electronicFiscalDocumentService,
                           ReceivableCommitmentService receivableCommitmentService,
                           CriticalActionApprovalService criticalActionApprovalService,
                           BillingPresentationService billingPresentationService,
                           TabularExportService tabularExportService) {
    this.billingService = billingService;
    this.invoiceRepository = invoiceRepository;
    this.paymentRepository = paymentRepository;
    this.paymentLinkService = paymentLinkService;
    this.cardNetClient = cardNetClient;
    this.fiscalSequenceService = fiscalSequenceService;
    this.cashSessionService = cashSessionService;
    this.billingSupervisorAccessService = billingSupervisorAccessService;
    this.systemBrandingService = systemBrandingService;
    this.electronicFiscalDocumentService = electronicFiscalDocumentService;
    this.receivableCommitmentService = receivableCommitmentService;
    this.criticalActionApprovalService = criticalActionApprovalService;
    this.billingPresentationService = billingPresentationService;
    this.tabularExportService = tabularExportService;
  }

  @GetMapping
  public String index(@RequestParam(required = false) InvoiceStatus status,
                      @RequestParam(required = false) Long patientId,
                      @RequestParam(required = false) Long appointmentId,
                      @RequestParam(required = false) LocalDate from,
                      @RequestParam(required = false) LocalDate to,
                      @RequestParam(defaultValue = "0") int page,
                      @RequestParam(defaultValue = "20") int size,
                      Model model) {
    LocalDate today = LocalDate.now();
    LocalDate fromDate = (from != null) ? from : today.minusDays(30);
    LocalDate toDate = (to != null) ? to : today;

    LocalDateTime fromDt = fromDate.atStartOfDay();
    LocalDateTime toDt = toDate.plusDays(1).atStartOfDay();
    List<Long> exportedInvoiceIds = new ArrayList<>();

    Page<Invoice> invoices = invoiceRepository.search(
        status,
        patientId,
        appointmentId,
        fromDt,
        toDt,
        PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
    );

    BigDecimal totalInvoiced = BigDecimal.ZERO;
    BigDecimal totalBalance = BigDecimal.ZERO;
    for (Invoice i : invoices.getContent()) {
      if (i.getTotal() != null) totalInvoiced = totalInvoiced.add(i.getTotal());
      if (i.getBalance() != null) totalBalance = totalBalance.add(i.getBalance());
    }
    BigDecimal totalCaptured = totalInvoiced.subtract(totalBalance).max(BigDecimal.ZERO);

    model.addAttribute("status", status);
    model.addAttribute("patientId", patientId);
    model.addAttribute("appointmentId", appointmentId);
    model.addAttribute("from", fromDate);
    model.addAttribute("to", toDate);
    model.addAttribute("size", size);
    model.addAttribute("statuses", InvoiceStatus.values());
    model.addAttribute("invoicesPage", invoices);
    model.addAttribute("totalInvoiced", totalInvoiced);
    model.addAttribute("totalBalance", totalBalance);
    model.addAttribute("totalCaptured", totalCaptured);
    model.addAttribute("pendingCollectionCount",
        invoiceRepository.countByStatusNotAndBalanceGreaterThan(InvoiceStatus.VOID, BigDecimal.ZERO));
    model.addAttribute("openChargeCount", billingService.countOpenCharges());
    model.addAttribute("pendingNcfCount",
        invoiceRepository.countPendingNcf(InvoiceStatus.VOID));
    model.addAttribute("overdueCollectionCount",
        invoiceRepository.countOverdueBalance(InvoiceStatus.VOID, LocalDateTime.now().minusDays(30)));
    addCashControlAttributes(model);
    return "billing/index";
  }

  @GetMapping("/accounts-receivable")
  public String accountsReceivable(@RequestParam(required = false) String q,
                                   @RequestParam(defaultValue = "0") int page,
                                   @RequestParam(defaultValue = "30") int size,
                                   Model model) {
    String query = billingPresentationService.normalizeQuery(q);
    int pageSize = Math.max(10, Math.min(size, 100));
    Pageable pageable = PageRequest.of(Math.max(0, page), pageSize, Sort.by(Sort.Direction.ASC, "createdAt"));

    Page<Invoice> receivables = invoiceRepository.searchAccountsReceivable(query, InvoiceStatus.VOID, pageable);

    LocalDateTime now = LocalDateTime.now();
    List<InvoiceRepository.AccountsReceivableBucketAggregate> aggregatedBuckets =
        invoiceRepository.summarizeAccountsReceivable(query, InvoiceStatus.VOID.name(), now);

    long count0To30 = 0L;
    long count31To60 = 0L;
    long count61To90 = 0L;
    long count91Plus = 0L;
    BigDecimal amount0To30 = BigDecimal.ZERO;
    BigDecimal amount31To60 = BigDecimal.ZERO;
    BigDecimal amount61To90 = BigDecimal.ZERO;
    BigDecimal amount91Plus = BigDecimal.ZERO;

    for (InvoiceRepository.AccountsReceivableBucketAggregate row : aggregatedBuckets) {
      if (row == null || row.getBucketOrder() == null) continue;
      int bucket = row.getBucketOrder().intValue();
      long count = row.getBucketCount() != null ? row.getBucketCount() : 0L;
      BigDecimal amount = row.getBucketAmount() != null ? row.getBucketAmount() : BigDecimal.ZERO;
      switch (bucket) {
        case 0 -> {
          count0To30 = count;
          amount0To30 = amount;
        }
        case 1 -> {
          count31To60 = count;
          amount31To60 = amount;
        }
        case 2 -> {
          count61To90 = count;
          amount61To90 = amount;
        }
        default -> {
          count91Plus = count;
          amount91Plus = amount;
        }
      }
    }

    List<BillingPresentationService.ReceivableBucket> buckets = List.of(
        new BillingPresentationService.ReceivableBucket("0_30", "0-30 días", count0To30, amount0To30),
        new BillingPresentationService.ReceivableBucket("31_60", "31-60 días", count31To60, amount31To60),
        new BillingPresentationService.ReceivableBucket("61_90", "61-90 días", count61To90, amount61To90),
        new BillingPresentationService.ReceivableBucket("91_plus", "91+ días", count91Plus, amount91Plus)
    );
    long totalInvoices = count0To30 + count31To60 + count61To90 + count91Plus;
    BigDecimal totalBalance = amount0To30.add(amount31To60).add(amount61To90).add(amount91Plus);
    BillingPresentationService.AccountsReceivableSummary summary =
        new BillingPresentationService.AccountsReceivableSummary(totalInvoices, totalBalance, buckets);

    Map<Long, Integer> ageDaysByInvoice = new HashMap<>();
    Map<Long, String> agingLabelByInvoice = new HashMap<>();
    for (Invoice invoice : receivables.getContent()) {
      int ageDays = billingPresentationService.ageDays(invoice, now);
      ageDaysByInvoice.put(invoice.getId(), ageDays);
      agingLabelByInvoice.put(invoice.getId(), billingPresentationService.agingLabel(ageDays));
    }

    List<ReceivableCommitment> upcomingCommitments = receivableCommitmentService.listPendingInWindow(
        now.minusDays(7),
        now.plusDays(14),
        20
    );
    Map<Long, String> commitmentTimingById = new HashMap<>();
    for (ReceivableCommitment commitment : upcomingCommitments) {
      commitmentTimingById.put(
          commitment.getId(),
          billingPresentationService.commitmentTimingLabel(commitment.getPromisedDate(), now)
      );
    }

    model.addAttribute("q", query == null ? "" : query);
    model.addAttribute("size", pageSize);
    model.addAttribute("receivablesPage", receivables);
    model.addAttribute("summary", summary);
    model.addAttribute("ageDaysByInvoice", ageDaysByInvoice);
    model.addAttribute("agingLabelByInvoice", agingLabelByInvoice);
    model.addAttribute("upcomingCommitments", upcomingCommitments);
    model.addAttribute("commitmentTimingById", commitmentTimingById);
    model.addAttribute("overdueCommitmentsCount", receivableCommitmentService.countOverduePending());
    return "billing/accounts-receivable";
  }

  @GetMapping(value = "/accounts-receivable/export.csv", produces = "text/csv")
  public void exportAccountsReceivableCsv(@RequestParam(required = false) String q,
                                          HttpServletResponse response) throws Exception {
    String query = billingPresentationService.normalizeQuery(q);

    LocalDateTime now = LocalDateTime.now();
    String filename = "praecura_cuentas_por_cobrar.csv";
    prepareCsvResponse(response, filename);
    try (PrintWriter out = csvWriter(response)) {
      writeBom(out);
      out.println(String.join(",", "invoice_id", "created_at", "age_days", "aging_bucket", "patient_id",
          "patient_name", "fiscal_tax_id", "ncf", "status", "total", "balance"));

      streamAccountsReceivableBatches(query, batch -> {
        for (Invoice i : batch) {
          int ageDays = billingPresentationService.ageDays(i, now);
          writeCsvLine(out,
              i.getId(),
              i.getCreatedAt(),
              ageDays,
              billingPresentationService.agingLabel(ageDays),
              i.getPatient() != null ? i.getPatient().getId() : null,
              i.getPatient() != null ? i.getPatient().getFullName() : null,
              i.getFiscalTaxId(),
              i.getNcf(),
              i.getStatus() != null ? i.getStatus().name() : null,
              i.getTotal(),
              i.getBalance()
          );
        }
      });
      out.flush();
    }
  }

  @GetMapping(value = "/export.csv", produces = "text/csv")
  public void exportCsv(@RequestParam(required = false) InvoiceStatus status,
                        @RequestParam(required = false) Long patientId,
                        @RequestParam(required = false) Long appointmentId,
                        @RequestParam(required = false) LocalDate from,
                        @RequestParam(required = false) LocalDate to,
                        HttpServletResponse response) throws Exception {
    LocalDate today = LocalDate.now();
    LocalDate fromDate = (from != null) ? from : today.minusDays(30);
    LocalDate toDate = (to != null) ? to : today;

    LocalDateTime fromDt = fromDate.atStartOfDay();
    LocalDateTime toDt = toDate.plusDays(1).atStartOfDay();

    String filename = "praecura_facturas_" + fromDate + "_" + toDate + ".csv";
    prepareCsvResponse(response, filename);
    try (PrintWriter out = csvWriter(response)) {
      writeBom(out);
      out.println(String.join(",", "invoice_id", "created_at", "patient_id", "patient_name",
          "appointment_id", "status", "invoice_type", "ncf_type", "ncf", "ncf_status",
          "fiscal_name", "fiscal_tax_id", "currency", "subtotal", "tax", "discount", "total", "balance", "notes"));
      streamInvoicesByCriteriaBatches(status, patientId, appointmentId, fromDt, toDt, batch -> {
        for (Invoice i : batch) {
          writeCsvLine(out,
              i.getId(),
              i.getCreatedAt(),
              i.getPatient() != null ? i.getPatient().getId() : null,
              i.getPatient() != null ? i.getPatient().getFullName() : null,
              i.getAppointment() != null ? i.getAppointment().getId() : null,
              i.getStatus() != null ? i.getStatus().name() : null,
              i.getInvoiceType() != null ? i.getInvoiceType().name() : null,
              i.getNcfType(),
              i.getNcf(),
              i.getNcfStatus() != null ? i.getNcfStatus().name() : null,
              i.getFiscalName(),
              i.getFiscalTaxId(),
              i.getCurrency(),
              i.getSubtotal(),
              i.getTax(),
              i.getDiscount(),
              i.getTotal(),
              i.getBalance(),
              i.getNotes()
          );
        }
      });
      out.flush();
    }
  }

  @GetMapping(value = "/export.xlsx", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
  public void exportXlsx(@RequestParam(required = false) InvoiceStatus status,
                         @RequestParam(required = false) Long patientId,
                         @RequestParam(required = false) Long appointmentId,
                         @RequestParam(required = false) LocalDate from,
                         @RequestParam(required = false) LocalDate to,
                         HttpServletResponse response) throws Exception {
    LocalDate today = LocalDate.now();
    LocalDate fromDate = (from != null) ? from : today.minusDays(30);
    LocalDate toDate = (to != null) ? to : today;

    LocalDateTime fromDt = fromDate.atStartOfDay();
    LocalDateTime toDt = toDate.plusDays(1).atStartOfDay();
    List<Long> exportedInvoiceIds = new ArrayList<>();

    String filename = "praecura_facturas_pagos_" + fromDate + "_" + toDate + ".xlsx";
    List<TabularExportService.StreamingSheetData> sheets = List.of(
        new TabularExportService.StreamingSheetData(
            "Facturas",
            List.of("invoice_id", "created_at", "patient_id", "patient_name", "appointment_id",
                "status", "invoice_type", "ncf_type", "ncf", "ncf_status",
                "fiscal_name", "fiscal_tax_id", "currency", "subtotal", "tax", "discount", "total", "balance", "notes"),
            appender -> streamInvoicesByCriteriaBatches(status, patientId, appointmentId, fromDt, toDt, batch -> {
              for (Invoice i : batch) {
                exportedInvoiceIds.add(i.getId());
                appender.append(Arrays.asList(
                    i.getId(),
                    i.getCreatedAt(),
                    i.getPatient() != null ? i.getPatient().getId() : null,
                    i.getPatient() != null ? i.getPatient().getFullName() : null,
                    i.getAppointment() != null ? i.getAppointment().getId() : null,
                    i.getStatus() != null ? i.getStatus().name() : null,
                    i.getInvoiceType() != null ? i.getInvoiceType().name() : null,
                    i.getNcfType(),
                    i.getNcf(),
                    i.getNcfStatus() != null ? i.getNcfStatus().name() : null,
                    i.getFiscalName(),
                    i.getFiscalTaxId(),
                    i.getCurrency(),
                    i.getSubtotal(),
                    i.getTax(),
                    i.getDiscount(),
                    i.getTotal(),
                    i.getBalance(),
                    i.getNotes()
                ));
              }
            })
        ),
        new TabularExportService.StreamingSheetData(
            "Pagos",
            List.of("payment_id", "created_at", "invoice_id", "patient_id", "amount", "cash_received", "cash_change", "currency",
                "method", "channel", "status", "provider", "external_id", "auth_code", "last4",
                "card_brand", "terminal_id", "batch_id", "rrn", "notes"),
            appender -> forEachInvoiceIdBatch(exportedInvoiceIds, invoiceIds -> {
              List<com.baldwin.praecura.entity.Payment> payments = paymentRepository.findByInvoiceIds(invoiceIds);
              for (var p : payments) {
                appender.append(Arrays.asList(
                    p.getId(),
                    p.getCreatedAt(),
                    p.getInvoice() != null ? p.getInvoice().getId() : null,
                    p.getPatient() != null ? p.getPatient().getId() : null,
                    p.getAmount(),
                    p.getCashReceived(),
                    p.getCashChange(),
                    p.getCurrency(),
                    p.getMethod() != null ? p.getMethod().name() : null,
                    p.getChannel() != null ? p.getChannel().name() : null,
                    p.getStatus() != null ? p.getStatus().name() : null,
                    p.getProvider(),
                    maskToken(p.getExternalId(), 4),
                    maskToken(p.getAuthCode(), 2),
                    p.getLast4(),
                    p.getCardBrand(),
                    maskToken(p.getTerminalId(), 2),
                    maskToken(p.getBatchId(), 2),
                    maskToken(p.getRrn(), 2),
                    p.getNotes()
                ));
              }
            })
        )
    );
    tabularExportService.writeXlsxMultiStreaming(response, filename, sheets, true);
  }

  @GetMapping("/invoices/{id}")
  public String detail(@PathVariable Long id, Model model) {
    Invoice invoice = invoiceRepository.findWithItems(id)
        .orElseThrow(() -> new IllegalArgumentException("La factura no existe."));
    var payments = paymentRepository.findByInvoiceIdOrderByCreatedAtDesc(id);
    var links = paymentLinkService.listByInvoice(id);

    model.addAttribute("invoice", invoice);
    model.addAttribute("payments", payments);
    model.addAttribute("links", links);
    model.addAttribute("commitments", receivableCommitmentService.listByInvoice(id));
    model.addAttribute("ecf", electronicFiscalDocumentService.findByInvoiceId(id).orElse(null));
    model.addAttribute("fiscalSequences", fiscalSequenceService.listActive());
    model.addAttribute("methods", PaymentMethod.values());
    model.addAttribute("channels", PaymentChannel.values());
    model.addAttribute("statuses", PaymentStatus.values());
    model.addAttribute("ecfStatuses", EcfStatus.values());
    model.addAttribute("linkProviders", PaymentLinkProvider.values());
    model.addAttribute("linkStatuses", PaymentLinkStatus.values());
    model.addAttribute("cardnetEnabled", cardNetClient.isEnabled());
    model.addAttribute("dgiiEnabled", electronicFiscalDocumentService.isEnabled());
    model.addAttribute("ncfRequiredBeforePayment", billingService.isNcfRequiredBeforePayment());
    addCashControlAttributes(model);
    addCompanyAttributes(model);
    return "billing/detail";
  }

  @GetMapping("/charges")
  public String charges(@RequestParam(required = false) BillingChargeStatus status,
                        @RequestParam(required = false) Long patientId,
                        @RequestParam(required = false) Long appointmentId,
                        @RequestParam(required = false) LocalDate from,
                        @RequestParam(required = false) LocalDate to,
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "30") int size,
                        Model model) {
    LocalDate today = LocalDate.now();
    LocalDate fromDate = (from != null) ? from : today.minusDays(30);
    LocalDate toDate = (to != null) ? to : today;
    BillingChargeStatus statusFilter = (status != null) ? status : BillingChargeStatus.OPEN;

    Page<BillingCharge> chargesPage = billingService.searchCharges(
        statusFilter,
        patientId,
        appointmentId,
        fromDate.atStartOfDay(),
        toDate.plusDays(1).atStartOfDay(),
        PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
    );

    model.addAttribute("status", statusFilter);
    model.addAttribute("patientId", patientId);
    model.addAttribute("appointmentId", appointmentId);
    model.addAttribute("from", fromDate);
    model.addAttribute("to", toDate);
    model.addAttribute("size", size);
    model.addAttribute("statuses", BillingChargeStatus.values());
    model.addAttribute("categories", BillingChargeCategory.values());
    model.addAttribute("chargesPage", chargesPage);
    model.addAttribute("openChargeCount", billingService.countOpenCharges());
    return "billing/charges";
  }

  @PostMapping("/charges")
  public String createCharge(@RequestParam Long patientId,
                             @RequestParam(required = false) Long appointmentId,
                             @RequestParam(required = false) Long serviceId,
                             @RequestParam(required = false) BillingChargeCategory category,
                             @RequestParam(required = false) String description,
                             @RequestParam(required = false) Integer quantity,
                             @RequestParam(required = false) BigDecimal unitPrice,
                             @RequestParam(required = false) BigDecimal taxRate,
                             @RequestParam(required = false) BigDecimal discount,
                             @RequestParam(required = false) String currency,
                             @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm") LocalDateTime performedAt,
                             @RequestParam(required = false) String notes,
                             RedirectAttributes ra,
                             @RequestHeader(value = "Referer", required = false) String referer) {
    try {
      billingService.createCharge(new BillingService.ChargeRequest(
          patientId,
          appointmentId,
          serviceId,
          category,
          description,
          quantity,
          unitPrice,
          taxRate,
          discount,
          currency,
          "MANUAL",
          performedAt,
          notes
      ));
      ra.addFlashAttribute("success", "Cargo clínico registrado.");
    } catch (IllegalArgumentException ex) {
      ra.addFlashAttribute("error", ex.getMessage());
    }
    return redirectBack(referer, "/billing/charges");
  }

  @PostMapping("/charges/{id}/cancel")
  public String cancelCharge(@PathVariable Long id,
                             RedirectAttributes ra,
                             @RequestHeader(value = "Referer", required = false) String referer) {
    try {
      billingService.cancelCharge(id);
      ra.addFlashAttribute("success", "Cargo cancelado.");
    } catch (IllegalArgumentException ex) {
      ra.addFlashAttribute("error", ex.getMessage());
    }
    return redirectBack(referer, "/billing/charges");
  }

  @GetMapping("/fiscal/ecf")
  public String ecfDashboard(@RequestParam(required = false) EcfStatus status,
                             @RequestParam(defaultValue = "0") int page,
                             @RequestParam(defaultValue = "30") int size,
                             Model model) {
    Page<com.baldwin.praecura.entity.ElectronicFiscalDocument> docs = electronicFiscalDocumentService.list(
        status,
        PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
    );
    model.addAttribute("status", status);
    model.addAttribute("statuses", EcfStatus.values());
    model.addAttribute("docsPage", docs);
    model.addAttribute("size", size);
    model.addAttribute("dgiiEnabled", electronicFiscalDocumentService.isEnabled());
    return "billing/ecf";
  }

  @PostMapping("/invoices/{id}/ecf/prepare")
  public String prepareEcf(@PathVariable Long id,
                           Authentication authentication,
                           RedirectAttributes ra,
                           @RequestHeader(value = "Referer", required = false) String referer) {
    if (!isSupervisor(authentication)) {
      ra.addFlashAttribute("error", "No tienes permiso para preparar e-CF. Requiere supervisor financiero.");
      return redirectBack(referer, "/billing/invoices/" + id);
    }
    try {
      electronicFiscalDocumentService.prepareForInvoice(id);
      ra.addFlashAttribute("success", "Registro e-CF preparado para esta factura.");
    } catch (IllegalArgumentException ex) {
      ra.addFlashAttribute("error", ex.getMessage());
    }
    return redirectBack(referer, "/billing/invoices/" + id);
  }

  @PostMapping("/invoices/{id}/ecf/send")
  public String sendEcf(@PathVariable Long id,
                        Authentication authentication,
                        RedirectAttributes ra,
                        @RequestHeader(value = "Referer", required = false) String referer) {
    if (!isSupervisor(authentication)) {
      ra.addFlashAttribute("error", "No tienes permiso para enviar e-CF. Requiere supervisor financiero.");
      return redirectBack(referer, "/billing/invoices/" + id);
    }
    try {
      var doc = electronicFiscalDocumentService.sendToDgii(id);
      String status = doc.getStatus() != null ? doc.getStatus().label() : "sin estado";
      ra.addFlashAttribute("success", "e-CF enviado/actualizado. Estado: " + status + ".");
    } catch (IllegalArgumentException ex) {
      ra.addFlashAttribute("error", ex.getMessage());
    }
    return redirectBack(referer, "/billing/invoices/" + id);
  }

  @PostMapping("/invoices/{id}/ecf/sync")
  public String syncEcf(@PathVariable Long id,
                        Authentication authentication,
                        RedirectAttributes ra,
                        @RequestHeader(value = "Referer", required = false) String referer) {
    if (!isSupervisor(authentication)) {
      ra.addFlashAttribute("error", "No tienes permiso para consultar estado e-CF. Requiere supervisor financiero.");
      return redirectBack(referer, "/billing/invoices/" + id);
    }
    try {
      var doc = electronicFiscalDocumentService.syncStatus(id);
      String status = doc.getStatus() != null ? doc.getStatus().label() : "sin estado";
      ra.addFlashAttribute("success", "Estado e-CF sincronizado. Estado: " + status + ".");
    } catch (IllegalArgumentException ex) {
      ra.addFlashAttribute("error", ex.getMessage());
    }
    return redirectBack(referer, "/billing/invoices/" + id);
  }

  @PostMapping("/fiscal/ecf/process")
  public String processEcfQueue(@RequestParam(required = false) Integer limit,
                                @RequestParam(required = false) EcfStatus status,
                                @RequestParam(defaultValue = "0") int page,
                                @RequestParam(defaultValue = "30") int size,
                                Authentication authentication,
                                RedirectAttributes ra) {
    if (!isSupervisor(authentication)) {
      ra.addFlashAttribute("error", "No tienes permiso para procesar la cola e-CF. Requiere supervisor financiero.");
      return "redirect:/billing/fiscal/ecf";
    }

    try {
      int batch = (limit == null || limit <= 0) ? 30 : limit;
      var result = electronicFiscalDocumentService.processAutomaticSync(batch);
      ra.addFlashAttribute(
          "success",
          "Cola e-CF procesada. Sincronizados: " + result.syncedCount() + " · Reintentos: " + result.retriedCount() + "."
      );
    } catch (IllegalArgumentException ex) {
      ra.addFlashAttribute("error", ex.getMessage());
    }

    StringBuilder target = new StringBuilder("redirect:/billing/fiscal/ecf?page=").append(Math.max(0, page))
        .append("&size=").append(size > 0 ? size : 30);
    if (status != null) {
      target.append("&status=").append(status.name());
    }
    return target.toString();
  }

  @PostMapping("/invoices/{id}/ecf/status")
  public String updateEcfStatus(@PathVariable Long id,
                                @RequestParam EcfStatus status,
                                @RequestParam(required = false) String trackId,
                                @RequestParam(required = false) String statusCode,
                                @RequestParam(required = false) String message,
                                Authentication authentication,
                                RedirectAttributes ra,
                                @RequestHeader(value = "Referer", required = false) String referer) {
    if (!isSupervisor(authentication)) {
      ra.addFlashAttribute("error", "No tienes permiso para actualizar estado e-CF. Requiere supervisor financiero.");
      return redirectBack(referer, "/billing/invoices/" + id);
    }
    try {
      electronicFiscalDocumentService.updateStatus(id, status, trackId, statusCode, message);
      ra.addFlashAttribute("success", "Estado e-CF actualizado.");
    } catch (IllegalArgumentException ex) {
      ra.addFlashAttribute("error", ex.getMessage());
    }
    return redirectBack(referer, "/billing/invoices/" + id);
  }

  @GetMapping("/invoices/{id}/print")
  public String print(@PathVariable Long id,
                      @RequestParam(name = "layout", required = false, defaultValue = "ticket") String layout,
                      Model model) {
    Invoice invoice = invoiceRepository.findWithItems(id)
        .orElseThrow(() -> new IllegalArgumentException("La factura no existe."));
    var payments = paymentRepository.findByInvoiceIdOrderByCreatedAtDesc(id);
    var ecf = electronicFiscalDocumentService.findByInvoiceId(id).orElse(null);
    String printLayout = billingPresentationService.normalizePrintLayout(layout);
    String fiscalDocumentNumber = billingPresentationService.resolveFiscalDocumentNumber(invoice, ecf);
    String qrImageUrl = billingPresentationService.buildQrImageUrl(
        billingPresentationService.resolveFiscalVerificationUrl(ecf)
    );
    model.addAttribute("invoice", invoice);
    model.addAttribute("payments", payments);
    model.addAttribute("ecf", ecf);
    model.addAttribute("printLayout", printLayout);
    model.addAttribute("fiscalDocumentNumber", fiscalDocumentNumber);
    model.addAttribute("qrImageUrl", qrImageUrl);
    addCompanyAttributes(model);
    return "billing/print";
  }

  @PostMapping("/invoices/{id}/commitments")
  public String createCommitment(@PathVariable Long id,
                                 @RequestParam BigDecimal promisedAmount,
                                 @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm") LocalDateTime promisedDate,
                                 @RequestParam(required = false) String notes,
                                 Authentication authentication,
                                 RedirectAttributes ra,
                                 @RequestHeader(value = "Referer", required = false) String referer) {
    if (!isSupervisor(authentication)) {
      ra.addFlashAttribute("error", "No tienes permiso para registrar compromisos. Requiere supervisor financiero.");
      return redirectBack(referer, "/billing/invoices/" + id);
    }
    try {
      receivableCommitmentService.create(id, promisedAmount, promisedDate, notes);
      ra.addFlashAttribute("success", "Compromiso de pago registrado.");
    } catch (IllegalArgumentException ex) {
      ra.addFlashAttribute("error", ex.getMessage());
    }
    return redirectBack(referer, "/billing/invoices/" + id);
  }

  @PostMapping("/commitments/{id}/fulfill")
  public String fulfillCommitment(@PathVariable Long id,
                                  Authentication authentication,
                                  RedirectAttributes ra,
                                  @RequestHeader(value = "Referer", required = false) String referer) {
    if (!isSupervisor(authentication)) {
      ra.addFlashAttribute("error", "No tienes permiso para cerrar compromisos. Requiere supervisor financiero.");
      return redirectBack(referer, "/billing/accounts-receivable");
    }
    try {
      var commitment = receivableCommitmentService.markFulfilled(id);
      ra.addFlashAttribute("success", "Compromiso marcado como cumplido.");
      return redirectBack(referer, "/billing/invoices/" + commitment.getInvoice().getId());
    } catch (IllegalArgumentException ex) {
      ra.addFlashAttribute("error", ex.getMessage());
      return redirectBack(referer, "/billing/accounts-receivable");
    }
  }

  @PostMapping("/commitments/{id}/break")
  public String breakCommitment(@PathVariable Long id,
                                Authentication authentication,
                                RedirectAttributes ra,
                                @RequestHeader(value = "Referer", required = false) String referer) {
    if (!isSupervisor(authentication)) {
      ra.addFlashAttribute("error", "No tienes permiso para marcar incumplimiento. Requiere supervisor financiero.");
      return redirectBack(referer, "/billing/accounts-receivable");
    }
    try {
      var commitment = receivableCommitmentService.markBroken(id);
      ra.addFlashAttribute("warning", "Compromiso marcado como incumplido.");
      return redirectBack(referer, "/billing/invoices/" + commitment.getInvoice().getId());
    } catch (IllegalArgumentException ex) {
      ra.addFlashAttribute("error", ex.getMessage());
      return redirectBack(referer, "/billing/accounts-receivable");
    }
  }

  @PostMapping("/commitments/{id}/cancel")
  public String cancelCommitment(@PathVariable Long id,
                                 Authentication authentication,
                                 RedirectAttributes ra,
                                 @RequestHeader(value = "Referer", required = false) String referer) {
    if (!isSupervisor(authentication)) {
      ra.addFlashAttribute("error", "No tienes permiso para cancelar compromisos. Requiere supervisor financiero.");
      return redirectBack(referer, "/billing/accounts-receivable");
    }
    try {
      var commitment = receivableCommitmentService.cancel(id);
      ra.addFlashAttribute("success", "Compromiso cancelado.");
      return redirectBack(referer, "/billing/invoices/" + commitment.getInvoice().getId());
    } catch (IllegalArgumentException ex) {
      ra.addFlashAttribute("error", ex.getMessage());
      return redirectBack(referer, "/billing/accounts-receivable");
    }
  }

  private void addCompanyAttributes(Model model) {
    var profile = systemBrandingService.load();
    model.addAttribute("companyName", profile.companyName());
    model.addAttribute("companyTradeName", profile.companyTradeName());
    model.addAttribute("companyRnc", profile.companyRnc());
    model.addAttribute("companyAddress", profile.companyAddress());
    model.addAttribute("companyPhone", profile.companyPhone());
    model.addAttribute("companyEmail", profile.companyEmail());
    model.addAttribute("invoiceFooter", profile.invoiceFooter());
  }

  private void addCashControlAttributes(Model model) {
    var activeSession = cashSessionService.findActive().orElse(null);
    model.addAttribute("cashSessionRequired", billingService.isCashSessionRequired());
    model.addAttribute("cashSessionActive", activeSession != null);
    model.addAttribute("activeCashSession", activeSession);
    if (activeSession != null) {
      model.addAttribute("activeCashSummary", cashSessionService.buildSummary(activeSession));
    }
  }

  @PostMapping("/invoices/{id}/ncf")
  public String assignNcf(@PathVariable Long id,
                          @RequestParam String ncfType,
                          Authentication authentication,
                          RedirectAttributes ra,
                          @RequestHeader(value = "Referer", required = false) String referer) {
    if (!isSupervisor(authentication)) {
      ra.addFlashAttribute("error", "No tienes permiso para asignar NCF. Requiere supervisor financiero.");
      return redirectBack(referer, "/billing/invoices/" + id);
    }
    try {
      billingService.assignNcf(id, ncfType);
      ra.addFlashAttribute("success", "NCF asignado correctamente.");
    } catch (IllegalArgumentException ex) {
      ra.addFlashAttribute("error", ex.getMessage());
    }
    return redirectBack(referer, "/billing/invoices/" + id);
  }

  @PostMapping("/invoices/{id}/credit-note")
  public String createCreditNote(@PathVariable Long id,
                                 @RequestParam(required = false) String ncfType,
                                 Authentication authentication,
                                 RedirectAttributes ra,
                                 @RequestHeader(value = "Referer", required = false) String referer) {
    if (!isSupervisor(authentication)) {
      ra.addFlashAttribute("error", "No tienes permiso para crear notas de crédito. Requiere supervisor financiero.");
      return redirectBack(referer, "/billing/invoices/" + id);
    }
    try {
      criticalActionApprovalService.ensureApprovedOrRequest(
          "BILLING_CREDIT_NOTE",
          "Invoice",
          id,
          "Emisión de nota de crédito para factura " + id,
          authentication
      );
      Invoice credit = billingService.createCreditNote(id, ncfType);
      ra.addFlashAttribute("success", "Nota de crédito creada.");
      return "redirect:/billing/invoices/" + credit.getId();
    } catch (IllegalArgumentException ex) {
      ra.addFlashAttribute("error", ex.getMessage());
      return redirectBack(referer, "/billing/invoices/" + id);
    }
  }

  @PostMapping("/invoices/create")
  public String createInvoice(@RequestParam Long appointmentId,
                              RedirectAttributes ra,
                              @RequestHeader(value = "Referer", required = false) String referer) {
    try {
      Invoice invoice = billingService.createInvoiceForAppointment(appointmentId);
      ra.addFlashAttribute("success", "Factura creada correctamente.");
      return "redirect:/billing/invoices/" + invoice.getId();
    } catch (IllegalArgumentException ex) {
      ra.addFlashAttribute("error", ex.getMessage());
      return redirectBack(referer, "/appointments");
    }
  }

  @PostMapping("/invoices/create-from-charges")
  public String createInvoiceFromCharges(@RequestParam Long patientId,
                                         @RequestParam(name = "chargeIds", required = false) List<Long> chargeIds,
                                         RedirectAttributes ra,
                                         @RequestHeader(value = "Referer", required = false) String referer) {
    try {
      Invoice invoice = billingService.createInvoiceFromCharges(patientId, chargeIds);
      ra.addFlashAttribute("success", "Factura creada desde cargos clínicos.");
      return "redirect:/billing/invoices/" + invoice.getId();
    } catch (IllegalArgumentException ex) {
      ra.addFlashAttribute("error", ex.getMessage());
      return redirectBack(referer, "/billing/charges");
    }
  }

  @PostMapping("/invoices/{id}/pay")
  public String pay(@PathVariable Long id,
                    @RequestParam BigDecimal amount,
                    @RequestParam(required = false) BigDecimal cashReceived,
                    @RequestParam(required = false) BigDecimal cashChange,
                    @RequestParam PaymentMethod method,
                    @RequestParam(required = false) PaymentChannel channel,
                    @RequestParam(required = false) String provider,
                    @RequestParam(required = false) String reference,
                    @RequestParam(required = false) String authCode,
                    @RequestParam(required = false) String last4,
                    @RequestParam(required = false) String cardBrand,
                    @RequestParam(required = false) String terminalId,
                    @RequestParam(required = false) String batchId,
                    @RequestParam(required = false) String rrn,
                    @RequestParam(required = false) String notes,
                    RedirectAttributes ra,
                    @RequestHeader(value = "Referer", required = false) String referer) {
    try {
      billingService.recordPayment(id, new BillingService.PaymentRequest(
          amount,
          cashReceived,
          cashChange,
          method,
          channel,
          provider,
          reference,
          authCode,
          last4,
          cardBrand,
          terminalId,
          batchId,
          rrn,
          notes
      ));
      ra.addFlashAttribute("success", "Pago registrado correctamente.");
    } catch (IllegalArgumentException ex) {
      ra.addFlashAttribute("error", ex.getMessage());
    }
    return redirectBack(referer, "/billing/invoices/" + id);
  }

  @PostMapping("/invoices/{id}/pay-split")
  public String paySplit(@PathVariable Long id,
                         @RequestParam(name = "amount", required = false) List<BigDecimal> amounts,
                         @RequestParam(name = "cashReceived", required = false) List<BigDecimal> cashReceivedValues,
                         @RequestParam(name = "cashChange", required = false) List<BigDecimal> cashChangeValues,
                         @RequestParam(name = "method", required = false) List<PaymentMethod> methods,
                         @RequestParam(name = "channel", required = false) List<PaymentChannel> channels,
                         @RequestParam(name = "provider", required = false) List<String> providers,
                         @RequestParam(name = "reference", required = false) List<String> references,
                         @RequestParam(name = "authCode", required = false) List<String> authCodes,
                         @RequestParam(name = "last4", required = false) List<String> last4Values,
                         @RequestParam(name = "cardBrand", required = false) List<String> cardBrands,
                         @RequestParam(name = "terminalId", required = false) List<String> terminalIds,
                         @RequestParam(name = "batchId", required = false) List<String> batchIds,
                         @RequestParam(name = "rrn", required = false) List<String> rrns,
                         @RequestParam(name = "notes", required = false) List<String> notes,
                         RedirectAttributes ra,
                         @RequestHeader(value = "Referer", required = false) String referer) {
    try {
      int max = maxSize(amounts, methods, channels, providers, references, authCodes, notes);
      List<BillingService.PaymentRequest> lines = new ArrayList<>();
      for (int i = 0; i < max; i++) {
        BigDecimal amount = getAt(amounts, i);
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) continue;
        PaymentMethod method = getAt(methods, i);
        if (method == null) {
          throw new IllegalArgumentException("Cada línea de pago debe indicar método.");
        }
        lines.add(new BillingService.PaymentRequest(
            amount,
            getAt(cashReceivedValues, i),
            getAt(cashChangeValues, i),
            method,
            getAt(channels, i),
            getAt(providers, i),
            getAt(references, i),
            getAt(authCodes, i),
            getAt(last4Values, i),
            getAt(cardBrands, i),
            getAt(terminalIds, i),
            getAt(batchIds, i),
            getAt(rrns, i),
            getAt(notes, i)
        ));
      }

      List<com.baldwin.praecura.entity.Payment> created = billingService.recordSplitPayments(id, lines);
      ra.addFlashAttribute("success", "Pago mixto registrado (" + created.size() + " líneas).");
    } catch (IllegalArgumentException ex) {
      ra.addFlashAttribute("error", ex.getMessage());
    }
    return redirectBack(referer, "/billing/invoices/" + id);
  }

  @PostMapping("/payments/{id}/refund")
  public String refund(@PathVariable Long id,
                       @RequestParam BigDecimal amount,
                       @RequestParam PaymentMethod method,
                       @RequestParam(required = false) String reference,
                       @RequestParam(required = false) String notes,
                       Authentication authentication,
                       RedirectAttributes ra,
                       @RequestHeader(value = "Referer", required = false) String referer) {
    if (!isSupervisor(authentication)) {
      ra.addFlashAttribute("error", "No tienes permiso para reembolsos. Requiere supervisor financiero.");
      return redirectBack(referer, "/billing");
    }
    try {
      criticalActionApprovalService.ensureApprovedOrRequest(
          "BILLING_REFUND",
          "Payment",
          id,
          "Reembolso solicitado por " + amount + " vía " + method,
          authentication
      );
      billingService.refundPayment(id, new BillingService.RefundRequest(amount, method, reference, notes));
      ra.addFlashAttribute("success", "Reembolso registrado correctamente.");
    } catch (IllegalArgumentException ex) {
      ra.addFlashAttribute("error", ex.getMessage());
    }
    return redirectBack(referer, "/billing");
  }

  @PostMapping("/invoices/{id}/links/azul")
  public String createAzulLink(@PathVariable Long id,
                               @RequestParam String url,
                               @RequestParam(required = false) String reference,
                               @RequestParam(required = false) String notes,
                               RedirectAttributes ra,
                               @RequestHeader(value = "Referer", required = false) String referer) {
    try {
      paymentLinkService.createAzulLink(id, url, reference, notes);
      ra.addFlashAttribute("success", "Link de pago AZUL registrado.");
    } catch (IllegalArgumentException | IllegalStateException ex) {
      ra.addFlashAttribute("error", ex.getMessage());
    }
    return redirectBack(referer, "/billing/invoices/" + id);
  }

  @PostMapping("/invoices/{id}/links/cardnet")
  public String createCardNet(@PathVariable Long id,
                              RedirectAttributes ra,
                              Model model) {
    try {
      PaymentLink link = paymentLinkService.createCardNetSession(id);
      model.addAttribute("authorizeUrl", cardNetClient.buildAuthorizeUrl());
      model.addAttribute("sessionId", link.getSessionId());
      model.addAttribute("invoiceId", id);
      return "billing/cardnet-redirect";
    } catch (IllegalArgumentException | IllegalStateException ex) {
      ra.addFlashAttribute("error", ex.getMessage());
      return "redirect:/billing/invoices/" + id;
    }
  }

  @PostMapping("/links/{id}/mark-paid")
  public String markPaid(@PathVariable Long id,
                         @RequestParam PaymentMethod method,
                         @RequestParam(required = false) String reference,
                         RedirectAttributes ra,
                         @RequestHeader(value = "Referer", required = false) String referer) {
    try {
      paymentLinkService.markPaid(id, method, reference);
      ra.addFlashAttribute("success", "Pago registrado desde link.");
    } catch (IllegalArgumentException ex) {
      ra.addFlashAttribute("error", ex.getMessage());
    }
    return redirectBack(referer, "/billing");
  }

  @GetMapping("/cardnet/return")
  public String cardNetReturn(@RequestParam("SESSION") String sessionId,
                              RedirectAttributes ra) {
    try {
      PaymentLink link = paymentLinkService.handleCardNetReturn(sessionId);
      ra.addFlashAttribute("success", "Pago CardNET procesado. Estado: " + link.getStatus().label());
      return "redirect:/billing/invoices/" + link.getInvoice().getId();
    } catch (Exception ex) {
      ra.addFlashAttribute("error", "No se pudo procesar el retorno de CardNET.");
      return "redirect:/billing";
    }
  }

  @GetMapping("/cardnet/cancel")
  public String cardNetCancel(@RequestParam("SESSION") String sessionId,
                              RedirectAttributes ra) {
    try {
      PaymentLink link = paymentLinkService.cancelCardNet(sessionId);
      ra.addFlashAttribute("warning", "Pago CardNET cancelado.");
      return "redirect:/billing/invoices/" + link.getInvoice().getId();
    } catch (Exception ex) {
      ra.addFlashAttribute("error", "No se pudo cancelar el pago de CardNET.");
      return "redirect:/billing";
    }
  }

  @PostMapping("/invoices/{id}/void")
  public String voidInvoice(@PathVariable Long id,
                            Authentication authentication,
                            RedirectAttributes ra,
                            @RequestHeader(value = "Referer", required = false) String referer) {
    if (!isSupervisor(authentication)) {
      ra.addFlashAttribute("error", "No tienes permiso para anular facturas. Requiere supervisor financiero.");
      return redirectBack(referer, "/billing/invoices/" + id);
    }
    try {
      criticalActionApprovalService.ensureApprovedOrRequest(
          "BILLING_VOID_INVOICE",
          "Invoice",
          id,
          "Solicitud de anulación de factura " + id,
          authentication
      );
      billingService.voidInvoice(id);
      ra.addFlashAttribute("success", "Factura anulada correctamente.");
    } catch (IllegalArgumentException ex) {
      ra.addFlashAttribute("error", ex.getMessage());
    }
    return redirectBack(referer, "/billing/invoices/" + id);
  }

  private void streamInvoicesByCriteriaBatches(InvoiceStatus status,
                                               Long patientId,
                                               Long appointmentId,
                                               LocalDateTime fromDt,
                                               LocalDateTime toDt,
                                               Consumer<List<Invoice>> batchConsumer) {
    Sort sort = Sort.by(Sort.Direction.DESC, "createdAt")
        .and(Sort.by(Sort.Direction.DESC, "id"));
    int page = 0;
    Page<Invoice> chunk;
    do {
      chunk = invoiceRepository.search(
          status,
          patientId,
          appointmentId,
          fromDt,
          toDt,
          PageRequest.of(page, EXPORT_BATCH_SIZE, sort)
      );
      if (!chunk.getContent().isEmpty()) {
        batchConsumer.accept(chunk.getContent());
      }
      page++;
    } while (chunk.hasNext());
  }

  private void streamAccountsReceivableBatches(String query,
                                               Consumer<List<Invoice>> batchConsumer) {
    Sort sort = Sort.by(Sort.Direction.ASC, "createdAt")
        .and(Sort.by(Sort.Direction.ASC, "id"));
    int page = 0;
    Page<Invoice> chunk;
    do {
      chunk = invoiceRepository.searchAccountsReceivable(
          query,
          InvoiceStatus.VOID,
          PageRequest.of(page, EXPORT_BATCH_SIZE, sort)
      );
      if (!chunk.getContent().isEmpty()) {
        batchConsumer.accept(chunk.getContent());
      }
      page++;
    } while (chunk.hasNext());
  }

  private void forEachInvoiceIdBatch(List<Long> invoiceIds, Consumer<List<Long>> batchConsumer) {
    if (invoiceIds == null || invoiceIds.isEmpty()) return;
    for (int i = 0; i < invoiceIds.size(); i += EXPORT_BATCH_SIZE) {
      int end = Math.min(invoiceIds.size(), i + EXPORT_BATCH_SIZE);
      batchConsumer.accept(invoiceIds.subList(i, end));
    }
  }

  private String redirectBack(String referer, String fallbackPath) {
    if (fallbackPath == null || fallbackPath.isBlank()) fallbackPath = "/";

    if (referer == null || referer.isBlank()) {
      return "redirect:" + fallbackPath;
    }

    if (referer.startsWith("/") && !referer.startsWith("//")) {
      return "redirect:" + referer;
    }

    try {
      URI uri = URI.create(referer);
      String path = uri.getPath();
      String query = uri.getQuery();
      String target = (path != null ? path : fallbackPath);
      if (query != null && !query.isBlank()) {
        target = target + "?" + query;
      }
      return "redirect:" + target;
    } catch (Exception ex) {
      return "redirect:" + fallbackPath;
    }
  }

  /**
   * Exporta trazas técnicas sin exponer identificadores completos de pagos.
   */
  private static String maskToken(String value, int visibleTail) {
    if (value == null || value.isBlank()) return value;
    String trimmed = value.trim();
    int keep = Math.max(0, visibleTail);
    if (trimmed.length() <= keep) return "***";
    return "***" + trimmed.substring(trimmed.length() - keep);
  }

  private void prepareCsvResponse(HttpServletResponse response, String filename) {
    tabularExportService.prepareCsvResponse(response, filename);
  }

  private java.io.PrintWriter csvWriter(HttpServletResponse response) throws Exception {
    return tabularExportService.csvWriter(response);
  }

  private void writeBom(java.io.PrintWriter out) {
    tabularExportService.writeBom(out);
  }

  private void writeCsvLine(java.io.PrintWriter out, Object... values) {
    tabularExportService.writeCsvLine(out, true, values);
  }

  private boolean isSupervisor(Authentication authentication) {
    boolean isAdmin = SecurityRoleUtils.hasAdminAuthority(authentication);
    if (isAdmin) return true;
    if (authentication == null || !authentication.isAuthenticated()) return false;
    return billingSupervisorAccessService.isSupervisor(authentication.getName());
  }

  private static int maxSize(List<?>... lists) {
    int max = 0;
    if (lists == null) return 0;
    for (List<?> list : lists) {
      if (list != null && list.size() > max) {
        max = list.size();
      }
    }
    return max;
  }

  private static <T> T getAt(List<T> list, int index) {
    if (list == null) return null;
    if (index < 0 || index >= list.size()) return null;
    return list.get(index);
  }
}
