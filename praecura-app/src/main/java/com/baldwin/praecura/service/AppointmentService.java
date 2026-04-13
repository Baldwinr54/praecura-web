package com.baldwin.praecura.service;

import com.baldwin.praecura.dto.AppointmentForm;
import com.baldwin.praecura.entity.*;
import com.baldwin.praecura.repository.*;
import com.baldwin.praecura.security.SecurityRoleUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

@Service
public class AppointmentService {

  private static final int AGENDA_BATCH_SIZE = 500;

  private final AppointmentRepository appointmentRepository;
  private final PatientRepository patientRepository;
  private final DoctorRepository doctorRepository;
  private final MedicalServiceRepository medicalServiceRepository;
  private final ClinicSiteRepository clinicSiteRepository;
  private final ClinicResourceRepository clinicResourceRepository;
  private final AuditService auditService;

  public AppointmentService(AppointmentRepository appointmentRepository,
                            PatientRepository patientRepository,
                            DoctorRepository doctorRepository,
                            MedicalServiceRepository medicalServiceRepository,
                            ClinicSiteRepository clinicSiteRepository,
                            ClinicResourceRepository clinicResourceRepository,
                            AuditService auditService) {
    this.appointmentRepository = appointmentRepository;
    this.patientRepository = patientRepository;
    this.doctorRepository = doctorRepository;
    this.medicalServiceRepository = medicalServiceRepository;
    this.clinicSiteRepository = clinicSiteRepository;
    this.clinicResourceRepository = clinicResourceRepository;
    this.auditService = auditService;
  }

  @Transactional(readOnly = true)
  public Page<Appointment> search(String q,
                                  Long doctorId,
                                  Long siteId,
                                  AppointmentStatus status,
                                  LocalDateTime fromDt,
                                  LocalDateTime toDt,
                                  Pageable pageable) {
    // Evita parámetros NULL sin tipo en consultas JPQL que luego se traducen a SQL con placeholders
    // (PostgreSQL puede fallar con: "could not determine data type of parameter $N").
    LocalDateTime safeFrom = (fromDt != null) ? fromDt : LocalDateTime.of(1970, 1, 1, 0, 0);
    LocalDateTime safeTo = (toDt != null) ? toDt : LocalDateTime.of(3000, 1, 1, 0, 0);
    return appointmentRepository.search(q, doctorId, siteId, status, safeFrom, safeTo, pageable);
  }

