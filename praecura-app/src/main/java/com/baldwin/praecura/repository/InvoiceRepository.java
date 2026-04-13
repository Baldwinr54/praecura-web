package com.baldwin.praecura.repository;

import com.baldwin.praecura.entity.Invoice;
import com.baldwin.praecura.entity.InvoiceStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

  interface AccountsReceivableBucketAggregate {
    Long getBucketOrder();
    String getBucketLabel();
    Long getBucketCount();
    BigDecimal getBucketAmount();
  }

  Optional<Invoice> findByAppointmentId(Long appointmentId);

  Optional<Invoice> findFirstByAppointmentIdAndStatusNotOrderByCreatedAtDesc(Long appointmentId, InvoiceStatus status);

  @Query("select distinct i from Invoice i left join fetch i.items where i.id = :id")
  Optional<Invoice> findWithItems(@Param("id") Long id);

  @Query("select i from Invoice i where " +
      "(:status is null or i.status = :status) and " +
      "(:patientId is null or i.patient.id = :patientId) and " +
      "(:appointmentId is null or i.appointment.id = :appointmentId) and " +
      "i.createdAt >= :fromDt and i.createdAt < :toDt")
  @EntityGraph(attributePaths = {"patient", "appointment"})
  Page<Invoice> search(@Param("status") InvoiceStatus status,
                       @Param("patientId") Long patientId,
                       @Param("appointmentId") Long appointmentId,
                       @Param("fromDt") LocalDateTime fromDt,
                       @Param("toDt") LocalDateTime toDt,
                       Pageable pageable);

  long countByStatusNotAndBalanceGreaterThan(InvoiceStatus status, BigDecimal balance);

  @Query("select count(i) from Invoice i " +
      "where i.status <> :voidStatus " +
      "and i.invoiceType = com.baldwin.praecura.entity.InvoiceType.INVOICE " +
      "and (i.ncf is null or trim(i.ncf) = '')")
  long countPendingNcf(@Param("voidStatus") InvoiceStatus voidStatus);

  @Query("select count(i) from Invoice i " +
      "where i.status <> :voidStatus " +
      "and i.balance > 0 " +
      "and i.createdAt < :beforeDt")
  long countOverdueBalance(@Param("voidStatus") InvoiceStatus voidStatus,
                           @Param("beforeDt") LocalDateTime beforeDt);

  @Query("select i from Invoice i " +
      "where i.status <> :voidStatus " +
      "and i.invoiceType = com.baldwin.praecura.entity.InvoiceType.INVOICE " +
      "and (i.ncf is null or trim(i.ncf) = '') " +
      "order by i.createdAt asc")
  List<Invoice> findPendingNcf(@Param("voidStatus") InvoiceStatus voidStatus, Pageable pageable);

  @Query("select i from Invoice i " +
      "where i.status <> :voidStatus " +
      "and i.balance > 0 " +
      "and i.createdAt < :beforeDt " +
      "order by i.createdAt asc")
  List<Invoice> findOverdueBalances(@Param("voidStatus") InvoiceStatus voidStatus,
                                    @Param("beforeDt") LocalDateTime beforeDt,
                                    Pageable pageable);

  @Query("select count(i) from Invoice i " +
      "where i.status <> :voidStatus " +
      "and i.invoiceType = com.baldwin.praecura.entity.InvoiceType.INVOICE " +
      "and (i.ncf is null or trim(i.ncf) = '') " +
      "and i.status in (com.baldwin.praecura.entity.InvoiceStatus.PAID, com.baldwin.praecura.entity.InvoiceStatus.PARTIALLY_PAID)")
  long countCollectedWithoutNcf(@Param("voidStatus") InvoiceStatus voidStatus);

  @Query("select i from Invoice i " +
      "where i.status <> :voidStatus " +
      "and i.balance > 0 " +
      "and (:q is null " +
      "  or lower(i.patient.fullName) like lower(concat('%', :q, '%')) " +
      "  or str(i.id) = :q " +
      "  or str(i.patient.id) = :q " +
      "  or (i.appointment is not null and str(i.appointment.id) = :q))")
  @EntityGraph(attributePaths = {"patient", "appointment"})
  Page<Invoice> searchPendingForCash(@Param("q") String q,
                                     @Param("voidStatus") InvoiceStatus voidStatus,
                                     Pageable pageable);

  @Query("select i from Invoice i " +
      "where i.status <> :voidStatus " +
      "and i.balance > 0 " +
      "and (:q is null " +
      "  or lower(i.patient.fullName) like lower(concat('%', :q, '%')) " +
      "  or str(i.id) = :q " +
      "  or str(i.patient.id) = :q " +
      "  or (i.ncf is not null and lower(i.ncf) like lower(concat('%', :q, '%'))) " +
      "  or (i.fiscalTaxId is not null and lower(i.fiscalTaxId) like lower(concat('%', :q, '%'))))")
  @EntityGraph(attributePaths = {"patient", "appointment"})
  Page<Invoice> searchAccountsReceivable(@Param("q") String q,
                                         @Param("voidStatus") InvoiceStatus voidStatus,
                                         Pageable pageable);

  @Query(value = """
      select
        case
          when date_part('day', :asOf - i.created_at) <= 30 then 0
          when date_part('day', :asOf - i.created_at) <= 60 then 1
          when date_part('day', :asOf - i.created_at) <= 90 then 2
          else 3
        end as bucket_order,
        case
          when date_part('day', :asOf - i.created_at) <= 30 then '0-30 días'
          when date_part('day', :asOf - i.created_at) <= 60 then '31-60 días'
          when date_part('day', :asOf - i.created_at) <= 90 then '61-90 días'
          else '91+ días'
        end as bucket_label,
        count(*) as bucket_count,
        coalesce(sum(i.balance), 0) as bucket_amount
      from invoices i
      left join patients p on p.id = i.patient_id
      where i.status <> :voidStatus
        and i.balance > 0
        and (
          :q is null
          or lower(coalesce(p.full_name, '')) like lower(concat('%', :q, '%'))
          or cast(i.id as text) = :q
          or cast(coalesce(i.patient_id, 0) as text) = :q
          or (i.ncf is not null and lower(i.ncf) like lower(concat('%', :q, '%')))
          or (i.fiscal_tax_id is not null and lower(i.fiscal_tax_id) like lower(concat('%', :q, '%')))
        )
      group by bucket_order, bucket_label
      order by bucket_order
      """, nativeQuery = true)
  List<AccountsReceivableBucketAggregate> summarizeAccountsReceivable(@Param("q") String q,
                                                                      @Param("voidStatus") String voidStatus,
                                                                      @Param("asOf") LocalDateTime asOf);

  @Query("select i from Invoice i " +
      "where i.status <> :voidStatus " +
      "and i.balance > 0")
  List<Invoice> findAllOpenBalances(@Param("voidStatus") InvoiceStatus voidStatus);
}
