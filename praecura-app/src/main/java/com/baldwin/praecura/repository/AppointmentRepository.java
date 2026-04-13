package com.baldwin.praecura.repository;

import com.baldwin.praecura.entity.Appointment;
import com.baldwin.praecura.entity.AppointmentStatus;
import com.baldwin.praecura.entity.InvoiceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.List;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

  interface DoctorLoadRow {
    Long getDoctorId();
    String getDoctorName();
    Long getTotal();
  }

  interface DoctorCountRow {
    Long getId();
    String getName();
    Long getTotal();
  }

  interface ServiceCountRow {
    Long getId();
    String getName();
    Long getTotal();
  }

  interface SpecialtyCountRow {
    Long getId();
    String getName();
    Long getTotal();
  }

  interface HourCountRow {
    Integer getHour();
    Long getTotal();
  }

  interface DayCountRow {
    Integer getDow();
    Long getTotal();
  }

  interface DoctorProductivityRow {
    Long getDoctorId();
    String getDoctorName();
    Long getTotal();
    Long getCompleted();
    Long getCancelled();
    Long getNoShow();
    Double getAvgDuration();
  }

  interface ServiceProductivityRow {
    Long getServiceId();
    String getServiceName();
    Long getTotal();
    Long getCompleted();
    Long getCancelled();
    Long getNoShow();
    Double getAvgDuration();
  }

  interface RevenueByServiceRow {
    Long getServiceId();
    String getServiceName();
    BigDecimal getRevenue();
    Long getCompleted();
  }

  interface RevenueByDoctorRow {
    Long getDoctorId();
    String getDoctorName();
    BigDecimal getRevenue();
    Long getCompleted();
  }

  interface RevenueBySpecialtyRow {
    Long getSpecialtyId();
    String getSpecialtyName();
    BigDecimal getRevenue();
    Long getCompleted();
  }

  interface PatientCountRow {
    Long getId();
    Long getTotal();
  }

  interface CashDeskQueueRow {
    Long getAppointmentId();
    LocalDateTime getScheduledAt();
    Long getPatientId();
    String getPatientName();
    String getDoctorName();
    String getServiceName();
    BigDecimal getServicePrice();
    Long getInvoiceId();
    BigDecimal getInvoiceBalance();
    InvoiceStatus getInvoiceStatus();
  }

  long countByScheduledAtBetweenAndActiveTrue(LocalDateTime fromDt, LocalDateTime toDt);

  long countByScheduledAtBetweenAndStatusAndActiveTrue(LocalDateTime fromDt, LocalDateTime toDt, AppointmentStatus status);

  long countByScheduledAtBetweenAndStatusInAndActiveTrue(LocalDateTime fromDt, LocalDateTime toDt, List<AppointmentStatus> statuses);

  List<Appointment> findTop5ByScheduledAtBetweenAndStatusNotInAndActiveTrueOrderByScheduledAtAsc(
      LocalDateTime fromDt, LocalDateTime toDt, List<AppointmentStatus> excluded);

  List<Appointment> findTop10ByScheduledAtBetweenAndStatusNotInAndActiveTrueOrderByScheduledAtAsc(
      LocalDateTime fromDt, LocalDateTime toDt, List<AppointmentStatus> excluded);

  @EntityGraph(attributePaths = {"patient", "doctor", "site", "resource", "service"})
  Page<Appointment> findByScheduledAtBetweenAndActiveTrueOrderByScheduledAtAsc(
      LocalDateTime fromDt, LocalDateTime toDt, Pageable pageable);

  @Query("select a.id as appointmentId, a.scheduledAt as scheduledAt, " +
      "p.id as patientId, p.fullName as patientName, d.fullName as doctorName, " +
      "s.name as serviceName, s.price as servicePrice, " +
      "i.id as invoiceId, i.balance as invoiceBalance, i.status as invoiceStatus " +
      "from Appointment a " +
      "join a.patient p " +
      "join a.doctor d " +
      "left join a.service s " +
      "left join Invoice i on i.appointment = a and i.status <> :voidStatus " +
      "where a.active = true " +
      "and a.scheduledAt >= :fromDt and a.scheduledAt < :toDt " +
      "order by a.scheduledAt asc, a.id asc")
  Page<CashDeskQueueRow> findCashDeskRows(@Param("fromDt") LocalDateTime fromDt,
                                          @Param("toDt") LocalDateTime toDt,
                                          @Param("voidStatus") InvoiceStatus voidStatus,
                                          Pageable pageable);

  @Query("select d.id as doctorId, d.fullName as doctorName, count(a) as total " +
      "from Appointment a join a.doctor d " +
      "where a.active = true " +
      "and a.scheduledAt >= :fromDt and a.scheduledAt < :toDt " +
      "and a.status not in :excluded " +
      "group by d.id, d.fullName " +
      "order by count(a) desc, d.fullName asc")
  List<DoctorLoadRow> loadByDoctor(@Param("fromDt") LocalDateTime fromDt,
                                  @Param("toDt") LocalDateTime toDt,
                                  @Param("excluded") List<AppointmentStatus> excluded);

  @Query("select d.id as id, d.fullName as name, count(a) as total " +
      "from Appointment a join a.doctor d " +
      "where a.active = true " +
      "and a.scheduledAt >= :fromDt and a.scheduledAt < :toDt " +
      "group by d.id, d.fullName " +
      "order by count(a) desc, d.fullName asc")
  List<DoctorCountRow> countByDoctor(@Param("fromDt") LocalDateTime fromDt,
                                     @Param("toDt") LocalDateTime toDt);

  @Query("select s.id as id, s.name as name, count(a) as total " +
      "from Appointment a join a.service s " +
      "where a.active = true " +
      "and a.scheduledAt >= :fromDt and a.scheduledAt < :toDt " +
      "group by s.id, s.name " +
      "order by count(a) desc, s.name asc")
  List<ServiceCountRow> countByService(@Param("fromDt") LocalDateTime fromDt,
                                       @Param("toDt") LocalDateTime toDt);

  @Query("select sp.id as id, sp.name as name, count(a) as total " +
      "from Appointment a join a.doctor d join d.specialty sp " +
      "where a.active = true " +
      "and a.scheduledAt >= :fromDt and a.scheduledAt < :toDt " +
      "group by sp.id, sp.name " +
      "order by count(a) desc, sp.name asc")
  List<SpecialtyCountRow> countBySpecialty(@Param("fromDt") LocalDateTime fromDt,
                                           @Param("toDt") LocalDateTime toDt);

  @Query(value = "select extract(hour from scheduled_at) as hour, count(*) as total " +
      "from appointments " +
      "where active = true " +
      "and scheduled_at >= :fromDt and scheduled_at < :toDt " +
      "group by hour " +
      "order by hour asc", nativeQuery = true)
  List<HourCountRow> countByHour(@Param("fromDt") LocalDateTime fromDt,
                                 @Param("toDt") LocalDateTime toDt);

  @Query(value = "select extract(dow from scheduled_at) as dow, count(*) as total " +
      "from appointments " +
      "where active = true " +
      "and scheduled_at >= :fromDt and scheduled_at < :toDt " +
      "group by dow " +
      "order by dow asc", nativeQuery = true)
  List<DayCountRow> countByDayOfWeek(@Param("fromDt") LocalDateTime fromDt,
                                     @Param("toDt") LocalDateTime toDt);

  @Query("select d.id as doctorId, d.fullName as doctorName, count(a) as total, " +
      "sum(case when a.status = com.baldwin.praecura.entity.AppointmentStatus.COMPLETADA then 1 else 0 end) as completed, " +
      "sum(case when a.status = com.baldwin.praecura.entity.AppointmentStatus.CANCELADA then 1 else 0 end) as cancelled, " +
      "sum(case when a.status = com.baldwin.praecura.entity.AppointmentStatus.NO_ASISTIO then 1 else 0 end) as noShow, " +
      "avg(a.durationMinutes) as avgDuration " +
      "from Appointment a join a.doctor d " +
      "where a.active = true " +
      "and a.scheduledAt >= :fromDt and a.scheduledAt < :toDt " +
      "group by d.id, d.fullName " +
      "order by count(a) desc, d.fullName asc")
  List<DoctorProductivityRow> productivityByDoctor(@Param("fromDt") LocalDateTime fromDt,
                                                   @Param("toDt") LocalDateTime toDt);

  @Query("select d.id as doctorId, d.fullName as doctorName, count(a) as total, " +
      "sum(case when a.status = com.baldwin.praecura.entity.AppointmentStatus.COMPLETADA then 1 else 0 end) as completed, " +
      "sum(case when a.status = com.baldwin.praecura.entity.AppointmentStatus.CANCELADA then 1 else 0 end) as cancelled, " +
      "sum(case when a.status = com.baldwin.praecura.entity.AppointmentStatus.NO_ASISTIO then 1 else 0 end) as noShow, " +
      "avg(a.durationMinutes) as avgDuration " +
      "from Appointment a join a.doctor d " +
      "where a.active = true " +
      "and d.id in :ids " +
      "and a.scheduledAt >= :fromDt and a.scheduledAt < :toDt " +
      "group by d.id, d.fullName " +
      "order by count(a) desc, d.fullName asc")
  List<DoctorProductivityRow> productivityByDoctorIds(@Param("ids") List<Long> ids,
                                                      @Param("fromDt") LocalDateTime fromDt,
                                                      @Param("toDt") LocalDateTime toDt);

  @Query("select s.id as serviceId, s.name as serviceName, count(a) as total, " +
      "sum(case when a.status = com.baldwin.praecura.entity.AppointmentStatus.COMPLETADA then 1 else 0 end) as completed, " +
      "sum(case when a.status = com.baldwin.praecura.entity.AppointmentStatus.CANCELADA then 1 else 0 end) as cancelled, " +
      "sum(case when a.status = com.baldwin.praecura.entity.AppointmentStatus.NO_ASISTIO then 1 else 0 end) as noShow, " +
      "avg(a.durationMinutes) as avgDuration " +
      "from Appointment a join a.service s " +
      "where a.active = true " +
      "and a.scheduledAt >= :fromDt and a.scheduledAt < :toDt " +
      "group by s.id, s.name " +
      "order by count(a) desc, s.name asc")
  List<ServiceProductivityRow> productivityByService(@Param("fromDt") LocalDateTime fromDt,
                                                     @Param("toDt") LocalDateTime toDt);

  @Query("select s.id as serviceId, s.name as serviceName, sum(s.price) as revenue, count(a) as completed " +
      "from Appointment a join a.service s " +
      "where a.active = true " +
      "and a.status = com.baldwin.praecura.entity.AppointmentStatus.COMPLETADA " +
      "and s.price is not null " +
      "and a.scheduledAt >= :fromDt and a.scheduledAt < :toDt " +
      "group by s.id, s.name " +
      "order by sum(s.price) desc, s.name asc")
  List<RevenueByServiceRow> revenueByService(@Param("fromDt") LocalDateTime fromDt,
                                             @Param("toDt") LocalDateTime toDt);

  @Query("select d.id as doctorId, d.fullName as doctorName, sum(s.price) as revenue, count(a) as completed " +
      "from Appointment a join a.doctor d join a.service s " +
      "where a.active = true " +
      "and a.status = com.baldwin.praecura.entity.AppointmentStatus.COMPLETADA " +
      "and s.price is not null " +
      "and a.scheduledAt >= :fromDt and a.scheduledAt < :toDt " +
      "group by d.id, d.fullName " +
      "order by sum(s.price) desc, d.fullName asc")
  List<RevenueByDoctorRow> revenueByDoctor(@Param("fromDt") LocalDateTime fromDt,
                                           @Param("toDt") LocalDateTime toDt);

  @Query("select sp.id as specialtyId, sp.name as specialtyName, sum(s.price) as revenue, count(a) as completed " +
      "from Appointment a join a.doctor d join d.specialty sp join a.service s " +
      "where a.active = true " +
      "and a.status = com.baldwin.praecura.entity.AppointmentStatus.COMPLETADA " +
      "and s.price is not null " +
      "and a.scheduledAt >= :fromDt and a.scheduledAt < :toDt " +
      "group by sp.id, sp.name " +
      "order by sum(s.price) desc, sp.name asc")
  List<RevenueBySpecialtyRow> revenueBySpecialty(@Param("fromDt") LocalDateTime fromDt,
                                                 @Param("toDt") LocalDateTime toDt);

  @Query("select sum(s.price) from Appointment a join a.service s " +
      "where a.active = true " +
      "and a.status = com.baldwin.praecura.entity.AppointmentStatus.COMPLETADA " +
      "and s.price is not null " +
      "and a.scheduledAt >= :fromDt and a.scheduledAt < :toDt")
  BigDecimal totalRevenue(@Param("fromDt") LocalDateTime fromDt,
                          @Param("toDt") LocalDateTime toDt);

  @Query("select count(a) from Appointment a join a.service s " +
      "where a.active = true " +
      "and a.status = com.baldwin.praecura.entity.AppointmentStatus.COMPLETADA " +
      "and s.price is not null " +
      "and a.scheduledAt >= :fromDt and a.scheduledAt < :toDt")
  long countCompletedWithService(@Param("fromDt") LocalDateTime fromDt,
                                 @Param("toDt") LocalDateTime toDt);

  @Query("select a.patient.id as id, count(a) as total " +
      "from Appointment a " +
      "where a.active = true and a.patient.id in :patientIds " +
      "and a.scheduledAt >= :fromDt and a.scheduledAt < :toDt " +
      "group by a.patient.id")
  List<PatientCountRow> countByPatients(@Param("patientIds") List<Long> patientIds,
                                        @Param("fromDt") LocalDateTime fromDt,
                                        @Param("toDt") LocalDateTime toDt);

  @Query("select a.patient.id as id, count(a) as total " +
      "from Appointment a " +
      "where a.active = true and a.patient.id in :patientIds " +
      "and a.status = :status " +
      "and a.scheduledAt >= :fromDt and a.scheduledAt < :toDt " +
      "group by a.patient.id")
  List<PatientCountRow> countByPatientsAndStatus(@Param("patientIds") List<Long> patientIds,
                                                 @Param("status") AppointmentStatus status,
                                                 @Param("fromDt") LocalDateTime fromDt,
                                                 @Param("toDt") LocalDateTime toDt);

  @Query("select a.status as status, count(a) as total " +
      "from Appointment a " +
      "where a.active = true " +
      "and a.scheduledAt >= :fromDt and a.scheduledAt < :toDt " +
      "group by a.status " +
      "order by a.status asc")
  List<Object[]> countByStatus(@Param("fromDt") LocalDateTime fromDt,
                               @Param("toDt") LocalDateTime toDt);

  @Query("select count(a) from Appointment a " +
      "where a.active = true and a.doctor.id = :doctorId " +
      "and a.scheduledAt >= :fromDt and a.scheduledAt < :toDt")
  long countByDoctorAndRange(@Param("doctorId") Long doctorId,
                             @Param("fromDt") LocalDateTime fromDt,
                             @Param("toDt") LocalDateTime toDt);

  @Query("select count(a) from Appointment a " +
      "where a.active = true and a.doctor.id = :doctorId " +
      "and a.status = :status " +
      "and a.scheduledAt >= :fromDt and a.scheduledAt < :toDt")
  long countByDoctorAndStatus(@Param("doctorId") Long doctorId,
                              @Param("status") AppointmentStatus status,
                              @Param("fromDt") LocalDateTime fromDt,
                              @Param("toDt") LocalDateTime toDt);

  @Query("select count(a) from Appointment a " +
      "where a.active = true " +
      "and a.scheduledAt >= :fromDt and a.scheduledAt < :toDt " +
      "and a.status not in :excluded " +
      "and a.service is null")
  long countMissingService(@Param("fromDt") LocalDateTime fromDt,
                           @Param("toDt") LocalDateTime toDt,
                           @Param("excluded") List<AppointmentStatus> excluded);

  @Query("select count(distinct p.id) from Appointment a join a.patient p " +
      "where a.active = true " +
      "and a.scheduledAt >= :fromDt and a.scheduledAt < :toDt " +
      "and a.status not in :excluded " +
      "and (p.phone is null or trim(p.phone) = '')")
  long countDistinctPatientsMissingPhone(@Param("fromDt") LocalDateTime fromDt,
                                        @Param("toDt") LocalDateTime toDt,
                                        @Param("excluded") List<AppointmentStatus> excluded);

  @Query("select count(a) from Appointment a join a.doctor d " +
      "where a.active = true " +
      "and a.scheduledAt >= :fromDt and a.scheduledAt < :toDt " +
      "and a.status not in :excluded " +
      "and d.active = false")
  long countWithInactiveDoctor(@Param("fromDt") LocalDateTime fromDt,
                               @Param("toDt") LocalDateTime toDt,
                               @Param("excluded") List<AppointmentStatus> excluded);

  @Query("select count(a) from Appointment a " +
      "where a.active = true " +
      "and a.scheduledAt >= :fromDt and a.scheduledAt < :toDt " +
      "and a.status in :statuses " +
      "and a.remindedAt is null")
  long countReminderPending(@Param("fromDt") LocalDateTime fromDt,
                            @Param("toDt") LocalDateTime toDt,
                            @Param("statuses") List<AppointmentStatus> statuses);

  @Query("select a from Appointment a " +
      "join a.patient p " +
      "join a.doctor d " +
      "where (:q is null or :q = '' or " +
      "  lower(p.fullName) like lower(concat('%', :q, '%')) or " +
      "  lower(coalesce(p.cedula,'')) like lower(concat('%', :q, '%')) or " +
      "  lower(coalesce(p.phone,'')) like lower(concat('%', :q, '%')) or " +
      "  lower(coalesce(a.reason,'')) like lower(concat('%', :q, '%'))" +
      ") " +
      "and (:doctorId is null or d.id = :doctorId) " +
      "and (:siteId is null or a.site.id = :siteId) " +
      "and (:status is null or a.status = :status) " +
      "and a.active = true " +
      "and a.scheduledAt >= :fromDt " +
      "and a.scheduledAt < :toDt " +
      "order by a.scheduledAt desc, a.id desc")
  @EntityGraph(attributePaths = {"patient", "doctor", "site", "resource", "service"})
  Page<Appointment> search(@Param("q") String q,
                           @Param("doctorId") Long doctorId,
                           @Param("siteId") Long siteId,
                           @Param("status") AppointmentStatus status,
                           @Param("fromDt") LocalDateTime fromDt,
                           @Param("toDt") LocalDateTime toDt,
                           Pageable pageable);

  @Query("select a from Appointment a where a.patient.id = :patientId and a.active = true order by a.scheduledAt desc")
  List<Appointment> historyForPatient(@Param("patientId") Long patientId);

  @Query("select a from Appointment a where a.patient.id = :patientId and a.active = true order by a.scheduledAt desc")
  Page<Appointment> historyForPatient(@Param("patientId") Long patientId, Pageable pageable);

  @Query("select count(a) from Appointment a where a.active = true and a.patient.id = :patientId " +
      "and a.scheduledAt >= :fromDt and a.scheduledAt < :toDt")
  long countByPatientAndRange(@Param("patientId") Long patientId,
                              @Param("fromDt") LocalDateTime fromDt,
                              @Param("toDt") LocalDateTime toDt);

  @Query("select count(a) from Appointment a where a.active = true and a.patient.id = :patientId " +
      "and a.status = :status and a.scheduledAt >= :fromDt and a.scheduledAt < :toDt")
  long countByPatientAndStatus(@Param("patientId") Long patientId,
                               @Param("status") AppointmentStatus status,
                               @Param("fromDt") LocalDateTime fromDt,
                               @Param("toDt") LocalDateTime toDt);

  @Query("select a from Appointment a where a.active = true and a.doctor.id = :doctorId " +
      "and a.scheduledAt >= :fromDt and a.scheduledAt < :toDt " +
      "and a.status not in :excluded " +
      "order by a.scheduledAt asc")
  List<Appointment> findDoctorDayNonCancelled(@Param("doctorId") Long doctorId,
                                              @Param("fromDt") LocalDateTime fromDt,
                                              @Param("toDt") LocalDateTime toDt,
                                              @Param("excluded") List<AppointmentStatus> excluded);

  @Query("select a from Appointment a where a.active = true and a.resource.id = :resourceId " +
      "and a.scheduledAt >= :fromDt and a.scheduledAt < :toDt " +
      "and a.status not in :excluded " +
      "order by a.scheduledAt asc")
  List<Appointment> findResourceDayNonCancelled(@Param("resourceId") Long resourceId,
                                                @Param("fromDt") LocalDateTime fromDt,
                                                @Param("toDt") LocalDateTime toDt,
                                                @Param("excluded") List<AppointmentStatus> excluded);
}
