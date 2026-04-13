package com.baldwin.praecura.service;

import com.baldwin.praecura.dto.DoctorForm;
import com.baldwin.praecura.entity.Doctor;
import com.baldwin.praecura.entity.MedicalService;
import com.baldwin.praecura.entity.Specialty;
import com.baldwin.praecura.repository.AppointmentRepository;
import com.baldwin.praecura.repository.DoctorRepository;
import com.baldwin.praecura.repository.MedicalServiceRepository;
import com.baldwin.praecura.repository.SpecialtyRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.time.LocalTime;

@Service
public class DoctorService {

  private final DoctorRepository doctorRepository;
  private final SpecialtyRepository specialtyRepository;
  private final MedicalServiceRepository medicalServiceRepository;
  private final AuditService auditService;
  private final AppointmentRepository appointmentRepository;

  public DoctorService(DoctorRepository doctorRepository,
                       SpecialtyRepository specialtyRepository,
                       MedicalServiceRepository medicalServiceRepository,
                       AuditService auditService,
                       AppointmentRepository appointmentRepository) {
    this.doctorRepository = doctorRepository;
    this.specialtyRepository = specialtyRepository;
    this.medicalServiceRepository = medicalServiceRepository;
    this.auditService = auditService;
    this.appointmentRepository = appointmentRepository;
  }

  public Page<Doctor> search(String q, Pageable pageable) {
    if (q == null || q.trim().isEmpty()) {
      return doctorRepository.findActive(pageable);
    }
    return doctorRepository.searchActive(q.trim(), pageable);
  }

  public Doctor findById(Long id) {
    return doctorRepository.findById(id).orElseThrow();
  }

  @Transactional(readOnly = true)
  public Doctor findByIdWithRelations(Long id) {
    Doctor doctor = doctorRepository.findByIdWithRelations(id).orElseThrow();

    // Evita LazyInitializationException cuando Open Session In View está deshabilitado.
    // Aunque el repo usa fetch join, en algunos casos (sin filas en la relación) Hibernate
    // puede dejar la colección como proxy perezoso.
    doctor.getServices().size();

    return doctor;
  }

  /**
   * Obtiene un médico activo. Útil para APIs/UI donde no debe usarse un médico desactivado.
   */
  public Doctor getActive(Long id) {
    Doctor d = findById(id);
    if (!d.isActive()) {
      throw new IllegalArgumentException("Médico inactivo");
    }
    return d;
  }

  @Transactional(readOnly = true)
  public Doctor getActiveWithRelations(Long id) {
    Doctor d = findByIdWithRelations(id);
    if (!d.isActive()) {
      throw new IllegalArgumentException("Médico inactivo");
    }
    return d;
  }

  @Cacheable(value = "selectDoctors", sync = true)
  public List<Doctor> listActiveForSelect() {
    return doctorRepository.findActiveForSelect();
  }

  @CacheEvict(value = {"selectDoctors"}, allEntries = true)
  public Doctor saveOrUpdate(DoctorForm form) {
    Doctor d = (form.getId() != null) ? findById(form.getId()) : new Doctor();

    d.setFullName(form.getFullName());
    d.setLicenseNo(blankToNull(form.getLicenseNo()));
    d.setPhone(normalizeRdPhone(blankToNull(form.getPhone())));
    

    // Buffer entre citas (minutos). Default: 5.
    Integer bm = form.getBufferMinutes();
    d.setBufferMinutes((bm == null) ? 5 : Math.max(0, Math.min(bm, 60)));
    d.setActive(true);

    // Horario laboral
    LocalTime start = (form.getWorkStart() != null) ? form.getWorkStart() : LocalTime.of(8, 0);
    LocalTime end = (form.getWorkEnd() != null) ? form.getWorkEnd() : LocalTime.of(17, 0);
    if (end.isBefore(start) || end.equals(start)) {
      // Ajuste defensivo: si vienen invertidos, forzamos 8h de jornada.
      start = LocalTime.of(8, 0);
      end = LocalTime.of(17, 0);
    }
    d.setWorkStart(start);
    d.setWorkEnd(end);

    d.setWorkDays(normalizeWorkDays(form.getWorkDays()));

    // Especialidad: prioriza FK; si no viene, conserva texto libre
    Specialty selected = null;
    if (form.getSpecialtyId() != null) {
      selected = specialtyRepository.findById(form.getSpecialtyId()).orElse(null);
    }
    d.setSpecialty(selected);
    if (selected != null) {
      d.setSpecialtyText(selected.getName());
    } else {
      d.setSpecialtyText(blankToNull(form.getSpecialty()));
    }

    // Servicios ofrecidos
    Set<MedicalService> selectedServices = new HashSet<>();
    if (form.getServiceIds() != null && !form.getServiceIds().isEmpty()) {
      selectedServices.addAll(medicalServiceRepository.findAllById(form.getServiceIds()));
    }
    d.setServices(selectedServices);

    Doctor saved = doctorRepository.save(d);
    auditService.log(form.getId() == null ? "CREATE" : "UPDATE", "Doctor", saved.getId(), saved.getFullName());
    return saved;
  }

