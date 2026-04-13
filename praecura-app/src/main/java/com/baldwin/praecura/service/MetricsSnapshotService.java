package com.baldwin.praecura.service;

import com.baldwin.praecura.entity.AppointmentStatus;
import com.baldwin.praecura.entity.MetricsDaily;
import com.baldwin.praecura.repository.AppointmentRepository;
import com.baldwin.praecura.repository.MetricsDailyRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MetricsSnapshotService {

  private final MetricsDailyRepository metricsDailyRepository;
  private final AppointmentRepository appointmentRepository;

  public MetricsSnapshotService(MetricsDailyRepository metricsDailyRepository,
                                AppointmentRepository appointmentRepository) {
    this.metricsDailyRepository = metricsDailyRepository;
    this.appointmentRepository = appointmentRepository;
  }

  @Transactional
  public List<MetricsDaily> ensureSnapshots(LocalDate from, LocalDate to) {
    LocalDate start = from;
    LocalDate end = to;
    if (start.isAfter(end)) {
      LocalDate tmp = start;
      start = end;
      end = tmp;
    }

    List<MetricsDaily> out = new ArrayList<>();
    LocalDate day = start;
    while (!day.isAfter(end)) {
      MetricsDaily d = metricsDailyRepository.findById(day).orElse(null);
      MetricsDaily computed = computeForDay(day);
      if (d == null) {
        d = computed;
      } else {
        d.setTotal(computed.getTotal());
        d.setPendientes(computed.getPendientes());
        d.setProgramadas(computed.getProgramadas());
        d.setConfirmadas(computed.getConfirmadas());
        d.setCompletadas(computed.getCompletadas());
        d.setCanceladas(computed.getCanceladas());
        d.setNoAsistio(computed.getNoAsistio());
        d.setNoShowRate(computed.getNoShowRate());
        d.setUpdatedAt(computed.getUpdatedAt());
      }
      metricsDailyRepository.save(d);
      out.add(d);
      day = day.plusDays(1);
    }
    return out;
  }

  private MetricsDaily computeForDay(LocalDate day) {
    LocalDateTime fromDt = day.atStartOfDay();
    LocalDateTime toDt = day.plusDays(1).atStartOfDay();

    long total = appointmentRepository.countByScheduledAtBetweenAndActiveTrue(fromDt, toDt);
    long programadas = appointmentRepository.countByScheduledAtBetweenAndStatusAndActiveTrue(fromDt, toDt, AppointmentStatus.PROGRAMADA);
    long confirmadas = appointmentRepository.countByScheduledAtBetweenAndStatusAndActiveTrue(fromDt, toDt, AppointmentStatus.CONFIRMADA);
    long completadas = appointmentRepository.countByScheduledAtBetweenAndStatusAndActiveTrue(fromDt, toDt, AppointmentStatus.COMPLETADA);
    long canceladas = appointmentRepository.countByScheduledAtBetweenAndStatusAndActiveTrue(fromDt, toDt, AppointmentStatus.CANCELADA);
    long noAsistio = appointmentRepository.countByScheduledAtBetweenAndStatusAndActiveTrue(fromDt, toDt, AppointmentStatus.NO_ASISTIO);

    long pendientes = programadas + confirmadas;
    long denom = Math.max(1, (total - canceladas));
    double noShowRate = ((double) noAsistio / (double) denom) * 100.0;

    MetricsDaily m = new MetricsDaily();
    m.setDay(day);
    m.setTotal(total);
    m.setPendientes(pendientes);
    m.setProgramadas(programadas);
    m.setConfirmadas(confirmadas);
    m.setCompletadas(completadas);
    m.setCanceladas(canceladas);
    m.setNoAsistio(noAsistio);
    m.setNoShowRate(noShowRate);
    m.setUpdatedAt(LocalDateTime.now());
    return m;
  }
}
