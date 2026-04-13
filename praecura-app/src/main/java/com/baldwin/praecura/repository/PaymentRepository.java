package com.baldwin.praecura.repository;

import com.baldwin.praecura.entity.Payment;
import com.baldwin.praecura.entity.PaymentMethod;
import com.baldwin.praecura.entity.PaymentStatus;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

  List<Payment> findByInvoiceIdOrderByCreatedAtDesc(Long invoiceId);

  @Query("select coalesce(sum(p.amount), 0) from Payment p where p.invoice.id = :invoiceId and p.status = :status")
  BigDecimal sumByInvoiceAndStatus(@Param("invoiceId") Long invoiceId,
                                   @Param("status") PaymentStatus status);

  @Query("select coalesce(sum(p.amount), 0) from Payment p where p.status = :status and p.createdAt >= :fromDt and p.createdAt < :toDt")
  BigDecimal sumByStatusBetween(@Param("status") PaymentStatus status,
                                @Param("fromDt") java.time.LocalDateTime fromDt,
                                @Param("toDt") java.time.LocalDateTime toDt);

  @Query("select p from Payment p join fetch p.invoice i where i.id in :invoiceIds order by p.createdAt desc")
  List<Payment> findByInvoiceIds(@Param("invoiceIds") List<Long> invoiceIds);

  @Query("select coalesce(sum(p.amount), 0) from Payment p where p.cashSession.id = :sessionId and p.method = :method and p.status = :status")
  BigDecimal sumByCashSession(@Param("sessionId") Long sessionId,
                              @Param("method") PaymentMethod method,
                              @Param("status") PaymentStatus status);

  @Query("select coalesce(sum(p.refundedAmount), 0) from Payment p where p.refundSession.id = :sessionId and p.refundMethod = :method")
  BigDecimal sumRefundedBySession(@Param("sessionId") Long sessionId,
                                  @Param("method") PaymentMethod method);

  @Query("select coalesce(sum(p.amount - coalesce(p.refundedAmount, 0)), 0) from Payment p " +
      "where p.invoice.id = :invoiceId and p.status in :statuses")
  BigDecimal sumNetByInvoiceAndStatusIn(@Param("invoiceId") Long invoiceId,
                                        @Param("statuses") List<PaymentStatus> statuses);

  @Query("select coalesce(sum(p.refundedAmount), 0) from Payment p where p.invoice.id = :invoiceId")
  BigDecimal sumRefundedByInvoice(@Param("invoiceId") Long invoiceId);

  List<Payment> findByCashSessionIdOrderByCreatedAtDesc(Long cashSessionId);

  List<Payment> findByRefundSessionIdOrderByCreatedAtDesc(Long refundSessionId);

  long countByInvoiceId(Long invoiceId);

  boolean existsByExternalId(String externalId);

  List<Payment> findByCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtAsc(LocalDateTime from, LocalDateTime to);

  List<Payment> findByRefundedAtGreaterThanEqualAndRefundedAtLessThanOrderByRefundedAtAsc(LocalDateTime from, LocalDateTime to);
}
