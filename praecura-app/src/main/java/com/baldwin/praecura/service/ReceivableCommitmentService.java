package com.baldwin.praecura.service;

import com.baldwin.praecura.entity.Invoice;
import com.baldwin.praecura.entity.InvoiceStatus;
import com.baldwin.praecura.entity.ReceivableCommitment;
import com.baldwin.praecura.entity.ReceivableCommitmentStatus;
import com.baldwin.praecura.repository.InvoiceRepository;
import com.baldwin.praecura.repository.ReceivableCommitmentRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReceivableCommitmentService {

  private final ReceivableCommitmentRepository receivableCommitmentRepository;
  private final InvoiceRepository invoiceRepository;
  private final AuditService auditService;

  public ReceivableCommitmentService(ReceivableCommitmentRepository receivableCommitmentRepository,
                                     InvoiceRepository invoiceRepository,
                                     AuditService auditService) {
    this.receivableCommitmentRepository = receivableCommitmentRepository;
    this.invoiceRepository = invoiceRepository;
    this.auditService = auditService;
  }

  @Transactional(readOnly = true)
  public List<ReceivableCommitment> listByInvoice(Long invoiceId) {
    if (invoiceId == null) return List.of();
    return receivableCommitmentRepository.findByInvoiceIdOrderByPromisedDateAscCreatedAtAsc(invoiceId);
  }

  @Transactional
  public ReceivableCommitment create(Long invoiceId,
                                     BigDecimal promisedAmount,
                                     LocalDateTime promisedDate,
                                     String notes) {
    Invoice invoice = invoiceRepository.findById(invoiceId)
        .orElseThrow(() -> new IllegalArgumentException("La factura no existe."));

    if (invoice.getStatus() == InvoiceStatus.VOID) {
      throw new IllegalArgumentException("No puedes crear compromisos para una factura anulada.");
    }
    if (nz(invoice.getBalance()).compareTo(BigDecimal.ZERO) <= 0) {
      throw new IllegalArgumentException("La factura no tiene balance pendiente.");
    }

    BigDecimal amount = scale(promisedAmount);
    if (amount.compareTo(BigDecimal.ZERO) <= 0) {
      throw new IllegalArgumentException("El monto del compromiso debe ser mayor a 0.");
    }
    if (amount.compareTo(nz(invoice.getBalance())) > 0) {
      throw new IllegalArgumentException("El monto del compromiso no puede exceder el balance pendiente.");
    }
    if (promisedDate == null) {
      throw new IllegalArgumentException("Debes indicar la fecha/hora de compromiso.");
    }

    ReceivableCommitment commitment = new ReceivableCommitment();
    commitment.setInvoice(invoice);
    commitment.setPromisedAmount(amount);
    commitment.setPromisedDate(promisedDate);
    commitment.setNotes(trimToNull(notes));
    commitment.setStatus(ReceivableCommitmentStatus.PENDING);
    commitment.setCreatedAt(LocalDateTime.now());
    commitment.setUpdatedAt(LocalDateTime.now());
    commitment.setCreatedBy(currentUsername());
    commitment.setUpdatedBy(currentUsername());

    receivableCommitmentRepository.save(commitment);

    auditService.log(
        "AR_COMMITMENT_CREATED",
        "ReceivableCommitment",
        commitment.getId(),
        "invoiceId=" + invoice.getId() + ", amount=" + amount + ", promisedDate=" + promisedDate
    );

    return commitment;
  }

  @Transactional
  public ReceivableCommitment markFulfilled(Long commitmentId) {
    ReceivableCommitment commitment = get(commitmentId);
    ensurePending(commitment);
    commitment.setStatus(ReceivableCommitmentStatus.FULFILLED);
    commitment.setFulfilledAt(LocalDateTime.now());
    touch(commitment);
    receivableCommitmentRepository.save(commitment);

    auditService.log(
        "AR_COMMITMENT_FULFILLED",
        "ReceivableCommitment",
        commitment.getId(),
        "invoiceId=" + commitment.getInvoice().getId()
    );

    return commitment;
  }

  @Transactional
  public ReceivableCommitment markBroken(Long commitmentId) {
    ReceivableCommitment commitment = get(commitmentId);
    ensurePending(commitment);
    commitment.setStatus(ReceivableCommitmentStatus.BROKEN);
    commitment.setBrokenAt(LocalDateTime.now());
    touch(commitment);
    receivableCommitmentRepository.save(commitment);

    auditService.log(
        "AR_COMMITMENT_BROKEN",
        "ReceivableCommitment",
        commitment.getId(),
        "invoiceId=" + commitment.getInvoice().getId()
    );

    return commitment;
  }

  @Transactional
  public ReceivableCommitment cancel(Long commitmentId) {
    ReceivableCommitment commitment = get(commitmentId);
    if (commitment.getStatus() == ReceivableCommitmentStatus.CANCELED) {
      return commitment;
    }
    if (commitment.getStatus() == ReceivableCommitmentStatus.FULFILLED) {
      throw new IllegalArgumentException("No puedes cancelar un compromiso ya cumplido.");
    }

    commitment.setStatus(ReceivableCommitmentStatus.CANCELED);
    commitment.setCanceledAt(LocalDateTime.now());
    touch(commitment);
    receivableCommitmentRepository.save(commitment);

    auditService.log(
        "AR_COMMITMENT_CANCELED",
        "ReceivableCommitment",
        commitment.getId(),
        "invoiceId=" + commitment.getInvoice().getId()
    );

    return commitment;
  }

  @Transactional
  public int syncByInvoice(Invoice invoice) {
    if (invoice == null || invoice.getId() == null) return 0;
    if (invoice.getStatus() == InvoiceStatus.VOID) {
      return cancelPendingForInvoice(invoice.getId(), "Factura anulada");
    }
    if (nz(invoice.getBalance()).compareTo(BigDecimal.ZERO) > 0) return 0;

    List<ReceivableCommitment> pending = receivableCommitmentRepository
        .findByInvoiceIdAndStatusOrderByPromisedDateAscCreatedAtAsc(invoice.getId(), ReceivableCommitmentStatus.PENDING);

    int changed = 0;
    LocalDateTime now = LocalDateTime.now();
    for (ReceivableCommitment commitment : pending) {
      commitment.setStatus(ReceivableCommitmentStatus.FULFILLED);
      commitment.setFulfilledAt(now);
      touch(commitment);
      receivableCommitmentRepository.save(commitment);
      changed++;
    }
    if (changed > 0) {
      auditService.log(
          "AR_COMMITMENT_SYNC_FULFILLED",
          "Invoice",
          invoice.getId(),
          "autoFulfilledCommitments=" + changed
      );
    }
    return changed;
  }

  @Transactional
  public int cancelPendingForInvoice(Long invoiceId, String reason) {
    if (invoiceId == null) return 0;
    List<ReceivableCommitment> pending = receivableCommitmentRepository
        .findByInvoiceIdAndStatusOrderByPromisedDateAscCreatedAtAsc(invoiceId, ReceivableCommitmentStatus.PENDING);

    int changed = 0;
    LocalDateTime now = LocalDateTime.now();
    for (ReceivableCommitment commitment : pending) {
      commitment.setStatus(ReceivableCommitmentStatus.CANCELED);
      commitment.setCanceledAt(now);
      if (trimToNull(reason) != null) {
        commitment.setNotes(trimToNull(reason));
      }
      touch(commitment);
      receivableCommitmentRepository.save(commitment);
      changed++;
    }
    if (changed > 0) {
      auditService.log(
          "AR_COMMITMENT_SYNC_CANCELED",
          "Invoice",
          invoiceId,
          "autoCanceledCommitments=" + changed
      );
    }
    return changed;
  }

  @Transactional(readOnly = true)
  public long countOverduePending() {
    return receivableCommitmentRepository.countByStatusAndPromisedDateBefore(
        ReceivableCommitmentStatus.PENDING,
        LocalDateTime.now()
    );
  }

  @Transactional(readOnly = true)
  public List<ReceivableCommitment> listPendingInWindow(LocalDateTime fromDt,
                                                        LocalDateTime toDt,
                                                        int limit) {
    LocalDateTime from = fromDt != null ? fromDt : LocalDateTime.now().minusDays(7);
    LocalDateTime to = toDt != null ? toDt : LocalDateTime.now().plusDays(14);
    int pageSize = Math.max(1, Math.min(limit, 200));
    return receivableCommitmentRepository.findPendingInWindow(
        ReceivableCommitmentStatus.PENDING,
        from,
        to,
        PageRequest.of(0, pageSize)
    );
  }

  private ReceivableCommitment get(Long commitmentId) {
    return receivableCommitmentRepository.findById(commitmentId)
        .orElseThrow(() -> new IllegalArgumentException("El compromiso no existe."));
  }

  private void ensurePending(ReceivableCommitment commitment) {
    if (commitment.getStatus() != ReceivableCommitmentStatus.PENDING) {
      throw new IllegalArgumentException("Solo puedes modificar compromisos en estado pendiente.");
    }
  }

  private void touch(ReceivableCommitment commitment) {
    commitment.setUpdatedAt(LocalDateTime.now());
    commitment.setUpdatedBy(currentUsername());
  }

  private String currentUsername() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth != null && auth.isAuthenticated()) {
      return auth.getName();
    }
    return null;
  }

  private BigDecimal nz(BigDecimal value) {
    return value != null ? value : BigDecimal.ZERO;
  }

  private BigDecimal scale(BigDecimal value) {
    if (value == null) return BigDecimal.ZERO;
    return value.setScale(2, RoundingMode.HALF_UP);
  }

  private String trimToNull(String value) {
    if (value == null) return null;
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }
}