  @CacheEvict(value = {"selectDoctors"}, allEntries = true)
  public void deactivate(Long id) {
    Doctor d = findById(id);
    d.setActive(false);
    doctorRepository.save(d);
    auditService.log("DEACTIVATE", "Doctor", id, d.getFullName());
  }

  private String blankToNull(String v) {
    if (v == null) return null;
    String t = v.trim();
    return t.isEmpty() ? null : t;
  }

  /**
   * Normaliza teléfonos dominicanos (RD) al formato 000-000-0000 cuando hay 10 dígitos.
   */
  private String normalizeRdPhone(String phone) {
    String t = blankToNull(phone);
    if (t == null) return null;
    String digits = t.replaceAll("\\D", "");
    if (digits.isEmpty()) return null;
    if (digits.length() == 10) {
      return digits.substring(0, 3) + "-" + digits.substring(3, 6) + "-" + digits.substring(6);
    }
    return digits;
  }

  private String normalizeWorkDays(List<String> days) {
    List<String> order = List.of("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN");
    if (days == null || days.isEmpty()) {
      return String.join(",", order.subList(0, 5));
    }
    Set<String> normalized = new HashSet<>();
    for (String d : days) {
      if (d == null) continue;
      String up = d.trim().toUpperCase();
      if (order.contains(up)) {
        normalized.add(up);
      }
    }
    if (normalized.isEmpty()) {
      return String.join(",", order.subList(0, 5));
    }
    List<String> sorted = order.stream().filter(normalized::contains).toList();
    return String.join(",", sorted);
  }

  public DoctorStats stats(Long doctorId, int days) {
    int range = Math.max(1, Math.min(days, 365));
    java.time.LocalDate today = java.time.LocalDate.now();
    java.time.LocalDate from = today.minusDays(range);
    java.time.LocalDateTime fromDt = from.atStartOfDay();
    java.time.LocalDateTime toDt = today.plusDays(1).atStartOfDay();

    long total = appointmentRepository.countByDoctorAndRange(doctorId, fromDt, toDt);
    long completadas = appointmentRepository.countByDoctorAndStatus(doctorId, com.baldwin.praecura.entity.AppointmentStatus.COMPLETADA, fromDt, toDt);
    long canceladas = appointmentRepository.countByDoctorAndStatus(doctorId, com.baldwin.praecura.entity.AppointmentStatus.CANCELADA, fromDt, toDt);
    long noAsistio = appointmentRepository.countByDoctorAndStatus(doctorId, com.baldwin.praecura.entity.AppointmentStatus.NO_ASISTIO, fromDt, toDt);

    long denom = Math.max(1, total - canceladas);
    double noShowRate = ((double) noAsistio / (double) denom) * 100.0;
    return new DoctorStats(total, completadas, canceladas, noAsistio, noShowRate, range);
  }

  @Transactional(readOnly = true)
  public java.util.Map<Long, DoctorStats> statsForDoctors(List<Long> doctorIds, int days) {
    if (doctorIds == null || doctorIds.isEmpty()) {
      return java.util.Collections.emptyMap();
    }
    int range = Math.max(1, Math.min(days, 365));
    java.time.LocalDate today = java.time.LocalDate.now();
    java.time.LocalDate from = today.minusDays(range);
    java.time.LocalDateTime fromDt = from.atStartOfDay();
    java.time.LocalDateTime toDt = today.plusDays(1).atStartOfDay();

    var rows = appointmentRepository.productivityByDoctorIds(doctorIds, fromDt, toDt);
    java.util.Map<Long, DoctorStats> out = new java.util.HashMap<>();

    for (var r : rows) {
      long total = r.getTotal() != null ? r.getTotal() : 0L;
      long completadas = r.getCompleted() != null ? r.getCompleted() : 0L;
      long canceladas = r.getCancelled() != null ? r.getCancelled() : 0L;
      long noAsistio = r.getNoShow() != null ? r.getNoShow() : 0L;
      long denom = Math.max(1, total - canceladas);
      double noShowRate = ((double) noAsistio / (double) denom) * 100.0;
      out.put(r.getDoctorId(), new DoctorStats(total, completadas, canceladas, noAsistio, noShowRate, range));
    }

    for (Long id : doctorIds) {
      out.putIfAbsent(id, new DoctorStats(0, 0, 0, 0, 0.0, range));
    }

    return out;
  }

  public record DoctorStats(long total, long completadas, long canceladas, long noAsistio, double noShowRate, int days) {}

  public java.util.List<DailyPoint> doctorSeries(Long doctorId, int days) {
    int range = Math.max(7, Math.min(days, 365));
    java.time.LocalDate today = java.time.LocalDate.now();
    java.time.LocalDate start = today.minusDays(range);
    java.util.List<DailyPoint> out = new java.util.ArrayList<>();
    java.time.LocalDate d = start;
    while (!d.isAfter(today)) {
      java.time.LocalDateTime fromDt = d.atStartOfDay();
      java.time.LocalDateTime toDt = d.plusDays(1).atStartOfDay();
      long total = appointmentRepository.countByDoctorAndRange(doctorId, fromDt, toDt);
      out.add(new DailyPoint(d, total));
      d = d.plusDays(1);
    }
    return out;
  }

  public record DailyPoint(java.time.LocalDate day, long total) {}
}
