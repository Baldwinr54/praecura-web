package com.baldwin.praecura.repository;

import com.baldwin.praecura.entity.BillingCharge;
import com.baldwin.praecura.entity.BillingChargeStatus;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BillingChargeRepository extends JpaRepository<BillingCharge, Long> {

  @Query("select c from BillingCharge c where " +
      "(:status is null or c.status = :status) and " +
      "(:patientId is null or c.patient.id = :patientId) and " +
      "(:appointmentId is null or c.appointment.id = :appointmentId) and " +
      "c.createdAt >= :fromDt and c.createdAt < :toDt")
  Page<BillingCharge> search(@Param("status") BillingChargeStatus status,
                             @Param("patientId") Long patientId,
                             @Param("appointmentId") Long appointmentId,
                             @Param("fromDt") LocalDateTime fromDt,
                             @Param("toDt") LocalDateTime toDt,
                             Pageable pageable);

  List<BillingCharge> findByAppointmentIdAndStatusOrderByCreatedAtAsc(Long appointmentId, BillingChargeStatus status);

  List<BillingCharge> findByInvoiceIdAndStatusOrderByCreatedAtAsc(Long invoiceId, BillingChargeStatus status);

  List<BillingCharge> findByIdIn(List<Long> ids);

  long countByStatus(BillingChargeStatus status);
}
