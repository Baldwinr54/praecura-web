package com.baldwin.praecura.service;

import com.baldwin.praecura.entity.Appointment;
import com.baldwin.praecura.entity.AppointmentStatus;
import com.baldwin.praecura.repository.AppointmentRepository;
import com.baldwin.praecura.repository.DoctorRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class DashboardService {

  private final AppointmentRepository appointmentRepository;
  private final DoctorRepository doctorRepository;
  private final MetricsSnapshotService metricsSnapshotService;

  public DashboardService(AppointmentRepository appointmentRepository,
                          DoctorRepository doctorRepository,
                          MetricsSnapshotService metricsSnapshotService) {
    this.appointmentRepository = appointmentRepository;
    this.doctorRepository = doctorRepository;
    this.metricsSnapshotService = metricsSnapshotService;
  }

  public DashboardSummary loadTodaySummary(LocalDate today) {
    LocalDateTime from = today.atStartOfDay();
    LocalDateTime to = today.plusDays(1).atStartOfDay();

    // Para métricas operativas, canceladas quedan separadas.
    List<AppointmentStatus> excludedForOps = List.of(AppointmentStatus.CANCELADA);
    List<AppointmentStatus> excludedForUpcoming = List.of(
        AppointmentStatus.CANCELADA,
        AppointmentStatus.COMPLETADA,
        AppointmentStatus.NO_ASISTIO
    );

    StatusCounters counters = loadStatusCounters(from, to);
    long citasHoy = counters.total();
    long programadas = counters.programadas();
    long confirmadas = counters.confirmadas();
    long pendientes = programadas + confirmadas;
    long canceladas = counters.canceladas();
    long noAsistio = counters.noAsistio();
    long atendidas = counters.completadas();
    long medicosActivos = doctorRepository.countByActiveTrue();

    // Próximas 2 horas: lista compacta para front-desk.
    LocalDateTime now = LocalDateTime.now();
    List<Appointment> proximas2h = appointmentRepository.findTop10ByScheduledAtBetweenAndStatusNotInAndActiveTrueOrderByScheduledAtAsc(
        now, now.plusHours(2), excludedForUpcoming);

    // Carga por médico (hoy)
    List<DoctorLoad> cargaPorMedico = appointmentRepository.loadByDoctor(from, to, excludedForOps).stream()
        .map(r -> new DoctorLoad(r.getDoctorId(), r.getDoctorName(), r.getTotal() == null ? 0L : r.getTotal()))
        .toList();

    // Alertas operativas y de calidad de datos.
    long citasSinServicioHoy = appointmentRepository.countMissingService(from, to, excludedForOps);
    long pacientesSinTelefonoProx7d = appointmentRepository.countDistinctPatientsMissingPhone(now, now.plusDays(7), excludedForOps);
    long recordatoriosPendientesProx24h = appointmentRepository.countReminderPending(
        now, now.plusHours(24), List.of(AppointmentStatus.PROGRAMADA, AppointmentStatus.CONFIRMADA));
    long citasConMedicoInactivoFuturo90d = appointmentRepository.countWithInactiveDoctor(now, now.plusDays(90), excludedForOps);

    double noShowRatePct = 0.0;
    long denom = Math.max(1, (citasHoy - canceladas));
    noShowRatePct = ((double) noAsistio / (double) denom) * 100.0;

    Alerts alerts = new Alerts(
        citasSinServicioHoy,
        pacientesSinTelefonoProx7d,
        recordatoriosPendientesProx24h,
        citasConMedicoInactivoFuturo90d,
        canceladas > 10,
        noShowRatePct >= 25.0,
        pendientes > 25
    );

    List<ActionItem> actions = new java.util.ArrayList<>();
    if (citasSinServicioHoy > 0) {
      actions.add(new ActionItem("Completar servicios",
          "Hay citas hoy sin servicio. Asigna el servicio para evitar reportes incompletos.",
          "/appointments"));
    }
    if (pacientesSinTelefonoProx7d > 0) {
      actions.add(new ActionItem("Actualizar contactos",
          "Pacientes sin teléfono en los próximos 7 días. Actualiza el contacto para recordatorios.",
          "/patients?missingPhone=true"));
    }
    if (recordatoriosPendientesProx24h > 0) {
      actions.add(new ActionItem("Enviar recordatorios",
          "Citas próximas sin recordatorio. Revisa y marca recordatorios pendientes.",
          "/appointments?status=PROGRAMADA"));
    }
    if (citasConMedicoInactivoFuturo90d > 0) {
      actions.add(new ActionItem("Reasignar médico",
          "Hay citas futuras con médico inactivo. Reasigna para evitar conflictos.",
          "/appointments?status=PROGRAMADA"));
    }
    if (noShowRatePct >= 25.0) {
      actions.add(new ActionItem("Reducir no‑show",
          "No‑show alto hoy. Prioriza confirmaciones o anticipos.",
          "/reports/analytics"));
    }

    return new DashboardSummary(
        today,
        citasHoy,
        programadas,
        confirmadas,
        pendientes,
        canceladas,
        noAsistio,
        atendidas,
        noShowRatePct,
        medicosActivos,
        proximas2h,
        cargaPorMedico,
        alerts,
        actions
    );
  }

  public MetricsSeries loadMetricsSeries(LocalDate from, LocalDate to) {
    List<com.baldwin.praecura.entity.MetricsDaily> rows = metricsSnapshotService.ensureSnapshots(from, to);
    List<String> labels = rows.stream()
        .map(com.baldwin.praecura.entity.MetricsDaily::getDay)
        .map(day -> day != null ? day.toString() : "")
        .toList();
    List<Long> total = rows.stream().map(com.baldwin.praecura.entity.MetricsDaily::getTotal).toList();
    List<Long> pendientes = rows.stream().map(com.baldwin.praecura.entity.MetricsDaily::getPendientes).toList();
    List<Long> canceladas = rows.stream().map(com.baldwin.praecura.entity.MetricsDaily::getCanceladas).toList();
    List<Long> noAsistio = rows.stream().map(com.baldwin.praecura.entity.MetricsDaily::getNoAsistio).toList();
    return new MetricsSeries(labels, total, pendientes, canceladas, noAsistio);
  }

  public NavBadges loadNavBadges(LocalDate today) {
    LocalDateTime from = today.atStartOfDay();
    LocalDateTime to = today.plusDays(1).atStartOfDay();

    StatusCounters counters = loadStatusCounters(from, to);
    long citasHoy = counters.total();
    long programadas = counters.programadas();
    long confirmadas = counters.confirmadas();
    long pendientes = programadas + confirmadas;
    long canceladas = counters.canceladas();
    long noAsistio = counters.noAsistio();

    long denom = Math.max(1, (citasHoy - canceladas));
    double noShowRatePct = ((double) noAsistio / (double) denom) * 100.0;
    int alertas = 0;
    if (canceladas > 10) alertas += 1;
    if (noShowRatePct >= 25.0) alertas += 1;
    if (pendientes > 25) alertas += 1;

    return new NavBadges(pendientes, citasHoy, alertas);
  }

  private StatusCounters loadStatusCounters(LocalDateTime from, LocalDateTime to) {
    Map<AppointmentStatus, Long> counts = new EnumMap<>(AppointmentStatus.class);
    long total = 0L;
    for (Object[] row : appointmentRepository.countByStatus(from, to)) {
      if (row == null || row.length < 2) continue;
      long value = asLong(row[1]);
      total += value;
      if (row[0] instanceof AppointmentStatus status) {
        counts.put(status, value);
      }
    }
    return new StatusCounters(
        total,
        counts.getOrDefault(AppointmentStatus.PROGRAMADA, 0L),
        counts.getOrDefault(AppointmentStatus.CONFIRMADA, 0L),
        counts.getOrDefault(AppointmentStatus.CANCELADA, 0L),
        counts.getOrDefault(AppointmentStatus.NO_ASISTIO, 0L),
        counts.getOrDefault(AppointmentStatus.COMPLETADA, 0L)
    );
  }

  private long asLong(Object value) {
    if (value instanceof Long l) return l;
    if (value instanceof Integer i) return i.longValue();
    if (value instanceof Number n) return n.longValue();
    return 0L;
  }

  public record DashboardSummary(
      LocalDate fecha,
      long citasHoy,
      long programadas,
      long confirmadas,
      long pendientes,
      long canceladas,
      long noAsistio,
      long atendidas,
      double noShowRatePct,
      long medicosActivos,
      List<Appointment> proximas2h,
      List<DoctorLoad> cargaPorMedico,
      Alerts alerts,
      List<ActionItem> actions
  ) {}

  public record DoctorLoad(Long doctorId, String doctorName, long total) {}

  public record Alerts(
      long citasSinServicioHoy,
      long pacientesSinTelefonoProx7d,
      long recordatoriosPendientesProx24h,
      long citasConMedicoInactivoFuturo90d,
      boolean highCancelRate,
      boolean highNoShowRate,
      boolean highPendingLoad
  ) {}

  public record ActionItem(
      String title,
      String detail,
      String href
  ) {}

  public record MetricsSeries(
      List<String> labels,
      List<Long> total,
      List<Long> pendientes,
      List<Long> canceladas,
      List<Long> noAsistio
  ) {}

  public record NavBadges(
      long pendientesHoy,
      long citasHoy,
      int alertas
  ) {}

  private record StatusCounters(
      long total,
      long programadas,
      long confirmadas,
      long canceladas,
      long noAsistio,
      long completadas
  ) {}
}