  @Transactional(readOnly = true)
  public Appointment get(Long id) {
    Appointment a = appointmentRepository.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("Cita no existe"));
    if (!a.isActive()) {
      throw new IllegalArgumentException("La cita está archivada.");
    }
    return a;
  }

  @Transactional(readOnly = true)
  public Page<Appointment> historyForPatient(Long patientId, Pageable pageable) {
    return appointmentRepository.historyForPatient(patientId, pageable);
  }

  @Transactional
  public Appointment saveOrUpdate(AppointmentForm form) {
    boolean isNew = (form.getId() == null);
    Appointment a = isNew ? new Appointment() : get(form.getId());

    Patient p = patientRepository.findById(form.getPatientId())
        .orElseThrow(() -> new IllegalArgumentException("Paciente no existe"));
    if (!p.isActive()) {
      throw new IllegalArgumentException("No se puede crear una cita para un paciente inactivo.");
    }
    boolean hasPhone = p.getPhone() != null && !p.getPhone().isBlank();
    boolean hasEmail = p.getEmail() != null && !p.getEmail().isBlank();
    if (!hasPhone && !hasEmail) {
      throw new IllegalArgumentException("El paciente no tiene teléfono ni email. Agrega al menos un contacto antes de crear la cita.");
    }

    Doctor d = doctorRepository.findById(form.getDoctorId())
        .orElseThrow(() -> new IllegalArgumentException("Médico no existe"));
    if (!d.isActive()) {
      throw new IllegalArgumentException("No se puede asignar un médico inactivo.");
    }

    if (form.getServiceId() == null) {
      throw new IllegalArgumentException("Debe seleccionar un servicio.");
    }
    MedicalService s = medicalServiceRepository.findById(form.getServiceId())
        .orElseThrow(() -> new IllegalArgumentException("Servicio no existe"));
    if (!s.isActive()) {
      throw new IllegalArgumentException("No se puede usar un servicio inactivo.");
    }

    if (form.getSiteId() == null) {
      throw new IllegalArgumentException("Debe seleccionar una sede.");
    }
    ClinicSite site = clinicSiteRepository.findById(form.getSiteId())
        .orElseThrow(() -> new IllegalArgumentException("Sede no existe"));
    if (!site.isActive()) {
      throw new IllegalArgumentException("No se puede usar una sede inactiva.");
    }

    if (form.getResourceId() == null) {
      throw new IllegalArgumentException("Debe seleccionar un recurso.");
    }
    ClinicResource resource = clinicResourceRepository.findById(form.getResourceId())
        .orElseThrow(() -> new IllegalArgumentException("Recurso no existe"));
    if (!resource.isActive()) {
      throw new IllegalArgumentException("No se puede usar un recurso inactivo.");
    }
    if (resource.getSite() != null && !resource.getSite().isActive()) {
      throw new IllegalArgumentException("No se puede usar un recurso de una sede inactiva.");
    }
    if (resource.getSite() != null && !resource.getSite().getId().equals(site.getId())) {
      throw new IllegalArgumentException("El recurso seleccionado no pertenece a la sede indicada.");
    }

    LocalDateTime scheduledAt = form.getScheduledAt();
    if (scheduledAt == null) {
      throw new IllegalArgumentException("La fecha/hora es obligatoria.");
    }

    if (scheduledAt.isBefore(LocalDateTime.now()) && !isAdmin()) {
      throw new IllegalArgumentException("No se permite crear/modificar citas en el pasado (solo ADMIN).");
    }

    int duration;
    if (s != null && form.isDurationAuto()) {
      duration = s.getDurationMinutes();
    } else {
      duration = form.getDurationMinutes();
    }
    if (duration <= 0) {
      duration = s.getDurationMinutes();
    }
    if (duration < 5 || duration > 480) {
      throw new IllegalArgumentException("La duración debe estar entre 5 y 480 minutos.");
    }

    validateDoctorAvailability(d, scheduledAt, duration);

    // Reglas de choque de horario (mismo médico)
    validateNoOverlap(a.getId(), d.getId(), scheduledAt, duration);

    if (resource != null) {
      validateResourceAvailability(resource, scheduledAt, duration, a.getId());
    }

    LocalDateTime checkedInAt = form.getCheckedInAt();
    LocalDateTime startedAt = form.getStartedAt();
    LocalDateTime completedAt = form.getCompletedAt();
    if (startedAt != null && checkedInAt != null && startedAt.isBefore(checkedInAt)) {
      throw new IllegalArgumentException("La hora de inicio no puede ser anterior al check-in.");
    }
    if (completedAt != null && startedAt != null && completedAt.isBefore(startedAt)) {
      throw new IllegalArgumentException("La hora de completado no puede ser anterior al inicio.");
    }

    a.setPatient(p);
    a.setDoctor(d);
    a.setService(s);
    a.setSite(site);
    a.setResource(resource);
    a.setScheduledAt(scheduledAt);
    a.setReason(blankToNull(form.getReason()));
    a.setDurationMinutes(duration);
    a.setNotes(blankToNull(form.getNotes()));
    a.setTriageLevel(form.getTriageLevel());
    a.setTriageNotes(blankToNull(form.getTriageNotes()));
    a.setCheckedInAt(checkedInAt);
    a.setStartedAt(startedAt);
    a.setCompletedAt(completedAt);
    AppointmentStatus targetStatus = (form.getStatus() != null) ? form.getStatus() : AppointmentStatus.PROGRAMADA;
    if (!isNew) {
      AppointmentStatus current = a.getStatus();
      if (current != null && !current.canTransitionTo(targetStatus)) {
        throw new IllegalArgumentException("Transición de estado no permitida: " + current.label() + " → " + targetStatus.label());
      }
    }

    a.setStatus(targetStatus);

    Appointment saved = appointmentRepository.save(a);

    auditService.log(form.getId() == null ? "CREATE" : "UPDATE", "Appointment", saved.getId(),
        String.format("%s | %s | %s", p.getFullName(), d.getFullName(), saved.getStatus()));

    return saved;
  }

  @Transactional
  public void cancel(Long id) {
    Appointment a = get(id);
    if (a.getStartedAt() != null || a.getCompletedAt() != null) {
      throw new IllegalArgumentException("No se puede cancelar una cita que ya inició atención.");
    }
    if (!a.getStatus().canTransitionTo(AppointmentStatus.CANCELADA)) {
      throw new IllegalArgumentException("La cita no puede cancelarse desde el estado actual: " + a.getStatus().label());
    }
    a.setStatus(AppointmentStatus.CANCELADA);
    appointmentRepository.save(a);
    auditService.log("CANCEL", "Appointment", id, "Cancelada");
  }

  @Transactional
  public void confirm(Long id) {
    Appointment a = get(id);
    if (!a.getStatus().canTransitionTo(AppointmentStatus.CONFIRMADA)) {
      throw new IllegalArgumentException("La cita no puede confirmarse desde el estado actual: " + a.getStatus().label());
    }
    a.setStatus(AppointmentStatus.CONFIRMADA);
    appointmentRepository.save(a);
    auditService.log("CONFIRM", "Appointment", id, "Confirmada");
  }

  @Transactional
  public void complete(Long id) {
    Appointment a = get(id);
    if (!a.getStatus().canTransitionTo(AppointmentStatus.COMPLETADA)) {
      throw new IllegalArgumentException("La cita no puede completarse desde el estado actual: " + a.getStatus().label());
    }
    LocalDateTime now = LocalDateTime.now();
    if (a.getCheckedInAt() == null) a.setCheckedInAt(now);
    if (a.getStartedAt() == null) a.setStartedAt(now);
    if (a.getCompletedAt() == null) a.setCompletedAt(now);
    a.setStatus(AppointmentStatus.COMPLETADA);
    appointmentRepository.save(a);
    auditService.log("COMPLETE", "Appointment", id, "Completada");
  }

  @Transactional
  public void markNoShow(Long id) {
    Appointment a = get(id);
    if (a.getCheckedInAt() != null || a.getStartedAt() != null) {
      throw new IllegalArgumentException("No se puede marcar no-show si el paciente ya hizo check-in o inició atención.");
    }
    if (!a.getStatus().canTransitionTo(AppointmentStatus.NO_ASISTIO)) {
      throw new IllegalArgumentException("La cita no puede marcarse como no-show desde el estado actual: " + a.getStatus().label());
    }
    a.setStatus(AppointmentStatus.NO_ASISTIO);
    appointmentRepository.save(a);
    auditService.log("NO_SHOW", "Appointment", id, "No asistió");
  }

  @Transactional
  public void delete(Long id) {
    Appointment a = get(id);
    a.setActive(false);
    appointmentRepository.save(a);
    auditService.log("ARCHIVE", "Appointment", id, "Archivada (soft delete)");
  }

  /**
   * Reprogramación dedicada (mantiene paciente/servicio, permite cambiar médico y hora).
   * Por UX: si estaba CONFIRMADA, vuelve a PROGRAMADA.
   */
  @Transactional
  public void reschedule(Long id, Long doctorId, LocalDateTime scheduledAt) {
    Appointment a = get(id);

    Doctor d = doctorRepository.findById(doctorId)
        .orElseThrow(() -> new IllegalArgumentException("Médico no existe"));
    if (!d.isActive()) {
      throw new IllegalArgumentException("No se puede asignar un médico inactivo.");
    }

    if (scheduledAt == null) {
      throw new IllegalArgumentException("La fecha/hora es obligatoria.");
    }

    if (scheduledAt.isBefore(LocalDateTime.now()) && !isAdmin()) {
      throw new IllegalArgumentException("No se permite reprogramar al pasado (solo ADMIN).");
    }

    int duration = a.getDurationMinutes() > 0 ? a.getDurationMinutes() : 30;
    validateDoctorAvailability(d, scheduledAt, duration);
    validateNoOverlap(a.getId(), d.getId(), scheduledAt, duration);
    if (a.getResource() != null) {
      validateResourceAvailability(a.getResource(), scheduledAt, duration, a.getId());
    }

    a.setDoctor(d);
    a.setScheduledAt(scheduledAt);
    a.setCheckedInAt(null);
    a.setStartedAt(null);
    a.setCompletedAt(null);

    // Reglas de estado al reprogramar
    if (a.getStatus() == AppointmentStatus.CONFIRMADA) {
      a.setStatus(AppointmentStatus.PROGRAMADA);
    }

    appointmentRepository.save(a);
    auditService.log("RESCHEDULE", "Appointment", id,
        String.format("Reprogramada: %s | %s | %s", a.getPatient().getFullName(), d.getFullName(), a.getScheduledAt()));
  }

  @Transactional
  public void checkIn(Long id) {
    Appointment a = get(id);
    if (a.getStatus() != AppointmentStatus.PROGRAMADA && a.getStatus() != AppointmentStatus.CONFIRMADA) {
      throw new IllegalArgumentException("Solo se puede hacer check-in de citas programadas o confirmadas.");
    }
    if (a.getStartedAt() != null || a.getCompletedAt() != null) {
      throw new IllegalArgumentException("La cita ya está en atención o finalizada.");
    }

    if (a.getCheckedInAt() == null) {
      a.setCheckedInAt(LocalDateTime.now());
    }
    if (a.getStatus() == AppointmentStatus.PROGRAMADA) {
      a.setStatus(AppointmentStatus.CONFIRMADA);
    }
    appointmentRepository.save(a);
    auditService.log("CHECK_IN", "Appointment", id, "Recepción check-in");
  }

  @Transactional
  public void startAttention(Long id) {
    Appointment a = get(id);
    if (a.getStatus() != AppointmentStatus.PROGRAMADA && a.getStatus() != AppointmentStatus.CONFIRMADA) {
      throw new IllegalArgumentException("Solo se puede iniciar atención desde una cita activa.");
    }
    if (a.getCompletedAt() != null || a.getStatus() == AppointmentStatus.COMPLETADA) {
      throw new IllegalArgumentException("La cita ya está completada.");
    }

    LocalDateTime now = LocalDateTime.now();
    if (a.getCheckedInAt() == null) a.setCheckedInAt(now);
    if (a.getStartedAt() == null) a.setStartedAt(now);
    if (a.getStatus() == AppointmentStatus.PROGRAMADA) {
      a.setStatus(AppointmentStatus.CONFIRMADA);
    }
    appointmentRepository.save(a);
    auditService.log("START_ATTENTION", "Appointment", id, "Recepción inicio de atención");
  }

  @Transactional
  public void undoCheckIn(Long id) {
    Appointment a = get(id);
    if (a.getStatus() == AppointmentStatus.CANCELADA
        || a.getStatus() == AppointmentStatus.COMPLETADA
        || a.getStatus() == AppointmentStatus.NO_ASISTIO) {
      throw new IllegalArgumentException("No se puede deshacer check-in en una cita finalizada.");
    }
    if (a.getStartedAt() != null || a.getCompletedAt() != null) {
      throw new IllegalArgumentException("No se puede deshacer check-in luego de iniciar atención.");
    }

    if (a.getCheckedInAt() == null) {
      return;
    }
    a.setCheckedInAt(null);
    if (a.getStatus() == AppointmentStatus.CONFIRMADA) {
      a.setStatus(AppointmentStatus.PROGRAMADA);
    }
    appointmentRepository.save(a);
    auditService.log("UNDO_CHECK_IN", "Appointment", id, "Recepción deshacer check-in");
  }

  @Transactional(readOnly = true)
  public AvailabilityResult checkAvailability(Long doctorId,
                                              LocalDateTime scheduledAt,
                                              int durationMinutes,
                                              Long appointmentId,
                                              Long resourceId) {
    if (doctorId == null || scheduledAt == null) {
      return new AvailabilityResult(false, "Selecciona médico y fecha/hora para validar.", null);
    }

    Doctor d = doctorRepository.findById(doctorId).orElse(null);
    if (d == null || !d.isActive()) {
      return new AvailabilityResult(false, "El médico no existe o está inactivo.", null);
    }

    int duration = durationMinutes > 0 ? durationMinutes : 30;
    LocalDateTime suggested = findNextAvailable(d, scheduledAt, duration, appointmentId);

    if (resourceId != null) {
      ClinicResource resource = clinicResourceRepository.findById(resourceId).orElse(null);
      if (resource == null || !resource.isActive()) {
        return new AvailabilityResult(false, "El recurso no existe o está inactivo.", null);
      }
      if (resource.getSite() != null && !resource.getSite().isActive()) {
        return new AvailabilityResult(false, "La sede del recurso está inactiva.", null);
      }
      boolean resourceOk = isResourceAvailable(resource, scheduledAt, duration, appointmentId);
      if (!resourceOk) {
        return new AvailabilityResult(false, "El recurso seleccionado ya está ocupado en ese horario.", null);
      }
    }

    if (suggested != null && suggested.equals(scheduledAt)) {
      return new AvailabilityResult(true, "Horario disponible.", suggested);
    }
    if (suggested != null) {
      return new AvailabilityResult(false, "Horario ocupado. Te sugerimos el próximo espacio disponible.", suggested);
    }
    return new AvailabilityResult(false, "No hay disponibilidad en el horario laboral para ese día.", null);
  }

  @Transactional
  public boolean markReminded(Long id) {
    Appointment a = get(id);
    if (a.getStatus() == AppointmentStatus.CANCELADA) {
      throw new IllegalArgumentException("No se puede marcar como recordada una cita cancelada.");
    }

    boolean nowReminded;
    if (a.getRemindedAt() == null) {
      a.setRemindedAt(LocalDateTime.now());
      nowReminded = true;
      auditService.log("REMIND", "Appointment", id, "Marcar recordado");
    } else {
      a.setRemindedAt(null);
      nowReminded = false;
      auditService.log("UNREMIND", "Appointment", id, "Desmarcar recordado");
    }

    appointmentRepository.save(a);
    return nowReminded;
  }

  @Transactional(readOnly = true)
  public List<Appointment> agendaForDay(LocalDate date, Long doctorId, Long siteId, AppointmentStatus status) {
    LocalDate day = (date != null) ? date : LocalDate.now();
    LocalDateTime from = day.atStartOfDay();
    LocalDateTime to = from.plusDays(1);
    return loadAgendaInBatches(from, to, doctorId, siteId, status);
  }

  @Transactional(readOnly = true)
  public void forEachAgendaForDay(LocalDate date,
                                  Long doctorId,
                                  Long siteId,
                                  AppointmentStatus status,
                                  Consumer<Appointment> consumer) {
    LocalDate day = (date != null) ? date : LocalDate.now();
    LocalDateTime from = day.atStartOfDay();
    LocalDateTime to = from.plusDays(1);
    streamAgendaInBatches(from, to, doctorId, siteId, status, batch -> batch.forEach(consumer));
  }

  @Transactional(readOnly = true)
  public List<Appointment> receptionQueue(LocalDate date, Long doctorId, Long siteId) {
    LocalDate day = (date != null) ? date : LocalDate.now();
    List<Appointment> queue = new ArrayList<>(agendaForDay(day, doctorId, siteId, null));
    queue.sort(Comparator.comparing(Appointment::getScheduledAt, Comparator.nullsLast(Comparator.naturalOrder())));
    return queue;
  }

  public ReceptionStats buildReceptionStats(List<Appointment> appointments) {
    if (appointments == null || appointments.isEmpty()) {
      return new ReceptionStats(0, 0, 0, 0, 0, 0, 0, 0, 0);
    }

    long total = appointments.size();
    long pendingCheckIn = 0;
    long waiting = 0;
    long inAttention = 0;
    long completed = 0;
    long noShow = 0;
    long cancelled = 0;
    long overdue = 0;
    LocalDateTime now = LocalDateTime.now();

    for (Appointment a : appointments) {
      AppointmentStatus st = a.getStatus();
      if (st == AppointmentStatus.CANCELADA) {
        cancelled++;
        continue;
      }
      if (st == AppointmentStatus.NO_ASISTIO) {
        noShow++;
        continue;
      }
      if (st == AppointmentStatus.COMPLETADA) {
        completed++;
        continue;
      }
      if (a.getStartedAt() != null) {
        inAttention++;
      } else if (a.getCheckedInAt() != null) {
        waiting++;
      } else {
        pendingCheckIn++;
        if (a.getScheduledAt() != null && a.getScheduledAt().isBefore(now.minusMinutes(10))) {
          overdue++;
        }
      }
    }

    long active = pendingCheckIn + waiting + inAttention;
    return new ReceptionStats(total, active, pendingCheckIn, waiting, inAttention, completed, noShow, cancelled, overdue);
  }

  public long waitingMinutes(Appointment appointment, LocalDateTime now) {
    if (appointment == null || appointment.getCheckedInAt() == null) return 0;
    LocalDateTime end = appointment.getStartedAt() != null ? appointment.getStartedAt() : now;
    return Math.max(0, Duration.between(appointment.getCheckedInAt(), end).toMinutes());
  }

  public long attentionMinutes(Appointment appointment, LocalDateTime now) {
    if (appointment == null || appointment.getStartedAt() == null) return 0;
    LocalDateTime end = appointment.getCompletedAt() != null ? appointment.getCompletedAt() : now;
    return Math.max(0, Duration.between(appointment.getStartedAt(), end).toMinutes());
  }

  @Transactional(readOnly = true)
  public List<Appointment> agendaForRange(LocalDate fromDate,
                                          LocalDate toDate,
                                          Long doctorId,
                                          Long siteId,
                                          AppointmentStatus status) {
    AgendaWindow window = resolveAgendaWindow(fromDate, toDate);
    return loadAgendaInBatches(window.from(), window.to(), doctorId, siteId, status);
  }

  @Transactional(readOnly = true)
  public void forEachAgendaForRange(LocalDate fromDate,
                                    LocalDate toDate,
                                    Long doctorId,
                                    Long siteId,
                                    AppointmentStatus status,
                                    Consumer<Appointment> consumer) {
    AgendaWindow window = resolveAgendaWindow(fromDate, toDate);
    streamAgendaInBatches(window.from(), window.to(), doctorId, siteId, status, batch -> batch.forEach(consumer));
  }

  private AgendaWindow resolveAgendaWindow(LocalDate fromDate, LocalDate toDate) {
    LocalDate startDay = (fromDate != null) ? fromDate : LocalDate.now();
    LocalDate endDay = (toDate != null) ? toDate : startDay;
    if (endDay.isBefore(startDay)) {
      // swap defensivo (UX): si el usuario invierte fechas, lo corregimos.
      LocalDate tmp = startDay;
      startDay = endDay;
      endDay = tmp;
    }
    LocalDateTime from = startDay.atStartOfDay();
    // Incluye el día final completo.
    LocalDateTime to = endDay.plusDays(1).atStartOfDay();
    return new AgendaWindow(from, to);
  }

  private List<Appointment> loadAgendaInBatches(LocalDateTime from,
                                                LocalDateTime to,
                                                Long doctorId,
                                                Long siteId,
                                                AppointmentStatus status) {
    List<Appointment> appointments = new ArrayList<>();
    streamAgendaInBatches(from, to, doctorId, siteId, status, appointments::addAll);
    return appointments;
  }

  private void streamAgendaInBatches(LocalDateTime from,
                                     LocalDateTime to,
                                     Long doctorId,
                                     Long siteId,
                                     AppointmentStatus status,
                                     Consumer<List<Appointment>> batchConsumer) {
    int page = 0;
    Page<Appointment> current;
    do {
      current = appointmentRepository.search(
          null,
          doctorId,
          siteId,
          status,
          from,
          to,
          PageRequest.of(page, AGENDA_BATCH_SIZE)
      );
      if (!current.getContent().isEmpty()) {
        batchConsumer.accept(current.getContent());
      }
      page++;
    } while (current.hasNext());
  }

  private record AgendaWindow(LocalDateTime from, LocalDateTime to) {}

  private void validateNoOverlap(Long currentId, Long doctorId, LocalDateTime start, int durationMinutes) {
    LocalDate day = start.toLocalDate();
    // Ventana ampliada para cubrir casos cerca de medianoche (buffer puede cruzar de día)
    LocalDateTime fromDt = day.minusDays(1).atStartOfDay();
    LocalDateTime toDt = day.plusDays(2).atStartOfDay();

    List<AppointmentStatus> excluded = List.of(AppointmentStatus.CANCELADA, AppointmentStatus.NO_ASISTIO);
    List<Appointment> existing = appointmentRepository.findDoctorDayNonCancelled(doctorId, fromDt, toDt, excluded);

    int bufferMinutes = doctorRepository.findById(doctorId)
        .map(Doctor::getBufferMinutes)
        .orElse(0);

    LocalDateTime end = start.plusMinutes(durationMinutes);
    LocalDateTime endWithBuffer = end.plusMinutes(bufferMinutes);

    for (Appointment e : existing) {
      if (currentId != null && e.getId().equals(currentId)) continue;

      LocalDateTime eStart = e.getScheduledAt();
      LocalDateTime eEnd = eStart.plusMinutes(e.getDurationMinutes());
      LocalDateTime eEndWithBuffer = eEnd.plusMinutes(bufferMinutes);

      boolean overlaps = eStart.isBefore(endWithBuffer) && start.isBefore(eEndWithBuffer);
      if (overlaps) {
        throw new IllegalArgumentException(
            "Choque de horario: el doctor ya tiene una cita que se solapa con este rango (incluyendo buffer).");
      }
    }
  }

  private void validateResourceAvailability(ClinicResource resource, LocalDateTime start, int durationMinutes, Long appointmentId) {
    if (resource == null || start == null) return;

    LocalDate day = start.toLocalDate();
    LocalDateTime fromDt = day.minusDays(1).atStartOfDay();
    LocalDateTime toDt = day.plusDays(2).atStartOfDay();

    List<AppointmentStatus> excluded = List.of(AppointmentStatus.CANCELADA, AppointmentStatus.NO_ASISTIO);
    List<Appointment> existing = appointmentRepository.findResourceDayNonCancelled(resource.getId(), fromDt, toDt, excluded);

    LocalDateTime end = start.plusMinutes(durationMinutes);
    for (Appointment e : existing) {
      if (appointmentId != null && appointmentId.equals(e.getId())) continue;
      LocalDateTime eStart = e.getScheduledAt();
      LocalDateTime eEnd = eStart.plusMinutes(e.getDurationMinutes());
      boolean overlaps = eStart.isBefore(end) && start.isBefore(eEnd);
      if (overlaps) {
        throw new IllegalArgumentException("Choque de recurso: el espacio/equipo ya está reservado en ese rango.");
      }
    }
  }

  private boolean isResourceAvailable(ClinicResource resource, LocalDateTime start, int durationMinutes, Long appointmentId) {
    if (resource == null || start == null) return true;
    LocalDate day = start.toLocalDate();
    LocalDateTime fromDt = day.minusDays(1).atStartOfDay();
    LocalDateTime toDt = day.plusDays(2).atStartOfDay();
    List<AppointmentStatus> excluded = List.of(AppointmentStatus.CANCELADA, AppointmentStatus.NO_ASISTIO);
    List<Appointment> existing = appointmentRepository.findResourceDayNonCancelled(resource.getId(), fromDt, toDt, excluded);

    LocalDateTime end = start.plusMinutes(durationMinutes);
    for (Appointment e : existing) {
      if (appointmentId != null && appointmentId.equals(e.getId())) continue;
      LocalDateTime eStart = e.getScheduledAt();
      LocalDateTime eEnd = eStart.plusMinutes(e.getDurationMinutes());
      boolean overlaps = eStart.isBefore(end) && start.isBefore(eEnd);
      if (overlaps) {
        return false;
      }
    }
    return true;
  }

  private void validateDoctorAvailability(Doctor doctor, LocalDateTime start, int durationMinutes) {
    if (doctor == null || start == null) return;

    String workDays = doctor.getWorkDays();
    if (workDays != null && !workDays.isBlank()) {
      String dow = start.getDayOfWeek().name().substring(0, 3);
      boolean allowed = false;
      for (String d : workDays.split(",")) {
        if (d == null) continue;
        if (d.trim().equalsIgnoreCase(dow)) {
          allowed = true;
          break;
        }
      }
      if (!allowed) {
        throw new IllegalArgumentException("El médico no atiende el día seleccionado.");
      }
    }

    if (doctor.getWorkStart() != null && doctor.getWorkEnd() != null) {
      int buffer = Math.max(0, doctor.getBufferMinutes());
      java.time.LocalTime startTime = start.toLocalTime();
      java.time.LocalTime endTime = startTime.plusMinutes(durationMinutes + buffer);

      if (endTime.isBefore(startTime)) {
        throw new IllegalArgumentException("La cita cruza el límite del día y no es válida en el horario laboral.");
      }

      if (startTime.isBefore(doctor.getWorkStart()) || endTime.isAfter(doctor.getWorkEnd())) {
        throw new IllegalArgumentException("La cita queda fuera del horario laboral del médico.");
      }
    }
  }

  private LocalDateTime findNextAvailable(Doctor doctor, LocalDateTime requested, int durationMinutes, Long appointmentId) {
    if (doctor == null || requested == null) return null;

    LocalTime workStart = doctor.getWorkStart() != null ? doctor.getWorkStart() : LocalTime.of(8, 0);
    LocalTime workEnd = doctor.getWorkEnd() != null ? doctor.getWorkEnd() : LocalTime.of(17, 0);

    Set<String> days = parseWorkDays(doctor.getWorkDays());
    LocalDate baseDay = requested.toLocalDate();
    if (!days.isEmpty()) {
      int attempts = 0;
      while (attempts < 14 && !days.contains(dayCode(baseDay.getDayOfWeek()))) {
        baseDay = baseDay.plusDays(1);
        attempts++;
      }
      if (attempts >= 14) return null;
    }

    LocalDateTime windowStart = baseDay.atTime(workStart);
    LocalDateTime windowEnd = baseDay.atTime(workEnd);

    LocalDateTime candidate = requested.isBefore(windowStart) ? windowStart : requested;
    if (candidate.isAfter(windowEnd)) return null;

    List<AppointmentStatus> excluded = List.of(AppointmentStatus.CANCELADA, AppointmentStatus.NO_ASISTIO);
    List<Appointment> existing = appointmentRepository.findDoctorDayNonCancelled(
        doctor.getId(),
        baseDay.atStartOfDay(),
        baseDay.plusDays(1).atStartOfDay(),
        excluded
    );

    existing.sort((a, b) -> a.getScheduledAt().compareTo(b.getScheduledAt()));

    int bufferMinutes = Math.max(0, doctor.getBufferMinutes());
    for (Appointment e : existing) {
      if (appointmentId != null && appointmentId.equals(e.getId())) continue;
      LocalDateTime eStart = e.getScheduledAt();
      LocalDateTime eEnd = eStart.plusMinutes(e.getDurationMinutes() + bufferMinutes);

      LocalDateTime candidateEnd = candidate.plusMinutes(durationMinutes + bufferMinutes);
      if (!candidateEnd.isAfter(eStart)) {
        if (!candidateEnd.isAfter(windowEnd)) {
          return candidate;
        }
      }

      if (candidate.isBefore(eEnd)) {
        candidate = eEnd;
      }
    }

    LocalDateTime candidateEnd = candidate.plusMinutes(durationMinutes + bufferMinutes);
    if (!candidateEnd.isAfter(windowEnd)) {
      return candidate;
    }
    return null;
  }

  private Set<String> parseWorkDays(String raw) {
    if (raw == null || raw.isBlank()) return Set.of();
    String[] parts = raw.split(",");
    Set<String> out = new HashSet<>();
    for (String p : parts) {
      if (p == null) continue;
      String t = p.trim().toUpperCase();
      if (!t.isEmpty()) out.add(t);
    }
    return out;
  }

  private String dayCode(DayOfWeek dow) {
    return switch (dow) {
      case MONDAY -> "MON";
      case TUESDAY -> "TUE";
      case WEDNESDAY -> "WED";
      case THURSDAY -> "THU";
      case FRIDAY -> "FRI";
      case SATURDAY -> "SAT";
      case SUNDAY -> "SUN";
    };
  }

  public record AvailabilityResult(boolean ok, String message, LocalDateTime suggestedAt) {}

  public record ReceptionStats(
      long total,
      long active,
      long pendingCheckIn,
      long waiting,
      long inAttention,
      long completed,
      long noShow,
      long cancelled,
      long overdue
  ) {}

  private boolean isAdmin() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    return SecurityRoleUtils.hasAdminAuthority(auth);
  }

  private String blankToNull(String v) {
    if (v == null) return null;
    String t = v.trim();
    return t.isEmpty() ? null : t;
  }
}
