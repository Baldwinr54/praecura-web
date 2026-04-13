package com.baldwin.praecura.service;

import com.baldwin.praecura.config.RequestContext;
import com.baldwin.praecura.config.RequestContextHolder;
import com.baldwin.praecura.entity.Appointment;
import com.baldwin.praecura.entity.BillingCharge;
import com.baldwin.praecura.entity.BillingChargeCategory;
import com.baldwin.praecura.entity.BillingChargeStatus;
import com.baldwin.praecura.entity.Invoice;
import com.baldwin.praecura.entity.InvoiceItem;
import com.baldwin.praecura.entity.InvoiceStatus;
import com.baldwin.praecura.entity.InvoiceType;
import com.baldwin.praecura.entity.MedicalService;
import com.baldwin.praecura.entity.NcfStatus;
import com.baldwin.praecura.entity.Payment;
import com.baldwin.praecura.entity.PaymentChannel;
import com.baldwin.praecura.entity.PaymentMethod;
import com.baldwin.praecura.entity.PaymentStatus;
import com.baldwin.praecura.entity.Patient;
import com.baldwin.praecura.repository.AppointmentRepository;
import com.baldwin.praecura.repository.BillingChargeRepository;
import com.baldwin.praecura.repository.InvoiceRepository;
import com.baldwin.praecura.repository.MedicalServiceRepository;
import com.baldwin.praecura.repository.PatientRepository;
import com.baldwin.praecura.repository.PaymentRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BillingService {

  private static final Pattern SENSITIVE_CARD_KEYWORDS = Pattern.compile(
      "\\b(cvv|cvc|cvv2|security\\s*code|codigo\\s*de\\s*seguridad|card\\s*number|numero\\s*de\\s*tarjeta|pan)\\b",
      Pattern.CASE_INSENSITIVE
  );

  private final InvoiceRepository invoiceRepository;
  private final PaymentRepository paymentRepository;
  private final AppointmentRepository appointmentRepository;
  private final BillingChargeRepository billingChargeRepository;
  private final PatientRepository patientRepository;
  private final MedicalServiceRepository medicalServiceRepository;
  private final AuditService auditService;
  private final FiscalSequenceService fiscalSequenceService;
  private final CashSessionService cashSessionService;
  private final ReceivableCommitmentService receivableCommitmentService;

  @Value("${praecura.billing.currency:DOP}")
  private String defaultCurrency;

  @Value("${praecura.billing.tax-rate:0.18}")
  private BigDecimal taxRate;

  @Value("${praecura.billing.require-cash-session:false}")
  private boolean requireCashSession;

  @Value("${praecura.billing.require-ncf-before-payment:true}")
  private boolean requireNcfBeforePayment;

  public BillingService(InvoiceRepository invoiceRepository,
                        PaymentRepository paymentRepository,
                        AppointmentRepository appointmentRepository,
                        BillingChargeRepository billingChargeRepository,
                        PatientRepository patientRepository,
                        MedicalServiceRepository medicalServiceRepository,
                        AuditService auditService,
                        FiscalSequenceService fiscalSequenceService,
                        CashSessionService cashSessionService,
                        ReceivableCommitmentService receivableCommitmentService) {
    this.invoiceRepository = invoiceRepository;
    this.paymentRepository = paymentRepository;
    this.appointmentRepository = appointmentRepository;
    this.billingChargeRepository = billingChargeRepository;
    this.patientRepository = patientRepository;
    this.medicalServiceRepository = medicalServiceRepository;
    this.auditService = auditService;
    this.fiscalSequenceService = fiscalSequenceService;
    this.cashSessionService = cashSessionService;
    this.receivableCommitmentService = receivableCommitmentService;
  }

  @Transactional
  public Invoice createInvoiceForAppointment(Long appointmentId) {
    Appointment appointment = appointmentRepository.findById(appointmentId)
        .orElseThrow(() -> new IllegalArgumentException("La cita no existe."));

    if (!appointment.isActive()) {
      throw new IllegalArgumentException("La cita está archivada.");
    }

    return invoiceRepository.findFirstByAppointmentIdAndStatusNotOrderByCreatedAtDesc(appointmentId, InvoiceStatus.VOID)
        .orElseGet(() -> createInvoiceForAppointmentInternal(appointment));
  }

  @Transactional(readOnly = true)
  public Page<BillingCharge> searchCharges(BillingChargeStatus status,
                                           Long patientId,
                                           Long appointmentId,
                                           LocalDateTime fromDt,
                                           LocalDateTime toDt,
                                           Pageable pageable) {
    return billingChargeRepository.search(status, patientId, appointmentId, fromDt, toDt, pageable);
  }

  @Transactional
  public BillingCharge createCharge(ChargeRequest request) {
    if (request == null) {
      throw new IllegalArgumentException("Datos de cargo inválidos.");
    }
    if (request.patientId() == null) {
      throw new IllegalArgumentException("Debes indicar el paciente del cargo.");
    }

    Patient patient = patientRepository.findById(request.patientId())
        .orElseThrow(() -> new IllegalArgumentException("El paciente no existe."));

    Appointment appointment = null;
    if (request.appointmentId() != null) {
      appointment = appointmentRepository.findById(request.appointmentId())
          .orElseThrow(() -> new IllegalArgumentException("La cita no existe."));
      if (!Objects.equals(appointment.getPatient().getId(), patient.getId())) {
        throw new IllegalArgumentException("La cita no corresponde al paciente indicado.");
      }
    }

    MedicalService service = null;
    if (request.serviceId() != null) {
      service = medicalServiceRepository.findById(request.serviceId())
          .orElseThrow(() -> new IllegalArgumentException("El servicio no existe."));
    }

    int quantity = request.quantity() != null ? request.quantity() : 1;
    if (quantity <= 0) {
      throw new IllegalArgumentException("La cantidad debe ser mayor a 0.");
    }

    BigDecimal unitPrice = resolveUnitPrice(request.unitPrice(), service, appointment);
    if (unitPrice.compareTo(BigDecimal.ZERO) < 0) {
      throw new IllegalArgumentException("El precio unitario no puede ser negativo.");
    }

    BigDecimal appliedTaxRate = resolveTaxRate(request.taxRate());
    BigDecimal subtotal = scale(unitPrice.multiply(BigDecimal.valueOf(quantity)));
    BigDecimal tax = scale(subtotal.multiply(appliedTaxRate));
    BigDecimal discount = scaleOrNull(request.discount());
    if (discount == null) discount = BigDecimal.ZERO;
    if (discount.compareTo(BigDecimal.ZERO) < 0) {
      throw new IllegalArgumentException("El descuento no puede ser negativo.");
    }
    BigDecimal total = scale(subtotal.add(tax).subtract(discount));
    if (total.compareTo(BigDecimal.ZERO) < 0) {
      throw new IllegalArgumentException("El total del cargo no puede ser negativo.");
    }

    BillingCharge charge = new BillingCharge();
    charge.setPatient(patient);
    charge.setAppointment(appointment);
    charge.setService(service);
    charge.setCategory(request.category() != null ? request.category() : inferCategory(appointment, service));
    charge.setDescription(resolveChargeDescription(request.description(), service, charge.getCategory()));
    charge.setQuantity(quantity);
    charge.setUnitPrice(scale(unitPrice));
    charge.setSubtotal(subtotal);
    charge.setTax(tax);
    charge.setDiscount(discount);
    charge.setTotal(total);
    charge.setCurrency(resolveCurrency(request.currency()));
    charge.setStatus(BillingChargeStatus.OPEN);
    charge.setSource(resolveChargeSource(request.source()));
    charge.setNotes(trimToNull(request.notes()));
    charge.setPerformedAt(request.performedAt() != null ? request.performedAt() : LocalDateTime.now());
    charge.setCreatedBy(currentUsername());
    charge.setUpdatedBy(currentUsername());
    charge.setCreatedAt(LocalDateTime.now());
    charge.setUpdatedAt(LocalDateTime.now());

    billingChargeRepository.save(charge);

    auditService.log("CHARGE_CREATED",
        "BillingCharge",
        charge.getId(),
        "patientId=" + patient.getId() + ", total=" + charge.getTotal() + ", source=" + charge.getSource());

    return charge;
  }

  @Transactional
  public BillingCharge cancelCharge(Long chargeId) {
    BillingCharge charge = billingChargeRepository.findById(chargeId)
        .orElseThrow(() -> new IllegalArgumentException("El cargo no existe."));

    if (charge.getStatus() == BillingChargeStatus.BILLED) {
      throw new IllegalArgumentException("No se puede cancelar un cargo ya facturado.");
    }
    if (charge.getStatus() == BillingChargeStatus.CANCELED) {
      return charge;
    }

    charge.setStatus(BillingChargeStatus.CANCELED);
    charge.setUpdatedAt(LocalDateTime.now());
    charge.setUpdatedBy(currentUsername());
    billingChargeRepository.save(charge);

    auditService.log("CHARGE_CANCELED",
        "BillingCharge",
        charge.getId(),
        "patientId=" + (charge.getPatient() != null ? charge.getPatient().getId() : null));

    return charge;
  }

  @Transactional
  public Invoice createInvoiceFromCharges(Long patientId, List<Long> chargeIds) {
    if (chargeIds == null || chargeIds.isEmpty()) {
      throw new IllegalArgumentException("Debes seleccionar al menos un cargo.");
    }

    List<Long> uniqueIds = chargeIds.stream().filter(Objects::nonNull).distinct().toList();
    if (uniqueIds.isEmpty()) {
      throw new IllegalArgumentException("Debes seleccionar cargos válidos.");
    }

    List<BillingCharge> charges = new ArrayList<>(billingChargeRepository.findByIdIn(uniqueIds));
    if (charges.size() != uniqueIds.size()) {
      throw new IllegalArgumentException("Algunos cargos seleccionados no existen.");
    }
    charges.sort((a, b) -> a.getCreatedAt().compareTo(b.getCreatedAt()));

    Patient patient = patientRepository.findById(patientId)
        .orElseThrow(() -> new IllegalArgumentException("El paciente no existe."));

    for (BillingCharge charge : charges) {
      if (charge.getStatus() != BillingChargeStatus.OPEN) {
        throw new IllegalArgumentException("Solo se pueden facturar cargos pendientes.");
      }
      if (charge.getPatient() == null || !Objects.equals(charge.getPatient().getId(), patient.getId())) {
        throw new IllegalArgumentException("Todos los cargos deben pertenecer al mismo paciente.");
      }
    }

    Appointment appointment = resolveInvoiceAppointment(charges);
    return createInvoiceFromChargesInternal(patient, appointment, charges);
  }

  @Transactional
  public Payment recordPayment(Long invoiceId, PaymentRequest request) {
    if (request == null) {
      throw new IllegalArgumentException("Datos de pago inválidos.");
    }

    Invoice invoice = invoiceRepository.findWithItems(invoiceId)
        .orElseThrow(() -> new IllegalArgumentException("La factura no existe."));

    if (invoice.getStatus() == InvoiceStatus.VOID) {
      throw new IllegalArgumentException("La factura está anulada.");
    }
    if (requireNcfBeforePayment
        && invoice.getInvoiceType() == InvoiceType.INVOICE
        && trimToNull(invoice.getNcf()) == null) {
      throw new IllegalArgumentException("Debes asignar NCF antes de registrar pagos.");
    }

    BigDecimal amount = scale(request.amount());
    if (amount.compareTo(BigDecimal.ZERO) <= 0) {
      throw new IllegalArgumentException("El monto debe ser mayor a 0.");
    }

    BigDecimal balance = nz(invoice.getBalance());
    if (amount.compareTo(balance) > 0) {
      throw new IllegalArgumentException("El monto excede el balance pendiente.");
    }

    PaymentMethod method = request.method() != null ? request.method() : PaymentMethod.CASH;
    PaymentChannel channel = resolveChannel(request.channel(), method);
    String provider = sanitizePaymentField("Proveedor", request.provider(), 60, false);
    String reference = sanitizePaymentField("Referencia", request.reference(), 120, true);
    String authCode = sanitizePaymentField("Código de autorización", request.authCode(), 60, true);
    String last4 = normalizeLast4(request.last4());
    String cardBrand = sanitizePaymentField("Marca de tarjeta", request.cardBrand(), 40, false);
    String terminalId = sanitizePaymentField("Terminal POS", request.terminalId(), 60, true);
    String batchId = sanitizePaymentField("Batch", request.batchId(), 60, true);
    String rrn = sanitizePaymentField("RRN", request.rrn(), 60, true);
    String notes = sanitizePaymentNotes(request.notes());

    Payment payment = new Payment();
    payment.setInvoice(invoice);
    payment.setPatient(invoice.getPatient());
    payment.setAppointment(invoice.getAppointment());
    payment.setAmount(amount);
    payment.setCurrency(invoice.getCurrency());
    payment.setMethod(method);
    payment.setChannel(channel);
    payment.setStatus(PaymentStatus.CAPTURED);
    payment.setProvider(provider);
    payment.setExternalId(reference);
    payment.setAuthCode(authCode);
    payment.setLast4(last4);
    payment.setCardBrand(cardBrand);
    payment.setTerminalId(terminalId);
    payment.setBatchId(batchId);
    payment.setRrn(rrn);
    payment.setNotes(notes);
    payment.setPaidAt(LocalDateTime.now());

    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth != null && auth.isAuthenticated()) {
      payment.setUsername(auth.getName());
    }

    RequestContext ctx = RequestContextHolder.get();
    if (ctx != null) {
      payment.setRequestId(ctx.getRequestId());
      payment.setIpAddress(ctx.getIpAddress());
      payment.setUserAgent(ctx.getUserAgent());
    }

    if (payment.getMethod() == PaymentMethod.CASH) {
      BigDecimal received = scaleOrNull(request.cashReceived());
      BigDecimal change = scaleOrNull(request.cashChange());
      if (received != null) {
        if (received.compareTo(amount) < 0) {
          throw new IllegalArgumentException("El monto recibido no puede ser menor al total.");
        }
        payment.setCashReceived(received);
        if (change == null) {
          BigDecimal calc = received.subtract(amount);
          if (calc.compareTo(BigDecimal.ZERO) > 0) {
            change = calc;
          }
        }
        payment.setCashChange(change);
      }
      var sessionOpt = cashSessionService.findActive();
      if (sessionOpt.isPresent()) {
        payment.setCashSession(sessionOpt.get());
      } else if (requireCashSession) {
        throw new IllegalArgumentException("No hay caja abierta para registrar pagos en efectivo.");
      }
    }

    paymentRepository.save(payment);
    refreshInvoiceFinancials(invoice);

    auditService.log("PAYMENT_CAPTURED",
        "Invoice",
        invoice.getId(),
        "amount=" + amount + ", method=" + request.method() + ", balance=" + invoice.getBalance());

    return payment;
  }

  @Transactional
  public List<Payment> recordSplitPayments(Long invoiceId, List<PaymentRequest> requests) {
    if (requests == null || requests.isEmpty()) {
      throw new IllegalArgumentException("Debes registrar al menos una línea de pago.");
    }

    List<PaymentRequest> normalized = requests.stream()
        .filter(Objects::nonNull)
        .filter(r -> r.amount() != null && r.amount().compareTo(BigDecimal.ZERO) > 0)
        .toList();

    if (normalized.isEmpty()) {
      throw new IllegalArgumentException("Debes indicar montos válidos para el pago mixto.");
    }

    BigDecimal total = BigDecimal.ZERO;
    List<Payment> created = new ArrayList<>();
    for (PaymentRequest request : normalized) {
      total = total.add(scale(request.amount()));
      created.add(recordPayment(invoiceId, request));
    }

    auditService.log("PAYMENT_CAPTURED_SPLIT",
        "Invoice",
        invoiceId,
        "lines=" + created.size() + ", total=" + scale(total));

    return created;
  }

  @Transactional
  public Payment refundPayment(Long paymentId, RefundRequest request) {
    if (request == null) {
      throw new IllegalArgumentException("Datos de reembolso inválidos.");
    }

    Payment payment = paymentRepository.findById(paymentId)
        .orElseThrow(() -> new IllegalArgumentException("El pago no existe."));

    if (payment.getStatus() == PaymentStatus.REFUNDED) {
      throw new IllegalArgumentException("El pago ya fue reembolsado.");
    }

    BigDecimal amount = scale(request.amount());
    if (amount.compareTo(BigDecimal.ZERO) <= 0) {
      throw new IllegalArgumentException("El monto a reembolsar debe ser mayor a 0.");
    }

    BigDecimal maxRefundable = scale(payment.getAmount().subtract(nz(payment.getRefundedAmount())));
    if (amount.compareTo(maxRefundable) > 0) {
      throw new IllegalArgumentException("El monto excede el máximo reembolsable.");
    }

    BigDecimal newRefunded = scale(nz(payment.getRefundedAmount()).add(amount));
    payment.setRefundedAmount(newRefunded);
    payment.setRefundedAt(LocalDateTime.now());
    payment.setRefundMethod(request.method());
    payment.setRefundReference(sanitizePaymentField("Referencia de reembolso", request.reference(), 120, true));
    appendRefundNotes(payment, request.notes());

    if (request.method() == PaymentMethod.CASH) {
      var sessionOpt = cashSessionService.findActive();
      if (sessionOpt.isPresent()) {
        payment.setRefundSession(sessionOpt.get());
      } else if (requireCashSession) {
        throw new IllegalArgumentException("No hay caja abierta para registrar reembolsos en efectivo.");
      }
    }

    if (newRefunded.compareTo(payment.getAmount()) >= 0) {
      payment.setStatus(PaymentStatus.REFUNDED);
    }

    paymentRepository.save(payment);

    Invoice invoice = payment.getInvoice();
    if (invoice != null) {
      refreshInvoiceFinancials(invoice);
    }

    auditService.log("PAYMENT_REFUNDED",
        "Payment",
        payment.getId(),
        "amount=" + amount + ", method=" + request.method() + ", invoiceId=" + (invoice != null ? invoice.getId() : null));

    return payment;
  }

  @Transactional
  public Invoice voidInvoice(Long invoiceId) {
    Invoice invoice = invoiceRepository.findWithItems(invoiceId)
        .orElseThrow(() -> new IllegalArgumentException("La factura no existe."));

    if (invoice.getStatus() == InvoiceStatus.VOID) {
      return invoice;
    }

    long paymentCount = paymentRepository.countByInvoiceId(invoiceId);
    if (paymentCount > 0) {
      throw new IllegalArgumentException("No se puede anular una factura con pagos registrados.");
    }

    invoice.setStatus(InvoiceStatus.VOID);
    invoice.setBalance(BigDecimal.ZERO);
    if (invoice.getNcf() != null && invoice.getNcfStatus() == NcfStatus.ISSUED) {
      invoice.setNcfStatus(NcfStatus.VOIDED);
    }
    invoice.setUpdatedAt(LocalDateTime.now());
    invoice.setUpdatedBy(currentUsername());
    invoiceRepository.save(invoice);

    List<BillingCharge> billedCharges = billingChargeRepository.findByInvoiceIdAndStatusOrderByCreatedAtAsc(
        invoiceId,
        BillingChargeStatus.BILLED
    );
    for (BillingCharge charge : billedCharges) {
      charge.setInvoice(null);
      charge.setStatus(BillingChargeStatus.OPEN);
      charge.setUpdatedAt(LocalDateTime.now());
      charge.setUpdatedBy(currentUsername());
      billingChargeRepository.save(charge);
    }

    auditService.log("INVOICE_VOIDED",
        "Invoice",
        invoice.getId(),
        "appointmentId=" + (invoice.getAppointment() != null ? invoice.getAppointment().getId() : null) + ", reopenedCharges=" + billedCharges.size());

    receivableCommitmentService.cancelPendingForInvoice(invoice.getId(), "Factura anulada");

    return invoice;
  }

  private Invoice createInvoiceForAppointmentInternal(Appointment appointment) {
    if (appointment == null) {
      throw new IllegalArgumentException("La cita no existe.");
    }
    if (appointment.getPatient() == null) {
      throw new IllegalArgumentException("La cita no tiene paciente asociado.");
    }

    List<BillingCharge> charges = new ArrayList<>(
        billingChargeRepository.findByAppointmentIdAndStatusOrderByCreatedAtAsc(
            appointment.getId(),
            BillingChargeStatus.OPEN
        )
    );

    if (charges.isEmpty()) {
      BillingCharge fallback = createFallbackChargeFromAppointment(appointment);
      if (fallback != null) {
        charges.add(fallback);
      }
    }

    if (charges.isEmpty()) {
      throw new IllegalArgumentException("La cita no tiene cargos pendientes para facturar.");
    }

    return createInvoiceFromChargesInternal(appointment.getPatient(), appointment, charges);
  }

  private Invoice createInvoiceFromChargesInternal(Patient patient,
                                                   Appointment appointment,
                                                   List<BillingCharge> charges) {
    if (patient == null) {
      throw new IllegalArgumentException("No se puede facturar sin paciente.");
    }
    if (charges == null || charges.isEmpty()) {
      throw new IllegalArgumentException("No hay cargos pendientes para facturar.");
    }

    String currency = resolveCurrency(charges.get(0).getCurrency());
    BigDecimal subtotal = BigDecimal.ZERO;
    BigDecimal tax = BigDecimal.ZERO;
    BigDecimal discount = BigDecimal.ZERO;
    List<InvoiceItem> items = new ArrayList<>();

    for (BillingCharge charge : charges) {
      if (charge.getStatus() != BillingChargeStatus.OPEN) {
        throw new IllegalArgumentException("Hay cargos que no están pendientes.");
      }
      if (charge.getPatient() == null || !Objects.equals(charge.getPatient().getId(), patient.getId())) {
        throw new IllegalArgumentException("Todos los cargos deben pertenecer al mismo paciente.");
      }
      String chargeCurrency = resolveCurrency(charge.getCurrency());
      if (!currency.equals(chargeCurrency)) {
        throw new IllegalArgumentException("No se pueden combinar cargos con distintas monedas.");
      }

      InvoiceItem item = new InvoiceItem();
      item.setService(charge.getService());
      item.setAppointment(charge.getAppointment());
      item.setDescription(charge.getDescription());
      item.setQuantity(charge.getQuantity());
      item.setUnitPrice(scale(charge.getUnitPrice()));
      item.setTotal(scale(charge.getSubtotal()));
      items.add(item);

      subtotal = subtotal.add(scale(charge.getSubtotal()));
      tax = tax.add(scale(charge.getTax()));
      discount = discount.add(scale(charge.getDiscount()));
    }

    subtotal = scale(subtotal);
    tax = scale(tax);
    discount = scale(discount);
    BigDecimal total = scale(subtotal.add(tax).subtract(discount));
    if (total.compareTo(BigDecimal.ZERO) < 0) {
      throw new IllegalArgumentException("El total de la factura no puede ser negativo.");
    }

    Invoice invoice = new Invoice();
    invoice.setPatient(patient);
    invoice.setAppointment(appointment);
    invoice.setStatus(InvoiceStatus.ISSUED);
    invoice.setInvoiceType(InvoiceType.INVOICE);
    invoice.setCurrency(currency);
    invoice.setIssuedAt(LocalDateTime.now());
    invoice.setCreatedBy(currentUsername());
    invoice.setUpdatedBy(currentUsername());
    invoice.setFiscalName(resolveFiscalName(patient));
    invoice.setFiscalTaxId(trimToNull(patient.getBillingTaxId()));
    invoice.setFiscalAddress(trimToNull(patient.getBillingAddress()));
    invoice.setSubtotal(subtotal);
    invoice.setTax(tax);
    invoice.setDiscount(discount);
    invoice.setTotal(total);
    invoice.setBalance(total);

    invoiceRepository.save(invoice);
    for (InvoiceItem item : items) {
      item.setInvoice(invoice);
      invoice.getItems().add(item);
    }
    invoiceRepository.save(invoice);

    for (BillingCharge charge : charges) {
      charge.setInvoice(invoice);
      charge.setStatus(BillingChargeStatus.BILLED);
      charge.setUpdatedAt(LocalDateTime.now());
      charge.setUpdatedBy(currentUsername());
      billingChargeRepository.save(charge);
    }

    auditService.log("INVOICE_CREATED",
        "Invoice",
        invoice.getId(),
        "appointmentId=" + (appointment != null ? appointment.getId() : null) + ", total=" + total + ", items=" + items.size());

    return invoice;
  }

  private BillingCharge createFallbackChargeFromAppointment(Appointment appointment) {
    if (appointment == null || appointment.getPatient() == null) return null;

    MedicalService service = appointment.getService();
    BigDecimal servicePrice = (service != null) ? scaleOrNull(service.getPrice()) : null;
    if (servicePrice == null || servicePrice.compareTo(BigDecimal.ZERO) <= 0) {
      return null;
    }

    BigDecimal rate = resolveTaxRate(null);
    BigDecimal subtotal = scale(servicePrice);
    BigDecimal tax = scale(subtotal.multiply(rate));
    BigDecimal discount = BigDecimal.ZERO;
    BigDecimal total = scale(subtotal.add(tax));

    BillingCharge charge = new BillingCharge();
    charge.setPatient(appointment.getPatient());
    charge.setAppointment(appointment);
    charge.setService(service);
    charge.setCategory(inferCategory(appointment, service));
    charge.setDescription(resolveChargeDescription(service != null ? service.getName() : null, service, BillingChargeCategory.CONSULTATION));
    charge.setQuantity(1);
    charge.setUnitPrice(servicePrice);
    charge.setSubtotal(subtotal);
    charge.setTax(tax);
    charge.setDiscount(discount);
    charge.setTotal(total);
    charge.setCurrency(resolveCurrency(null));
    charge.setStatus(BillingChargeStatus.OPEN);
    charge.setSource("APPOINTMENT");
    charge.setPerformedAt(appointment.getCompletedAt() != null ? appointment.getCompletedAt() : appointment.getScheduledAt());
    charge.setCreatedBy(currentUsername());
    charge.setUpdatedBy(currentUsername());
    charge.setCreatedAt(LocalDateTime.now());
    charge.setUpdatedAt(LocalDateTime.now());
    billingChargeRepository.save(charge);

    auditService.log("CHARGE_CREATED_AUTO",
        "BillingCharge",
        charge.getId(),
        "appointmentId=" + appointment.getId() + ", total=" + total);
    return charge;
  }

  @Transactional
  public Invoice assignNcf(Long invoiceId, String ncfType) {
    Invoice invoice = invoiceRepository.findWithItems(invoiceId)
        .orElseThrow(() -> new IllegalArgumentException("La factura no existe."));

    if (invoice.getStatus() == InvoiceStatus.VOID) {
      throw new IllegalArgumentException("La factura está anulada.");
    }
    if (invoice.getNcf() != null && !invoice.getNcf().isBlank()) {
      throw new IllegalArgumentException("La factura ya tiene NCF asignado.");
    }
    String type = trimToNull(ncfType);
    if (type == null) {
      throw new IllegalArgumentException("Selecciona el tipo de comprobante (NCF).");
    }
    type = type.toUpperCase();
    validateNcfTypeForInvoice(invoice, type);

    String ncf = fiscalSequenceService.nextNcf(type);
    invoice.setNcfType(type);
    invoice.setNcf(ncf);
    invoice.setNcfStatus(NcfStatus.ISSUED);
    invoice.setNcfIssuedAt(LocalDateTime.now());
    invoice.setUpdatedAt(LocalDateTime.now());
    invoice.setUpdatedBy(currentUsername());
    invoiceRepository.save(invoice);

    auditService.log("NCF_ASSIGNED",
        "Invoice",
        invoice.getId(),
        "ncf=" + ncf + ", type=" + type);

    return invoice;
  }

  @Transactional
  public Invoice createCreditNote(Long invoiceId, String ncfType) {
    Invoice original = invoiceRepository.findWithItems(invoiceId)
        .orElseThrow(() -> new IllegalArgumentException("La factura no existe."));

    if (original.getStatus() == InvoiceStatus.VOID) {
      throw new IllegalArgumentException("No se puede crear una nota de crédito de una factura anulada.");
    }
    if (original.getInvoiceType() == InvoiceType.CREDIT_NOTE) {
      throw new IllegalArgumentException("No se puede crear una nota de crédito desde otra nota de crédito.");
    }

    Invoice credit = new Invoice();
    credit.setPatient(original.getPatient());
    credit.setAppointment(original.getAppointment());
    credit.setStatus(InvoiceStatus.ISSUED);
    credit.setInvoiceType(InvoiceType.CREDIT_NOTE);
    credit.setCurrency(original.getCurrency());
    credit.setIssuedAt(LocalDateTime.now());
    credit.setCreatedBy(currentUsername());
    credit.setUpdatedBy(currentUsername());
    credit.setFiscalName(original.getFiscalName());
    credit.setFiscalTaxId(original.getFiscalTaxId());
    credit.setFiscalAddress(original.getFiscalAddress());
    credit.setCreditNoteOf(original);
    credit.setNotes("Nota de crédito de factura #" + original.getId());

    BigDecimal subtotal = nz(original.getSubtotal()).negate();
    BigDecimal tax = nz(original.getTax()).negate();
    BigDecimal discount = nz(original.getDiscount()).negate();
    BigDecimal total = nz(original.getTotal()).negate();
    credit.setSubtotal(scale(subtotal));
    credit.setTax(scale(tax));
    credit.setDiscount(scale(discount));
    credit.setTotal(scale(total));
    credit.setBalance(BigDecimal.ZERO);

    invoiceRepository.save(credit);

    for (InvoiceItem item : original.getItems()) {
      InvoiceItem ci = new InvoiceItem();
      ci.setInvoice(credit);
      ci.setService(item.getService());
      ci.setAppointment(item.getAppointment());
      ci.setDescription("NC: " + item.getDescription());
      ci.setQuantity(item.getQuantity());
      ci.setUnitPrice(scale(nz(item.getUnitPrice()).negate()));
      ci.setTotal(scale(nz(item.getTotal()).negate()));
      credit.getItems().add(ci);
    }
    invoiceRepository.save(credit);

    if (trimToNull(ncfType) != null) {
      assignNcf(credit.getId(), ncfType);
    }

    auditService.log("CREDIT_NOTE_CREATED",
        "Invoice",
        credit.getId(),
        "originalInvoiceId=" + original.getId());

    return credit;
  }

  public boolean isCashSessionRequired() {
    return requireCashSession;
  }

  public boolean isNcfRequiredBeforePayment() {
    return requireNcfBeforePayment;
  }

  @Transactional(readOnly = true)
  public boolean hasActiveCashSession() {
    return cashSessionService.findActive().isPresent();
  }

  @Transactional(readOnly = true)
  public long countOpenCharges() {
    return billingChargeRepository.countByStatus(BillingChargeStatus.OPEN);
  }

  private void refreshInvoiceFinancials(Invoice invoice) {
    if (invoice == null || invoice.getId() == null) return;
    if (invoice.getStatus() == InvoiceStatus.VOID) return;

    BigDecimal total = scale(nz(invoice.getTotal()));
    BigDecimal netCollected = scale(paymentRepository.sumNetByInvoiceAndStatusIn(
        invoice.getId(),
        List.of(PaymentStatus.CAPTURED, PaymentStatus.REFUNDED)
    ));
    if (netCollected.compareTo(BigDecimal.ZERO) < 0) {
      netCollected = BigDecimal.ZERO;
    }

    BigDecimal balance = scale(total.subtract(netCollected));
    if (balance.compareTo(BigDecimal.ZERO) < 0) {
      balance = BigDecimal.ZERO;
    }
    invoice.setBalance(balance);
    invoice.setUpdatedAt(LocalDateTime.now());
    invoice.setUpdatedBy(currentUsername());

    BigDecimal totalRefunded = scale(paymentRepository.sumRefundedByInvoice(invoice.getId()));
    if (balance.compareTo(BigDecimal.ZERO) == 0 && netCollected.compareTo(BigDecimal.ZERO) > 0) {
      invoice.setStatus(InvoiceStatus.PAID);
      invoice.setPaidAt(LocalDateTime.now());
    } else if (netCollected.compareTo(BigDecimal.ZERO) > 0) {
      invoice.setStatus(InvoiceStatus.PARTIALLY_PAID);
      invoice.setPaidAt(null);
    } else if (totalRefunded.compareTo(BigDecimal.ZERO) > 0) {
      invoice.setStatus(InvoiceStatus.REFUNDED);
      invoice.setPaidAt(null);
    } else {
      invoice.setStatus(InvoiceStatus.ISSUED);
      invoice.setPaidAt(null);
    }
    invoiceRepository.save(invoice);
    receivableCommitmentService.syncByInvoice(invoice);
  }

  private void validateNcfTypeForInvoice(Invoice invoice, String ncfType) {
    if (invoice == null || ncfType == null || ncfType.isBlank()) return;
    if (invoice.getInvoiceType() == InvoiceType.CREDIT_NOTE && !ncfType.startsWith("B04")) {
      throw new IllegalArgumentException("Las notas de crédito deben usar tipo NCF B04.");
    }
    if (invoice.getInvoiceType() == InvoiceType.INVOICE && ncfType.startsWith("B04")) {
      throw new IllegalArgumentException("El tipo NCF B04 solo aplica para notas de crédito.");
    }
  }

  private Appointment resolveInvoiceAppointment(List<BillingCharge> charges) {
    if (charges == null || charges.isEmpty()) return null;
    Appointment candidate = charges.get(0).getAppointment();
    if (candidate == null) return null;
    for (BillingCharge charge : charges) {
      Appointment current = charge.getAppointment();
      if (current == null || !Objects.equals(current.getId(), candidate.getId())) {
        return null;
      }
    }
    return candidate;
  }

  private BigDecimal resolveUnitPrice(BigDecimal requestUnitPrice, MedicalService service, Appointment appointment) {
    BigDecimal unitPrice = scaleOrNull(requestUnitPrice);
    if (unitPrice != null) return unitPrice;

    if (service != null && service.getPrice() != null) {
      return scale(service.getPrice());
    }
    if (appointment != null && appointment.getService() != null && appointment.getService().getPrice() != null) {
      return scale(appointment.getService().getPrice());
    }
    throw new IllegalArgumentException("Indica un precio unitario o selecciona un servicio con precio.");
  }

  private BigDecimal resolveTaxRate(BigDecimal requestedTaxRate) {
    BigDecimal rate = requestedTaxRate != null ? requestedTaxRate : taxRate;
    if (rate == null) return BigDecimal.ZERO;
    if (rate.compareTo(BigDecimal.ZERO) < 0) {
      throw new IllegalArgumentException("La tasa de impuesto no puede ser negativa.");
    }
    return rate;
  }

  private BillingChargeCategory inferCategory(Appointment appointment, MedicalService service) {
    if (service != null && service.getName() != null) {
      String n = service.getName().trim().toLowerCase();
      if (n.contains("lab")) return BillingChargeCategory.LABORATORY;
      if (n.contains("imagen") || n.contains("ray") || n.contains("sonograf")) return BillingChargeCategory.IMAGING;
      if (n.contains("consulta")) return BillingChargeCategory.CONSULTATION;
      if (n.contains("proced")) return BillingChargeCategory.PROCEDURE;
    }
    if (appointment != null) return BillingChargeCategory.CONSULTATION;
    return BillingChargeCategory.OTHER;
  }

  private String resolveChargeDescription(String requestDescription,
                                          MedicalService service,
                                          BillingChargeCategory category) {
    String description = trimToNull(requestDescription);
    if (description != null) return description;
    if (service != null) {
      String serviceName = trimToNull(service.getName());
      if (serviceName != null) return serviceName;
    }
    BillingChargeCategory safeCategory = category != null ? category : BillingChargeCategory.OTHER;
    return safeCategory.label();
  }

  private String resolveChargeSource(String source) {
    String value = trimToNull(source);
    if (value == null) return "MANUAL";
    return value.toUpperCase();
  }

  private String resolveCurrency(String currency) {
    String chosen = trimToNull(currency);
    if (chosen == null) {
      chosen = trimToNull(defaultCurrency);
    }
    if (chosen == null) {
      return "DOP";
    }
    return chosen.toUpperCase();
  }

  private String currentUsername() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth != null && auth.isAuthenticated()) {
      return auth.getName();
    }
    return null;
  }

  private BigDecimal nz(BigDecimal v) {
    return v == null ? BigDecimal.ZERO : v;
  }

  private BigDecimal scale(BigDecimal v) {
    if (v == null) return BigDecimal.ZERO;
    return v.setScale(2, RoundingMode.HALF_UP);
  }

  private BigDecimal scaleOrNull(BigDecimal v) {
    if (v == null) return null;
    return v.setScale(2, RoundingMode.HALF_UP);
  }

  private String trimToNull(String value) {
    if (value == null) return null;
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }

  private String sanitizePaymentField(String label, String value, int maxLength, boolean rejectPotentialPan) {
    String trimmed = trimToNull(value);
    if (trimmed == null) return null;
    if (trimmed.length() > maxLength) {
      throw new IllegalArgumentException(label + " excede longitud máxima (" + maxLength + ").");
    }
    if (SENSITIVE_CARD_KEYWORDS.matcher(trimmed).find()) {
      throw new IllegalArgumentException(label + " contiene términos sensibles de tarjeta y no puede almacenarse.");
    }
    if (rejectPotentialPan && containsPotentialPan(trimmed)) {
      throw new IllegalArgumentException(label + " parece contener un número de tarjeta completo. Guarda solo trazas seguras.");
    }
    return trimmed;
  }

  private String sanitizePaymentNotes(String notes) {
    return sanitizePaymentField("Notas", notes, 500, true);
  }

  private String normalizeLast4(String value) {
    String trimmed = trimToNull(value);
    if (trimmed == null) return null;
    String digits = trimmed.replaceAll("\\D", "");
    if (digits.isEmpty()) {
      return null;
    }
    if (digits.length() != 4) {
      throw new IllegalArgumentException("Last4 debe contener exactamente 4 dígitos.");
    }
    return digits;
  }

  private void appendRefundNotes(Payment payment, String rawNotes) {
    String notes = sanitizePaymentNotes(rawNotes);
    if (notes == null) return;
    String existing = trimToNull(payment.getNotes());
    String combined = existing == null ? "Reembolso: " + notes : existing + " | Reembolso: " + notes;
    if (combined.length() > 500) {
      combined = combined.substring(0, 500);
    }
    payment.setNotes(combined);
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

  private String resolveFiscalName(Patient patient) {
    if (patient == null) return null;
    String billing = trimToNull(patient.getBillingName());
    if (billing != null) return billing;
    return trimToNull(patient.getFullName());
  }

  public record PaymentRequest(BigDecimal amount,
                               BigDecimal cashReceived,
                               BigDecimal cashChange,
                               PaymentMethod method,
                               PaymentChannel channel,
                               String provider,
                               String reference,
                               String authCode,
                               String last4,
                               String cardBrand,
                               String terminalId,
                               String batchId,
                               String rrn,
                               String notes) {}

  public record RefundRequest(BigDecimal amount,
                              PaymentMethod method,
                              String reference,
                              String notes) {}

  public record ChargeRequest(Long patientId,
                              Long appointmentId,
                              Long serviceId,
                              BillingChargeCategory category,
                              String description,
                              Integer quantity,
                              BigDecimal unitPrice,
                              BigDecimal taxRate,
                              BigDecimal discount,
                              String currency,
                              String source,
                              LocalDateTime performedAt,
                              String notes) {}

  private PaymentChannel resolveChannel(PaymentChannel channel, PaymentMethod method) {
    if (channel != null) return channel;
    if (method == null) return PaymentChannel.IN_PERSON;
    return switch (method) {
      case TRANSFER -> PaymentChannel.BANK_TRANSFER;
      case INSURANCE -> PaymentChannel.INSURANCE;
      default -> PaymentChannel.IN_PERSON;
    };
  }
}
