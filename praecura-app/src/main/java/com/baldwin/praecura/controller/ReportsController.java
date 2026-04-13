package com.baldwin.praecura.controller;

import com.baldwin.praecura.entity.AuditLog;
import com.baldwin.praecura.entity.AppointmentStatus;
import com.baldwin.praecura.entity.MessageChannel;
import com.baldwin.praecura.entity.MessageLog;
import com.baldwin.praecura.entity.MessageStatus;
import com.baldwin.praecura.entity.MetricsDaily;
import com.baldwin.praecura.entity.PaymentStatus;
import com.baldwin.praecura.repository.AuditLogRepository;
import com.baldwin.praecura.repository.AppointmentRepository;
import com.baldwin.praecura.repository.DoctorRepository;
import com.baldwin.praecura.repository.MessageLogRepository;
import com.baldwin.praecura.repository.MetricsDailyRepository;
import com.baldwin.praecura.repository.PaymentRepository;
import com.baldwin.praecura.service.MetricsSnapshotService;
import com.baldwin.praecura.service.OperationalComplianceService;
import com.baldwin.praecura.service.TabularExportService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.PrintWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

@Controller
public class ReportsController {

  private final AuditLogRepository auditLogRepository;
  private final AppointmentRepository appointmentRepository;
  private final DoctorRepository doctorRepository;
  private final MessageLogRepository messageLogRepository;
  private final MetricsDailyRepository metricsDailyRepository;
  private final MetricsSnapshotService metricsSnapshotService;
  private final PaymentRepository paymentRepository;
  private final OperationalComplianceService operationalComplianceService;
  private final TabularExportService tabularExportService;

  @Value("${praecura.audit.retention-days:180}")
  private int defaultRetentionDays;

  @Value("${praecura.audit.export-secret:}")
  private String auditExportSecret;

  public ReportsController(AuditLogRepository auditLogRepository,
                           AppointmentRepository appointmentRepository,
                           DoctorRepository doctorRepository,
                           MessageLogRepository messageLogRepository,
                           MetricsDailyRepository metricsDailyRepository,
                           MetricsSnapshotService metricsSnapshotService,
                           PaymentRepository paymentRepository,
                           OperationalComplianceService operationalComplianceService,
                           TabularExportService tabularExportService) {
    this.auditLogRepository = auditLogRepository;
    this.appointmentRepository = appointmentRepository;
    this.doctorRepository = doctorRepository;
    this.messageLogRepository = messageLogRepository;
    this.metricsDailyRepository = metricsDailyRepository;
    this.metricsSnapshotService = metricsSnapshotService;
    this.paymentRepository = paymentRepository;
    this.operationalComplianceService = operationalComplianceService;
    this.tabularExportService = tabularExportService;
  }

  @GetMapping("/reports")
  public String index(@RequestParam(required = false) LocalDate from,
                      @RequestParam(required = false) LocalDate to,
                      Model model) {
    LocalDate today = LocalDate.now();
    LocalDate fromDate = (from != null) ? from : today.minusDays(30);
    LocalDate toDate = (to != null) ? to : today;

    LocalDateTime fromDt = fromDate.atStartOfDay();
    LocalDateTime toDt = toDate.plusDays(1).atStartOfDay();

    long total = appointmentRepository.countByScheduledAtBetweenAndActiveTrue(fromDt, toDt);
    long programadas = appointmentRepository.countByScheduledAtBetweenAndStatusAndActiveTrue(fromDt, toDt, AppointmentStatus.PROGRAMADA);
    long confirmadas = appointmentRepository.countByScheduledAtBetweenAndStatusAndActiveTrue(fromDt, toDt, AppointmentStatus.CONFIRMADA);
    long completadas = appointmentRepository.countByScheduledAtBetweenAndStatusAndActiveTrue(fromDt, toDt, AppointmentStatus.COMPLETADA);
    long canceladas = appointmentRepository.countByScheduledAtBetweenAndStatusAndActiveTrue(fromDt, toDt, AppointmentStatus.CANCELADA);
    long noAsistio = appointmentRepository.countByScheduledAtBetweenAndStatusAndActiveTrue(fromDt, toDt, AppointmentStatus.NO_ASISTIO);

    long medicosActivos = doctorRepository.countByActiveTrue();
    long pendientes = programadas + confirmadas;
    long denom = Math.max(1, total - canceladas);
    double noShowRatePct = ((double) noAsistio / (double) denom) * 100.0;

    model.addAttribute("from", fromDate);
    model.addAttribute("to", toDate);
    model.addAttribute("metrics", new Metrics(
        total,
        pendientes,
        programadas,
        confirmadas,
        completadas,
        canceladas,
        noAsistio,
        noShowRatePct,
        medicosActivos
    ));
    return "reports/index";
  }

  @GetMapping("/reports/analytics")
  public String analytics(@RequestParam(required = false) LocalDate from,
                          @RequestParam(required = false) LocalDate to,
                          Model model) {
    LocalDate today = LocalDate.now();
    LocalDate fromDate = (from != null) ? from : today.minusDays(30);
    LocalDate toDate = (to != null) ? to : today;

    LocalDateTime fromDt = fromDate.atStartOfDay();
    LocalDateTime toDt = toDate.plusDays(1).atStartOfDay();

    var byDoctor = appointmentRepository.countByDoctor(fromDt, toDt);
    var byService = appointmentRepository.countByService(fromDt, toDt);
    var byStatus = appointmentRepository.countByStatus(fromDt, toDt);

    // Ensure snapshots for charting
    metricsSnapshotService.ensureSnapshots(fromDate, toDate);
    var series = metricsDailyRepository.findByDayBetweenOrderByDayAsc(fromDate, toDate);
    var seriesJs = series.stream()
        .map(s -> new SeriesPoint(
            s.getDay() != null ? s.getDay().toString() : "",
            s.getTotal(),
            s.getCanceladas(),
            s.getNoAsistio()
        ))
        .toList();

    long total = series.stream().mapToLong(s -> s.getTotal()).sum();
    long canceladas = series.stream().mapToLong(s -> s.getCanceladas()).sum();
    long noAsistio = series.stream().mapToLong(s -> s.getNoAsistio()).sum();
    long completadas = series.stream().mapToLong(s -> s.getCompletadas()).sum();
    long programadasSum = series.stream().mapToLong(s -> s.getProgramadas()).sum();
    long confirmadasSum = series.stream().mapToLong(s -> s.getConfirmadas()).sum();
    double denom = Math.max(1.0, (double) (total - canceladas));
    double noShowRate = (noAsistio / denom) * 100.0;
    double cancelRate = (canceladas / Math.max(1.0, (double) total)) * 100.0;
    double completionRate = (completadas / Math.max(1.0, (double) total)) * 100.0;
    double pendingShare = total > 0 ? ((double) (programadasSum + confirmadasSum) / (double) total) * 100.0 : 0.0;

    String topDoctor = byDoctor.isEmpty() ? "—" : byDoctor.get(0).getName();
    String topService = byService.isEmpty() ? "—" : byService.get(0).getName();
    boolean highNoShow = noShowRate >= 25.0;
    boolean highCancel = cancelRate >= 20.0;
    boolean highPending = pendingShare >= 45.0;

    List<ActionItem> actions = new ArrayList<>();
    if (highCancel) {
      actions.add(new ActionItem("Revisar cancelaciones",
          "Analiza las citas canceladas y ajusta confirmaciones.",
          "/appointments?status=CANCELADA"));
    }
    if (highNoShow) {
      actions.add(new ActionItem("Reducir no‑show",
          "Prioriza llamadas o recordatorios para pacientes con riesgo.",
          "/appointments?status=NO_ASISTIO"));
    }
    if (highPending) {
      actions.add(new ActionItem("Despejar pendientes",
          "Confirma o reprograma citas pendientes para mejorar el flujo.",
          "/appointments?status=PROGRAMADA"));
    }
    if (completionRate < 60.0) {
      actions.add(new ActionItem("Optimizar atención",
          "Eficiencia baja; revisa tiempos y procesos por médico.",
          "/reports/productivity"));
    }

    model.addAttribute("from", fromDate);
    model.addAttribute("to", toDate);
    model.addAttribute("byDoctor", byDoctor);
    model.addAttribute("byService", byService);
    model.addAttribute("byStatus", byStatus);
    model.addAttribute("series", series);
    model.addAttribute("seriesJs", seriesJs);
    model.addAttribute("actions", actions);
    model.addAttribute("insights", new AnalyticsInsights(
        total,
        cancelRate,
        noShowRate,
        completionRate,
        topDoctor,
        topService,
        highCancel,
        highNoShow
    ));
    return "reports/analytics";
  }

  @GetMapping(value = "/reports/analytics/export.csv", produces = "text/csv")
  public void exportAnalyticsCsv(@RequestParam(required = false) LocalDate from,
                                 @RequestParam(required = false) LocalDate to,
                                 HttpServletResponse response) throws Exception {
    LocalDate today = LocalDate.now();
    LocalDate fromDate = (from != null) ? from : today.minusDays(30);
    LocalDate toDate = (to != null) ? to : today;

    metricsSnapshotService.ensureSnapshots(fromDate, toDate);
    var series = metricsDailyRepository.findByDayBetweenOrderByDayAsc(fromDate, toDate);

    prepareCsvResponse(response, "praecura_analytics_series_" + fromDate + "_" + toDate + ".csv");
    try (PrintWriter out = csvWriter(response)) {
      writeBom(out);
      out.println(String.join(",", "day", "total", "pendientes", "programadas", "confirmadas", "completadas", "canceladas", "no_asistio", "no_show_rate"));
      for (var row : series) {
        writeCsvLine(out,
            row.getDay(),
            row.getTotal(),
            row.getPendientes(),
            row.getProgramadas(),
            row.getConfirmadas(),
            row.getCompletadas(),
            row.getCanceladas(),
            row.getNoAsistio(),
            String.format("%.2f", row.getNoShowRate())
        );
      }
      out.flush();
    }
  }

  @GetMapping(value = "/reports/analytics/export.xlsx", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
  public void exportAnalyticsXlsx(@RequestParam(required = false) LocalDate from,
                                  @RequestParam(required = false) LocalDate to,
                                  HttpServletResponse response) throws Exception {
    LocalDate today = LocalDate.now();
    LocalDate fromDate = (from != null) ? from : today.minusDays(30);
    LocalDate toDate = (to != null) ? to : today;

    LocalDateTime fromDt = fromDate.atStartOfDay();
    LocalDateTime toDt = toDate.plusDays(1).atStartOfDay();

    var byDoctor = appointmentRepository.countByDoctor(fromDt, toDt);
    var byService = appointmentRepository.countByService(fromDt, toDt);
    var byStatus = appointmentRepository.countByStatus(fromDt, toDt);

    metricsSnapshotService.ensureSnapshots(fromDate, toDate);
    var series = metricsDailyRepository.findByDayBetweenOrderByDayAsc(fromDate, toDate);

    List<SheetData> sheets = new ArrayList<>();
    sheets.add(new SheetData("Serie",
        List.of("day", "total", "pendientes", "programadas", "confirmadas", "completadas", "canceladas", "no_asistio", "no_show_rate"),
        series.stream()
            .map(r -> List.of(r.getDay(), r.getTotal(), r.getPendientes(), r.getProgramadas(), r.getConfirmadas(),
                r.getCompletadas(), r.getCanceladas(), r.getNoAsistio(), r.getNoShowRate()))
            .toList()));

    sheets.add(new SheetData("Doctores",
        List.of("doctor_id", "doctor_name", "total"),
        byDoctor.stream().map(r -> List.of(r.getId(), r.getName(), r.getTotal())).toList()));

    sheets.add(new SheetData("Servicios",
        List.of("service_id", "service_name", "total"),
        byService.stream().map(r -> List.of(r.getId(), r.getName(), r.getTotal())).toList()));

    sheets.add(new SheetData("Estados",
        List.of("status", "total"),
        byStatus.stream().map(r -> List.of(r[0], r[1])).toList()));

    writeXlsxMulti(response,
        "praecura_analytics_" + fromDate + "_" + toDate + ".xlsx",
        sheets);
  }

  @GetMapping("/reports/doctors")
  public String doctorsReport(@RequestParam(required = false) LocalDate from,
                              @RequestParam(required = false) LocalDate to,
                              @RequestParam(required = false) Integer limit,
                              Model model) {
    LocalDate today = LocalDate.now();
    LocalDate fromDate = (from != null) ? from : today.minusDays(30);
    LocalDate toDate = (to != null) ? to : today;

    LocalDateTime fromDt = fromDate.atStartOfDay();
    LocalDateTime toDt = toDate.plusDays(1).atStartOfDay();

    var byDoctor = appointmentRepository.countByDoctor(fromDt, toDt);
    int resolvedLimit = resolveLimit(limit, 50);
    var tableRows = limitList(byDoctor, resolvedLimit);
    var chartRows = limitList(byDoctor, 12);
    model.addAttribute("from", fromDate);
    model.addAttribute("to", toDate);
    model.addAttribute("byDoctor", tableRows);
    model.addAttribute("chartRows", chartRows);
    model.addAttribute("limit", resolvedLimit);
    model.addAttribute("totalRows", byDoctor.size());
    model.addAttribute("showingRows", tableRows.size());
    model.addAttribute("limited", byDoctor.size() > tableRows.size());
    return "reports/doctors";
  }

  @GetMapping(value = "/reports/doctors/export.csv", produces = "text/csv")
  public void exportDoctorsCsv(@RequestParam(required = false) LocalDate from,
                               @RequestParam(required = false) LocalDate to,
                               HttpServletResponse response) throws Exception {
    LocalDate today = LocalDate.now();
    LocalDate fromDate = (from != null) ? from : today.minusDays(30);
    LocalDate toDate = (to != null) ? to : today;

    LocalDateTime fromDt = fromDate.atStartOfDay();
    LocalDateTime toDt = toDate.plusDays(1).atStartOfDay();

    var byDoctor = appointmentRepository.countByDoctor(fromDt, toDt);
    prepareCsvResponse(response, "praecura_report_doctores_" + fromDate + "_" + toDate + ".csv");
    try (PrintWriter out = csvWriter(response)) {
      writeBom(out);
      out.println(String.join(",", "doctor_id", "doctor_name", "total"));
      for (var row : byDoctor) {
        writeCsvLine(out, row.getId(), row.getName(), row.getTotal());
      }
      out.flush();
    }
  }

  @GetMapping(value = "/reports/doctors/export.xlsx", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
  public void exportDoctorsXlsx(@RequestParam(required = false) LocalDate from,
                                @RequestParam(required = false) LocalDate to,
                                HttpServletResponse response) throws Exception {
    LocalDate today = LocalDate.now();
    LocalDate fromDate = (from != null) ? from : today.minusDays(30);
    LocalDate toDate = (to != null) ? to : today;

    LocalDateTime fromDt = fromDate.atStartOfDay();
    LocalDateTime toDt = toDate.plusDays(1).atStartOfDay();

    var byDoctor = appointmentRepository.countByDoctor(fromDt, toDt);
    writeXlsx(response,
        "praecura_report_doctores_" + fromDate + "_" + toDate + ".xlsx",
        "Doctores",
        List.of("doctor_id", "doctor_name", "total"),
        byDoctor.stream()
            .map(r -> List.of(r.getId(), r.getName(), r.getTotal()))
            .toList());
  }

  @GetMapping("/reports/services")
  public String servicesReport(@RequestParam(required = false) LocalDate from,
                               @RequestParam(required = false) LocalDate to,
                               @RequestParam(required = false) Integer limit,
                               Model model) {
    LocalDate today = LocalDate.now();
    LocalDate fromDate = (from != null) ? from : today.minusDays(30);
    LocalDate toDate = (to != null) ? to : today;

    LocalDateTime fromDt = fromDate.atStartOfDay();
    LocalDateTime toDt = toDate.plusDays(1).atStartOfDay();

    var byService = appointmentRepository.countByService(fromDt, toDt);
    int resolvedLimit = resolveLimit(limit, 50);
    var tableRows = limitList(byService, resolvedLimit);
    var chartRows = limitList(byService, 12);
    model.addAttribute("from", fromDate);
    model.addAttribute("to", toDate);
    model.addAttribute("byService", tableRows);
    model.addAttribute("chartRows", chartRows);
    model.addAttribute("limit", resolvedLimit);
    model.addAttribute("totalRows", byService.size());
    model.addAttribute("showingRows", tableRows.size());
    model.addAttribute("limited", byService.size() > tableRows.size());
    return "reports/services";
  }

  @GetMapping(value = "/reports/services/export.csv", produces = "text/csv")
  public void exportServicesCsv(@RequestParam(required = false) LocalDate from,
                                @RequestParam(required = false) LocalDate to,
                                HttpServletResponse response) throws Exception {
    LocalDate today = LocalDate.now();
    LocalDate fromDate = (from != null) ? from : today.minusDays(30);
    LocalDate toDate = (to != null) ? to : today;

    LocalDateTime fromDt = fromDate.atStartOfDay();
    LocalDateTime toDt = toDate.plusDays(1).atStartOfDay();

    var byService = appointmentRepository.countByService(fromDt, toDt);
    prepareCsvResponse(response, "praecura_report_servicios_" + fromDate + "_" + toDate + ".csv");
    try (PrintWriter out = csvWriter(response)) {
      writeBom(out);
      out.println(String.join(",", "service_id", "service_name", "total"));
      for (var row : byService) {
        writeCsvLine(out, row.getId(), row.getName(), row.getTotal());
      }
      out.flush();
    }
  }

  @GetMapping(value = "/reports/services/export.xlsx", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
  public void exportServicesXlsx(@RequestParam(required = false) LocalDate from,
                                 @RequestParam(required = false) LocalDate to,
                                 HttpServletResponse response) throws Exception {
    LocalDate today = LocalDate.now();
    LocalDate fromDate = (from != null) ? from : today.minusDays(30);
    LocalDate toDate = (to != null) ? to : today;

    LocalDateTime fromDt = fromDate.atStartOfDay();
    LocalDateTime toDt = toDate.plusDays(1).atStartOfDay();

    var byService = appointmentRepository.countByService(fromDt, toDt);
    writeXlsx(response,
        "praecura_report_servicios_" + fromDate + "_" + toDate + ".xlsx",
        "Servicios",
        List.of("service_id", "service_name", "total"),
        byService.stream()
            .map(r -> List.of(r.getId(), r.getName(), r.getTotal()))
            .toList());
  }

  @GetMapping("/reports/specialties")
  public String specialtiesReport(@RequestParam(required = false) LocalDate from,
                                  @RequestParam(required = false) LocalDate to,
                                  @RequestParam(required = false) Integer limit,
                                  Model model) {
    LocalDate today = LocalDate.now();
    LocalDate fromDate = (from != null) ? from : today.minusDays(30);
    LocalDate toDate = (to != null) ? to : today;

    LocalDateTime fromDt = fromDate.atStartOfDay();
    LocalDateTime toDt = toDate.plusDays(1).atStartOfDay();

    var bySpecialty = appointmentRepository.countBySpecialty(fromDt, toDt);
    int resolvedLimit = resolveLimit(limit, 50);
    var tableRows = limitList(bySpecialty, resolvedLimit);
    var chartRows = limitList(bySpecialty, 12);
    model.addAttribute("from", fromDate);
    model.addAttribute("to", toDate);
    model.addAttribute("bySpecialty", tableRows);
    model.addAttribute("chartRows", chartRows);
    model.addAttribute("limit", resolvedLimit);
    model.addAttribute("totalRows", bySpecialty.size());
    model.addAttribute("showingRows", tableRows.size());
    model.addAttribute("limited", bySpecialty.size() > tableRows.size());
    return "reports/specialties";
  }

  @GetMapping(value = "/reports/specialties/export.csv", produces = "text/csv")
  public void exportSpecialtiesCsv(@RequestParam(required = false) LocalDate from,
                                   @RequestParam(required = false) LocalDate to,
                                   HttpServletResponse response) throws Exception {
    LocalDate today = LocalDate.now();
    LocalDate fromDate = (from != null) ? from : today.minusDays(30);
    LocalDate toDate = (to != null) ? to : today;

    LocalDateTime fromDt = fromDate.atStartOfDay();
    LocalDateTime toDt = toDate.plusDays(1).atStartOfDay();

    var bySpecialty = appointmentRepository.countBySpecialty(fromDt, toDt);
    prepareCsvResponse(response, "praecura_report_especialidades_" + fromDate + "_" + toDate + ".csv");
    try (PrintWriter out = csvWriter(response)) {
      writeBom(out);
      out.println(String.join(",", "specialty_id", "specialty_name", "total"));
      for (var row : bySpecialty) {
        writeCsvLine(out, row.getId(), row.getName(), row.getTotal());
      }
      out.flush();
    }
  }

  @GetMapping(value = "/reports/specialties/export.xlsx", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
  public void exportSpecialtiesXlsx(@RequestParam(required = false) LocalDate from,
                                    @RequestParam(required = false) LocalDate to,
                                    HttpServletResponse response) throws Exception {
    LocalDate today = LocalDate.now();
    LocalDate fromDate = (from != null) ? from : today.minusDays(30);
    LocalDate toDate = (to != null) ? to : today;

    LocalDateTime fromDt = fromDate.atStartOfDay();
    LocalDateTime toDt = toDate.plusDays(1).atStartOfDay();

    var bySpecialty = appointmentRepository.countBySpecialty(fromDt, toDt);
    writeXlsx(response,
        "praecura_report_especialidades_" + fromDate + "_" + toDate + ".xlsx",
        "Especialidades",
        List.of("specialty_id", "specialty_name", "total"),
        bySpecialty.stream()
            .map(r -> List.of(r.getId(), r.getName(), r.getTotal()))
            .toList());
  }

  @GetMapping("/reports/hours")
  public String hoursReport(@RequestParam(required = false) LocalDate from,
                            @RequestParam(required = false) LocalDate to,
                            Model model) {
    LocalDate today = LocalDate.now();
    LocalDate fromDate = (from != null) ? from : today.minusDays(30);
    LocalDate toDate = (to != null) ? to : today;

    LocalDateTime fromDt = fromDate.atStartOfDay();
    LocalDateTime toDt = toDate.plusDays(1).atStartOfDay();

    var byHour = appointmentRepository.countByHour(fromDt, toDt);
    long total = byHour.stream().mapToLong(r -> r.getTotal() == null ? 0 : r.getTotal()).sum();
    var peak = byHour.stream()
        .filter(r -> r.getTotal() != null)
        .max(Comparator.comparingLong(r -> r.getTotal() == null ? 0 : r.getTotal()));
    String peakLabel = peak.isPresent() && peak.get().getHour() != null
        ? String.format("%02d:00", peak.get().getHour())
        : "—";
    double avgPerHour = byHour.isEmpty()
        ? 0.0
        : (double) total / (double) byHour.size();
    int peakHour = peak.isPresent() && peak.get().getHour() != null ? peak.get().getHour() : -1;
    int quietHour = byHour.stream()
        .filter(r -> r.getHour() != null)
        .min(Comparator.comparingLong(r -> r.getTotal() == null ? 0 : r.getTotal()))
        .map(r -> r.getHour())
        .orElse(-1);

    List<ActionItem> actions = new ArrayList<>();
    if (peakHour >= 0) {
      actions.add(new ActionItem("Refuerza personal en hora pico",
          "La hora con mayor demanda es " + String.format("%02d:00", peakHour) + ". Considera reforzar recepción.",
          "/agenda"));
    }
    if (quietHour >= 0) {
      actions.add(new ActionItem("Usa horas valle para reprogramar",
          "La hora con menor demanda es " + String.format("%02d:00", quietHour) + ". Útil para reprogramaciones.",
          "/appointments"));
    }

    model.addAttribute("from", fromDate);
    model.addAttribute("to", toDate);
    model.addAttribute("byHour", byHour);
    model.addAttribute("hourStats", new HourStats(total, peakLabel, avgPerHour));
    model.addAttribute("actions", actions);
    return "reports/hours";
  }

  @GetMapping(value = "/reports/hours/export.csv", produces = "text/csv")
  public void exportHoursCsv(@RequestParam(required = false) LocalDate from,
                             @RequestParam(required = false) LocalDate to,
                             HttpServletResponse response) throws Exception {
    LocalDate today = LocalDate.now();
    LocalDate fromDate = (from != null) ? from : today.minusDays(30);
    LocalDate toDate = (to != null) ? to : today;

    LocalDateTime fromDt = fromDate.atStartOfDay();
    LocalDateTime toDt = toDate.plusDays(1).atStartOfDay();

    var byHour = appointmentRepository.countByHour(fromDt, toDt);
    prepareCsvResponse(response, "praecura_report_horas_" + fromDate + "_" + toDate + ".csv");
    try (PrintWriter out = csvWriter(response)) {
      writeBom(out);
      out.println(String.join(",", "hour", "total"));
      for (var row : byHour) {
        writeCsvLine(out, row.getHour(), row.getTotal());
      }
      out.flush();
    }
  }

  @GetMapping(value = "/reports/hours/export.xlsx", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
  public void exportHoursXlsx(@RequestParam(required = false) LocalDate from,
                              @RequestParam(required = false) LocalDate to,
                              HttpServletResponse response) throws Exception {
    LocalDate today = LocalDate.now();
    LocalDate fromDate = (from != null) ? from : today.minusDays(30);
    LocalDate toDate = (to != null) ? to : today;

    LocalDateTime fromDt = fromDate.atStartOfDay();
    LocalDateTime toDt = toDate.plusDays(1).atStartOfDay();

    var byHour = appointmentRepository.countByHour(fromDt, toDt);
    writeXlsx(response,
        "praecura_report_horas_" + fromDate + "_" + toDate + ".xlsx",
        "Horas",
        List.of("hour", "total"),
        byHour.stream()
            .map(r -> List.of(r.getHour(), r.getTotal()))
            .toList());
  }

  @GetMapping("/reports/days")
  public String daysReport(@RequestParam(required = false) LocalDate from,
                           @RequestParam(required = false) LocalDate to,
                           Model model) {
    LocalDate today = LocalDate.now();
    LocalDate fromDate = (from != null) ? from : today.minusDays(30);
    LocalDate toDate = (to != null) ? to : today;

    LocalDateTime fromDt = fromDate.atStartOfDay();
    LocalDateTime toDt = toDate.plusDays(1).atStartOfDay();

    var byDay = appointmentRepository.countByDayOfWeek(fromDt, toDt);
    Map<Integer, Long> totals = new HashMap<>();
    for (var row : byDay) {
      if (row.getDow() == null) continue;
      totals.put(row.getDow(), row.getTotal() == null ? 0L : row.getTotal());
    }

    List<String> labels = List.of(
        "Domingo",
        "Lunes",
        "Martes",
        "Miércoles",
        "Jueves",
        "Viernes",
        "Sábado"
    );
    List<DaySummary> daySeries = new ArrayList<>();
    for (int i = 0; i < labels.size(); i++) {
      daySeries.add(new DaySummary(labels.get(i), totals.getOrDefault(i, 0L)));
    }

    long total = daySeries.stream().mapToLong(DaySummary::total).sum();
    DaySummary peak = daySeries.stream()
        .max(Comparator.comparingLong(DaySummary::total))
        .orElse(null);
    String peakLabel = (peak != null && peak.total() > 0) ? peak.label() : "—";
    double avgPerDay = total / 7.0;
    long minTotal = daySeries.stream().mapToLong(DaySummary::total).min().orElse(0L);
    String quietDay = daySeries.stream()
        .filter(d -> d.total() == minTotal)
        .map(DaySummary::label)
        .findFirst()
        .orElse("—");

    List<ActionItem> actions = new ArrayList<>();
    if (!peakLabel.equals("—")) {
      actions.add(new ActionItem("Refuerza el día pico",
          "El día con más demanda es " + peakLabel + ". Planifica más capacidad.",
          "/agenda/week"));
    }
    if (!quietDay.equals("—")) {
      actions.add(new ActionItem("Agenda reprogramaciones en días valle",
          "El día con menor demanda es " + quietDay + ". Útil para mover citas.",
          "/appointments"));
    }

    model.addAttribute("from", fromDate);
    model.addAttribute("to", toDate);
    model.addAttribute("daySeries", daySeries);
    model.addAttribute("dayStats", new DayStats(total, peakLabel, avgPerDay));
    model.addAttribute("actions", actions);
    return "reports/days";
  }

  @GetMapping("/reports/revenue")
  public String revenueReport(@RequestParam(required = false) LocalDate from,
                              @RequestParam(required = false) LocalDate to,
                              Model model) {
    LocalDate today = LocalDate.now();
    LocalDate fromDate = (from != null) ? from : today.minusDays(30);
    LocalDate toDate = (to != null) ? to : today;

    LocalDateTime fromDt = fromDate.atStartOfDay();
    LocalDateTime toDt = toDate.plusDays(1).atStartOfDay();

    long days = ChronoUnit.DAYS.between(fromDate, toDate) + 1;
    LocalDate prevToDate = fromDate.minusDays(1);
    LocalDate prevFromDate = prevToDate.minusDays(days - 1);
    LocalDateTime prevFromDt = prevFromDate.atStartOfDay();
    LocalDateTime prevToDt = prevToDate.plusDays(1).atStartOfDay();

    BigDecimal revenue = nz(appointmentRepository.totalRevenue(fromDt, toDt));
    BigDecimal prevRevenue = nz(appointmentRepository.totalRevenue(prevFromDt, prevToDt));
    BigDecimal cashCaptured = nz(paymentRepository.sumByStatusBetween(PaymentStatus.CAPTURED, fromDt, toDt));
    BigDecimal prevCashCaptured = nz(paymentRepository.sumByStatusBetween(PaymentStatus.CAPTURED, prevFromDt, prevToDt));
    long completedCount = appointmentRepository.countCompletedWithService(fromDt, toDt);
    long prevCompletedCount = appointmentRepository.countCompletedWithService(prevFromDt, prevToDt);

    BigDecimal avgTicket = completedCount > 0
        ? revenue.divide(BigDecimal.valueOf(completedCount), 2, RoundingMode.HALF_UP)
        : BigDecimal.ZERO;
    BigDecimal prevAvgTicket = prevCompletedCount > 0
        ? prevRevenue.divide(BigDecimal.valueOf(prevCompletedCount), 2, RoundingMode.HALF_UP)
        : BigDecimal.ZERO;

    double revenueDeltaPct = percentChange(revenue, prevRevenue);
    double cashDeltaPct = percentChange(cashCaptured, prevCashCaptured);
    double ticketDeltaPct = percentChange(avgTicket, prevAvgTicket);
    double completedDeltaPct = percentChange(BigDecimal.valueOf(completedCount), BigDecimal.valueOf(prevCompletedCount));

    boolean revenueDown = prevRevenue.compareTo(BigDecimal.ZERO) > 0 && revenue.compareTo(prevRevenue) < 0;
    boolean cashDown = prevCashCaptured.compareTo(BigDecimal.ZERO) > 0 && cashCaptured.compareTo(prevCashCaptured) < 0;
    boolean revenueDropAlert = prevRevenue.compareTo(BigDecimal.ZERO) > 0
        && revenue.compareTo(prevRevenue.multiply(new BigDecimal("0.85"))) < 0;

    var byService = appointmentRepository.revenueByService(fromDt, toDt);
    var byDoctor = appointmentRepository.revenueByDoctor(fromDt, toDt);
    var bySpecialty = appointmentRepository.revenueBySpecialty(fromDt, toDt);

    String topService = byService.isEmpty() ? "—" : byService.get(0).getServiceName();
    String topDoctor = byDoctor.isEmpty() ? "—" : byDoctor.get(0).getDoctorName();
    String topSpecialty = bySpecialty.isEmpty() ? "—" : bySpecialty.get(0).getSpecialtyName();

    List<AlertItem> alerts = new ArrayList<>();
    List<ActionItem> actions = new ArrayList<>();
    if (revenueDropAlert) {
      alerts.add(new AlertItem("warning", "Ingresos bajaron más de 15% vs. período anterior."));
      actions.add(new ActionItem("Revisar cancelaciones",
          "Evalúa las cancelaciones del período y ajusta confirmaciones.",
          "/reports/analytics"));
    }
    if (completedCount == 0) {
      alerts.add(new AlertItem("info", "No hay citas completadas con precio en el rango."));
      actions.add(new ActionItem("Validar precios de servicios",
          "No hay ingresos registrados; revisa servicios con precio.",
          "/services"));
    }
    if (revenueDown) {
      actions.add(new ActionItem("Activar campañas de recuperación",
          "Prioriza pacientes inactivos con potencial de retorno.",
          "/patients?missingPhone=true"));
    }
    if (cashCaptured.compareTo(BigDecimal.ZERO) == 0 && revenue.compareTo(BigDecimal.ZERO) > 0) {
      alerts.add(new AlertItem("warning", "Hay ingresos estimados pero no hay cobros registrados."));
      actions.add(new ActionItem("Registrar pagos pendientes",
          "Crea facturas y registra cobros para reflejar ingresos reales.",
          "/billing"));
    }

    model.addAttribute("from", fromDate);
    model.addAttribute("to", toDate);
    model.addAttribute("revenueStats", new RevenueStats(
        revenue,
        avgTicket,
        completedCount,
        revenueDeltaPct,
        ticketDeltaPct,
        completedDeltaPct,
        revenueDown,
        prevRevenue.compareTo(BigDecimal.ZERO) > 0
    ));
    model.addAttribute("cashStats", new CashStats(
        cashCaptured,
        cashDeltaPct,
        cashDown,
        prevCashCaptured.compareTo(BigDecimal.ZERO) > 0
    ));
    model.addAttribute("topService", topService);
    model.addAttribute("topDoctor", topDoctor);
    model.addAttribute("topSpecialty", topSpecialty);
    model.addAttribute("alerts", alerts);
    model.addAttribute("actions", actions);
    model.addAttribute("byService", byService);
    model.addAttribute("byDoctor", byDoctor);
    model.addAttribute("bySpecialty", bySpecialty);
    return "reports/revenue";
  }

  @GetMapping("/reports/benchmark")
  public String benchmarkReport(@RequestParam(required = false) LocalDate from,
                                @RequestParam(required = false) LocalDate to,
                                Model model) {
    LocalDate today = LocalDate.now();
    LocalDate fromDate = (from != null) ? from : today.minusDays(30);
    LocalDate toDate = (to != null) ? to : today;

    metricsSnapshotService.ensureSnapshots(fromDate, toDate);
    var current = metricsDailyRepository.findRange(fromDate, toDate);

    long days = ChronoUnit.DAYS.between(fromDate, toDate) + 1;
    LocalDate prevToDate = fromDate.minusDays(1);
    LocalDate prevFromDate = prevToDate.minusDays(days - 1);

    metricsSnapshotService.ensureSnapshots(prevFromDate, prevToDate);
    var previous = metricsDailyRepository.findRange(prevFromDate, prevToDate);

    long currentTotal = current.stream().mapToLong(MetricsDaily::getTotal).sum();
    long prevTotal = previous.stream().mapToLong(MetricsDaily::getTotal).sum();
    long currentCompleted = current.stream().mapToLong(MetricsDaily::getCompletadas).sum();
    long prevCompleted = previous.stream().mapToLong(MetricsDaily::getCompletadas).sum();
    long currentCancelled = current.stream().mapToLong(MetricsDaily::getCanceladas).sum();
    long prevCancelled = previous.stream().mapToLong(MetricsDaily::getCanceladas).sum();
    long currentNoShow = current.stream().mapToLong(MetricsDaily::getNoAsistio).sum();
    long prevNoShow = previous.stream().mapToLong(MetricsDaily::getNoAsistio).sum();

    double totalDelta = percentChange(BigDecimal.valueOf(currentTotal), BigDecimal.valueOf(prevTotal));
    double completedDelta = percentChange(BigDecimal.valueOf(currentCompleted), BigDecimal.valueOf(prevCompleted));
    double cancelledDelta = percentChange(BigDecimal.valueOf(currentCancelled), BigDecimal.valueOf(prevCancelled));
    double noShowDelta = percentChange(BigDecimal.valueOf(currentNoShow), BigDecimal.valueOf(prevNoShow));

    List<BenchmarkRow> rows = List.of(
        new BenchmarkRow("Total citas", currentTotal, prevTotal, totalDelta),
        new BenchmarkRow("Completadas", currentCompleted, prevCompleted, completedDelta),
        new BenchmarkRow("Canceladas", currentCancelled, prevCancelled, cancelledDelta),
        new BenchmarkRow("No-show", currentNoShow, prevNoShow, noShowDelta)
    );

    model.addAttribute("from", fromDate);
    model.addAttribute("to", toDate);
    model.addAttribute("prevFrom", prevFromDate);
    model.addAttribute("prevTo", prevToDate);
    model.addAttribute("benchRows", rows);
    model.addAttribute("currentSeries", current);
    model.addAttribute("previousSeries", previous);
    model.addAttribute("currentSeriesJs", toSeriesPoints(current));
    model.addAttribute("previousSeriesJs", toSeriesPoints(previous));
    return "reports/benchmark";
  }

  @GetMapping("/reports/benchmark/monthly")
  public String benchmarkMonthly(@RequestParam(required = false) Integer year,
                                 @RequestParam(required = false) Integer month,
                                 Model model) {
    LocalDate today = LocalDate.now();
    int y = (year != null) ? year : today.getYear();
    int m = (month != null) ? month : today.getMonthValue();
    if (m < 1) m = 1;
    if (m > 12) m = 12;

    LocalDate fromDate = LocalDate.of(y, m, 1);
    LocalDate toDate = fromDate.plusMonths(1).minusDays(1);

    LocalDate prevFrom = fromDate.minusMonths(1);
    LocalDate prevTo = prevFrom.plusMonths(1).minusDays(1);

    metricsSnapshotService.ensureSnapshots(fromDate, toDate);
    metricsSnapshotService.ensureSnapshots(prevFrom, prevTo);

    var current = metricsDailyRepository.findRange(fromDate, toDate);
    var previous = metricsDailyRepository.findRange(prevFrom, prevTo);

    long currentTotal = current.stream().mapToLong(MetricsDaily::getTotal).sum();
    long prevTotal = previous.stream().mapToLong(MetricsDaily::getTotal).sum();
    long currentCompleted = current.stream().mapToLong(MetricsDaily::getCompletadas).sum();
    long prevCompleted = previous.stream().mapToLong(MetricsDaily::getCompletadas).sum();
    long currentCancelled = current.stream().mapToLong(MetricsDaily::getCanceladas).sum();
    long prevCancelled = previous.stream().mapToLong(MetricsDaily::getCanceladas).sum();
    long currentNoShow = current.stream().mapToLong(MetricsDaily::getNoAsistio).sum();
    long prevNoShow = previous.stream().mapToLong(MetricsDaily::getNoAsistio).sum();

    double totalDelta = percentChange(BigDecimal.valueOf(currentTotal), BigDecimal.valueOf(prevTotal));
    double completedDelta = percentChange(BigDecimal.valueOf(currentCompleted), BigDecimal.valueOf(prevCompleted));
    double cancelledDelta = percentChange(BigDecimal.valueOf(currentCancelled), BigDecimal.valueOf(prevCancelled));
    double noShowDelta = percentChange(BigDecimal.valueOf(currentNoShow), BigDecimal.valueOf(prevNoShow));

    List<BenchmarkRow> rows = List.of(
        new BenchmarkRow("Total citas", currentTotal, prevTotal, totalDelta),
        new BenchmarkRow("Completadas", currentCompleted, prevCompleted, completedDelta),
        new BenchmarkRow("Canceladas", currentCancelled, prevCancelled, cancelledDelta),
        new BenchmarkRow("No-show", currentNoShow, prevNoShow, noShowDelta)
    );

    model.addAttribute("from", fromDate);
    model.addAttribute("to", toDate);
    model.addAttribute("prevFrom", prevFrom);
    model.addAttribute("prevTo", prevTo);
    model.addAttribute("benchRows", rows);
    model.addAttribute("currentSeries", current);
    model.addAttribute("previousSeries", previous);
    model.addAttribute("currentSeriesJs", toSeriesPoints(current));
    model.addAttribute("previousSeriesJs", toSeriesPoints(previous));
    model.addAttribute("year", y);
    model.addAttribute("month", m);
    return "reports/benchmark-monthly";
  }

  @GetMapping(value = "/reports/revenue/export.csv", produces = "text/csv")
  public void exportRevenueCsv(@RequestParam(required = false) LocalDate from,
                               @RequestParam(required = false) LocalDate to,
                               HttpServletResponse response) throws Exception {
    LocalDate today = LocalDate.now();
    LocalDate fromDate = (from != null) ? from : today.minusDays(30);
    LocalDate toDate = (to != null) ? to : today;

    LocalDateTime fromDt = fromDate.atStartOfDay();
    LocalDateTime toDt = toDate.plusDays(1).atStartOfDay();

    BigDecimal revenue = nz(appointmentRepository.totalRevenue(fromDt, toDt));
    long completedCount = appointmentRepository.countCompletedWithService(fromDt, toDt);
    BigDecimal avgTicket = completedCount > 0
        ? revenue.divide(BigDecimal.valueOf(completedCount), 2, RoundingMode.HALF_UP)
        : BigDecimal.ZERO;

    var byService = appointmentRepository.revenueByService(fromDt, toDt);
    var byDoctor = appointmentRepository.revenueByDoctor(fromDt, toDt);
    var bySpecialty = appointmentRepository.revenueBySpecialty(fromDt, toDt);

    prepareCsvResponse(response, "praecura_revenue_" + fromDate + "_" + toDate + ".csv");
    try (PrintWriter out = csvWriter(response)) {
      writeBom(out);
      out.println(String.join(",", "metric", "value"));
      writeCsvLine(out, "total_revenue", revenue);
      writeCsvLine(out, "completed_count", completedCount);
      writeCsvLine(out, "avg_ticket", avgTicket);
      out.println();
      out.println("by_service");
      out.println(String.join(",", "service_id", "service_name", "revenue", "completed"));
      for (var r : byService) {
        writeCsvLine(out, r.getServiceId(), r.getServiceName(), r.getRevenue(), r.getCompleted());
      }
      out.println();
      out.println("by_doctor");
      out.println(String.join(",", "doctor_id", "doctor_name", "revenue", "completed"));
      for (var r : byDoctor) {
        writeCsvLine(out, r.getDoctorId(), r.getDoctorName(), r.getRevenue(), r.getCompleted());
      }
      out.println();
      out.println("by_specialty");
      out.println(String.join(",", "specialty_id", "specialty_name", "revenue", "completed"));
      for (var r : bySpecialty) {
        writeCsvLine(out, r.getSpecialtyId(), r.getSpecialtyName(), r.getRevenue(), r.getCompleted());
      }
      out.flush();
    }
  }

  @GetMapping(value = "/reports/revenue/export.xlsx", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
  public void exportRevenueXlsx(@RequestParam(required = false) LocalDate from,
                                @RequestParam(required = false) LocalDate to,
                                HttpServletResponse response) throws Exception {
    LocalDate today = LocalDate.now();
    LocalDate fromDate = (from != null) ? from : today.minusDays(30);
    LocalDate toDate = (to != null) ? to : today;

    LocalDateTime fromDt = fromDate.atStartOfDay();
    LocalDateTime toDt = toDate.plusDays(1).atStartOfDay();

    BigDecimal revenue = nz(appointmentRepository.totalRevenue(fromDt, toDt));
    long completedCount = appointmentRepository.countCompletedWithService(fromDt, toDt);
    BigDecimal avgTicket = completedCount > 0
        ? revenue.divide(BigDecimal.valueOf(completedCount), 2, RoundingMode.HALF_UP)
        : BigDecimal.ZERO;

    var byService = appointmentRepository.revenueByService(fromDt, toDt);
    var byDoctor = appointmentRepository.revenueByDoctor(fromDt, toDt);
    var bySpecialty = appointmentRepository.revenueBySpecialty(fromDt, toDt);

    List<SheetData> sheets = new ArrayList<>();
    sheets.add(new SheetData("Resumen",
        List.of("metric", "value"),
        List.of(
            List.of("total_revenue", revenue),
            List.of("completed_count", completedCount),
            List.of("avg_ticket", avgTicket)
        )));

    sheets.add(new SheetData("Servicios",
        List.of("service_id", "service_name", "revenue", "completed"),
        byService.stream()
            .map(r -> List.of(r.getServiceId(), r.getServiceName(), r.getRevenue(), r.getCompleted()))
            .toList()));

    sheets.add(new SheetData("Doctores",
        List.of("doctor_id", "doctor_name", "revenue", "completed"),
        byDoctor.stream()
            .map(r -> List.of(r.getDoctorId(), r.getDoctorName(), r.getRevenue(), r.getCompleted()))
            .toList()));

    sheets.add(new SheetData("Especialidades",
        List.of("specialty_id", "specialty_name", "revenue", "completed"),
        bySpecialty.stream()
            .map(r -> List.of(r.getSpecialtyId(), r.getSpecialtyName(), r.getRevenue(), r.getCompleted()))
            .toList()));

    writeXlsxMulti(response, "praecura_revenue_" + fromDate + "_" + toDate + ".xlsx", sheets);
  }

  @GetMapping(value = "/reports/days/export.csv", produces = "text/csv")
  public void exportDaysCsv(@RequestParam(required = false) LocalDate from,
                            @RequestParam(required = false) LocalDate to,
                            HttpServletResponse response) throws Exception {
    LocalDate today = LocalDate.now();
    LocalDate fromDate = (from != null) ? from : today.minusDays(30);
    LocalDate toDate = (to != null) ? to : today;

    LocalDateTime fromDt = fromDate.atStartOfDay();
    LocalDateTime toDt = toDate.plusDays(1).atStartOfDay();

    var byDay = appointmentRepository.countByDayOfWeek(fromDt, toDt);
    Map<Integer, Long> totals = new HashMap<>();
    for (var row : byDay) {
      if (row.getDow() == null) continue;
      totals.put(row.getDow(), row.getTotal() == null ? 0L : row.getTotal());
    }
    List<String> labels = List.of(
        "Domingo",
        "Lunes",
        "Martes",
        "Miércoles",
        "Jueves",
        "Viernes",
        "Sábado"
    );

    prepareCsvResponse(response, "praecura_report_dias_" + fromDate + "_" + toDate + ".csv");
    try (PrintWriter out = csvWriter(response)) {
      writeBom(out);
      out.println(String.join(",", "day", "total"));
      for (int i = 0; i < labels.size(); i++) {
        writeCsvLine(out, labels.get(i), totals.getOrDefault(i, 0L));
      }
      out.flush();
    }
  }

  @GetMapping(value = "/reports/days/export.xlsx", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
  public void exportDaysXlsx(@RequestParam(required = false) LocalDate from,
                             @RequestParam(required = false) LocalDate to,
                             HttpServletResponse response) throws Exception {
    LocalDate today = LocalDate.now();
    LocalDate fromDate = (from != null) ? from : today.minusDays(30);
    LocalDate toDate = (to != null) ? to : today;

    LocalDateTime fromDt = fromDate.atStartOfDay();
    LocalDateTime toDt = toDate.plusDays(1).atStartOfDay();

    var byDay = appointmentRepository.countByDayOfWeek(fromDt, toDt);
    Map<Integer, Long> totals = new HashMap<>();
    for (var row : byDay) {
      if (row.getDow() == null) continue;
      totals.put(row.getDow(), row.getTotal() == null ? 0L : row.getTotal());
    }
    List<String> labels = List.of(
        "Domingo",
        "Lunes",
        "Martes",
        "Miércoles",
        "Jueves",
        "Viernes",
        "Sábado"
    );

    List<List<Object>> rows = new ArrayList<>();
    for (int i = 0; i < labels.size(); i++) {
      rows.add(List.of(labels.get(i), totals.getOrDefault(i, 0L)));
    }

    writeXlsx(response,
        "praecura_report_dias_" + fromDate + "_" + toDate + ".xlsx",
        "Dias",
        List.of("day", "total"),
        rows);
  }

  @GetMapping("/reports/productivity")
  public String productivityReport(@RequestParam(required = false) LocalDate from,
                                   @RequestParam(required = false) LocalDate to,
                                   @RequestParam(required = false) Integer limit,
                                   Model model) {
    LocalDate today = LocalDate.now();
    LocalDate fromDate = (from != null) ? from : today.minusDays(30);
    LocalDate toDate = (to != null) ? to : today;

    LocalDateTime fromDt = fromDate.atStartOfDay();
    LocalDateTime toDt = toDate.plusDays(1).atStartOfDay();

    var rows = appointmentRepository.productivityByDoctor(fromDt, toDt);
    List<DoctorProductivityView> summary = rows.stream().map(r -> {
      long total = r.getTotal() == null ? 0L : r.getTotal();
      long completed = r.getCompleted() == null ? 0L : r.getCompleted();
      long cancelled = r.getCancelled() == null ? 0L : r.getCancelled();
      long noShow = r.getNoShow() == null ? 0L : r.getNoShow();
      double denom = Math.max(1.0, (double) total);
      double completionRate = (completed / denom) * 100.0;
      double cancelRate = (cancelled / denom) * 100.0;
      double noShowRate = (noShow / denom) * 100.0;
      double avgDuration = (r.getAvgDuration() == null) ? 0.0 : r.getAvgDuration();
      boolean highCancel = cancelRate >= 20.0;
      boolean highNoShow = noShowRate >= 25.0;
      boolean lowCompletion = completionRate <= 60.0;
      return new DoctorProductivityView(
          r.getDoctorId(),
          r.getDoctorName(),
          total,
          completed,
          cancelled,
          noShow,
          completionRate,
          cancelRate,
          noShowRate,
          avgDuration,
          0.0,
          highCancel,
          highNoShow,
          lowCompletion
      );
    }).toList();

    double avgCompletion = summary.isEmpty()
        ? 0.0
        : summary.stream().mapToDouble(DoctorProductivityView::completionRate).average().orElse(0.0);
    List<DoctorProductivityView> enriched = summary.stream().map(r ->
        new DoctorProductivityView(
            r.doctorId(),
            r.doctorName(),
            r.total(),
            r.completed(),
            r.cancelled(),
            r.noShow(),
            r.completionRate(),
            r.cancelRate(),
            r.noShowRate(),
            r.avgDuration(),
            r.completionRate() - avgCompletion,
            r.highCancel(),
            r.highNoShow(),
            r.lowCompletion()
        )
    ).toList();
    int resolvedLimit = resolveLimit(limit, 50);
    List<DoctorProductivityView> tableRows = limitList(enriched, resolvedLimit);
    List<DoctorProductivityView> topRows = limitList(enriched, 8);
    List<DoctorProductivityView> chartRows = limitList(enriched, 12);

    List<AlertItem> alerts = enriched.stream()
        .filter(r -> r.highCancel() || r.highNoShow() || r.lowCompletion())
        .limit(6)
        .map(r -> {
          StringBuilder msg = new StringBuilder();
          msg.append(r.doctorName()).append(": ");
          if (r.highCancel()) msg.append("cancelación alta ");
          if (r.highNoShow()) msg.append("no-show alto ");
          if (r.lowCompletion()) msg.append("eficiencia baja ");
          return new AlertItem("warning", msg.toString().trim());
        })
        .toList();
    List<ActionItem> actions = new ArrayList<>();
    if (!alerts.isEmpty()) {
      actions.add(new ActionItem("Revisar cancelaciones y no-show",
          "Prioriza médicos con alertas para reducir pérdidas.",
          "/reports/analytics"));
    }
    if (avgCompletion < 60.0) {
      actions.add(new ActionItem("Optimizar confirmaciones",
          "Eficiencia promedio baja. Reforzar confirmaciones y recordatorios.",
          "/appointments?status=PROGRAMADA"));
    }
    if (summary.stream().anyMatch(r -> r.noShowRate() >= 30.0)) {
      actions.add(new ActionItem("Priorizar llamadas manuales",
          "Algunos médicos tienen no‑show alto. Requiere contacto directo.",
          "/appointments?status=PROGRAMADA"));
    }

    model.addAttribute("from", fromDate);
    model.addAttribute("to", toDate);
    model.addAttribute("summary", tableRows);
    model.addAttribute("tableRows", tableRows);
    model.addAttribute("topRows", topRows);
    model.addAttribute("chartRows", chartRows);
    model.addAttribute("limit", resolvedLimit);
    model.addAttribute("totalRows", enriched.size());
    model.addAttribute("showingRows", tableRows.size());
    model.addAttribute("limited", enriched.size() > tableRows.size());
    model.addAttribute("avgCompletion", avgCompletion);
    model.addAttribute("alerts", alerts);
    model.addAttribute("actions", actions);
    return "reports/productivity";
  }

  @GetMapping("/reports/productivity/services")
  public String productivityServicesReport(@RequestParam(required = false) LocalDate from,
                                           @RequestParam(required = false) LocalDate to,
                                           @RequestParam(required = false) Integer limit,
                                           Model model) {
    LocalDate today = LocalDate.now();
    LocalDate fromDate = (from != null) ? from : today.minusDays(30);
    LocalDate toDate = (to != null) ? to : today;

    LocalDateTime fromDt = fromDate.atStartOfDay();
    LocalDateTime toDt = toDate.plusDays(1).atStartOfDay();

    var rows = appointmentRepository.productivityByService(fromDt, toDt);
    List<ServiceProductivityView> summary = rows.stream().map(r -> {
      long total = r.getTotal() == null ? 0L : r.getTotal();
      long completed = r.getCompleted() == null ? 0L : r.getCompleted();
      long cancelled = r.getCancelled() == null ? 0L : r.getCancelled();
      long noShow = r.getNoShow() == null ? 0L : r.getNoShow();
      double denom = Math.max(1.0, (double) total);
      double completionRate = (completed / denom) * 100.0;
      double cancelRate = (cancelled / denom) * 100.0;
      double noShowRate = (noShow / denom) * 100.0;
      double avgDuration = (r.getAvgDuration() == null) ? 0.0 : r.getAvgDuration();
      boolean highCancel = cancelRate >= 20.0;
      boolean highNoShow = noShowRate >= 25.0;
      boolean lowCompletion = completionRate <= 60.0;
      return new ServiceProductivityView(
          r.getServiceId(),
          r.getServiceName(),
          total,
          completed,
          cancelled,
          noShow,
          completionRate,
          cancelRate,
          noShowRate,
          avgDuration,
          0.0,
          highCancel,
          highNoShow,
          lowCompletion
      );
    }).toList();

    double avgCompletion = summary.isEmpty()
        ? 0.0
        : summary.stream().mapToDouble(ServiceProductivityView::completionRate).average().orElse(0.0);
    List<ServiceProductivityView> enriched = summary.stream().map(r ->
        new ServiceProductivityView(
            r.serviceId(),
            r.serviceName(),
            r.total(),
            r.completed(),
            r.cancelled(),
            r.noShow(),
            r.completionRate(),
            r.cancelRate(),
            r.noShowRate(),
            r.avgDuration(),
            r.completionRate() - avgCompletion,
            r.highCancel(),
            r.highNoShow(),
            r.lowCompletion()
        )
    ).toList();
    int resolvedLimit = resolveLimit(limit, 50);
    List<ServiceProductivityView> tableRows = limitList(enriched, resolvedLimit);
    List<ServiceProductivityView> topRows = limitList(enriched, 8);
    List<ServiceProductivityView> chartRows = limitList(enriched, 12);

    List<AlertItem> alerts = enriched.stream()
        .filter(r -> r.highCancel() || r.highNoShow() || r.lowCompletion())
        .limit(6)
        .map(r -> {
          StringBuilder msg = new StringBuilder();
          msg.append(r.serviceName()).append(": ");
          if (r.highCancel()) msg.append("cancelación alta ");
          if (r.highNoShow()) msg.append("no-show alto ");
          if (r.lowCompletion()) msg.append("eficiencia baja ");
          return new AlertItem("warning", msg.toString().trim());
        })
        .toList();
    List<ActionItem> actions = new ArrayList<>();
    if (!alerts.isEmpty()) {
      actions.add(new ActionItem("Revisar servicios críticos",
          "Servicios con eficiencia baja o cancelación alta.",
          "/services"));
    }
    if (avgCompletion < 60.0) {
      actions.add(new ActionItem("Refinar protocolos de atención",
          "Optimizar procesos para elevar eficiencia.",
          "/reports/analytics"));
    }
    if (summary.stream().anyMatch(r -> r.cancelRate() >= 25.0)) {
      actions.add(new ActionItem("Revisar política de cancelación",
          "Servicios con cancelación alta; considerar confirmaciones dobles.",
          "/appointments?status=CANCELADA"));
    }

    model.addAttribute("from", fromDate);
    model.addAttribute("to", toDate);
    model.addAttribute("summary", tableRows);
    model.addAttribute("tableRows", tableRows);
    model.addAttribute("topRows", topRows);
    model.addAttribute("chartRows", chartRows);
    model.addAttribute("limit", resolvedLimit);
    model.addAttribute("totalRows", enriched.size());
    model.addAttribute("showingRows", tableRows.size());
    model.addAttribute("limited", enriched.size() > tableRows.size());
    model.addAttribute("avgCompletion", avgCompletion);
    model.addAttribute("alerts", alerts);
    model.addAttribute("actions", actions);
    return "reports/productivity-services";
  }

  @GetMapping(value = "/reports/productivity/export.csv", produces = "text/csv")
  public void exportProductivityCsv(@RequestParam(required = false) LocalDate from,
                                    @RequestParam(required = false) LocalDate to,
                                    HttpServletResponse response) throws Exception {
    LocalDate today = LocalDate.now();
    LocalDate fromDate = (from != null) ? from : today.minusDays(30);
    LocalDate toDate = (to != null) ? to : today;

    LocalDateTime fromDt = fromDate.atStartOfDay();
    LocalDateTime toDt = toDate.plusDays(1).atStartOfDay();

    var rows = appointmentRepository.productivityByDoctor(fromDt, toDt);
    prepareCsvResponse(response, "praecura_report_productividad_" + fromDate + "_" + toDate + ".csv");
    try (PrintWriter out = csvWriter(response)) {
      writeBom(out);
      out.println(String.join(",",
          "doctor_id",
          "doctor_name",
          "total",
          "completed",
          "cancelled",
          "no_show",
          "completion_rate_pct",
          "cancel_rate_pct",
          "no_show_rate_pct",
          "avg_duration_minutes"
      ));
      for (var r : rows) {
        long total = r.getTotal() == null ? 0L : r.getTotal();
        long completed = r.getCompleted() == null ? 0L : r.getCompleted();
        long cancelled = r.getCancelled() == null ? 0L : r.getCancelled();
        long noShow = r.getNoShow() == null ? 0L : r.getNoShow();
        double denom = Math.max(1.0, (double) total);
        double completionRate = (completed / denom) * 100.0;
        double cancelRate = (cancelled / denom) * 100.0;
        double noShowRate = (noShow / denom) * 100.0;
        double avgDuration = (r.getAvgDuration() == null) ? 0.0 : r.getAvgDuration();
        writeCsvLine(out,
            r.getDoctorId(),
            r.getDoctorName(),
            total,
            completed,
            cancelled,
            noShow,
            String.format("%.2f", completionRate),
            String.format("%.2f", cancelRate),
            String.format("%.2f", noShowRate),
            String.format("%.1f", avgDuration)
        );
      }
      out.flush();
    }
  }

  @GetMapping(value = "/reports/productivity/export.xlsx", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
  public void exportProductivityXlsx(@RequestParam(required = false) LocalDate from,
                                     @RequestParam(required = false) LocalDate to,
                                     HttpServletResponse response) throws Exception {
    LocalDate today = LocalDate.now();
    LocalDate fromDate = (from != null) ? from : today.minusDays(30);
    LocalDate toDate = (to != null) ? to : today;

    LocalDateTime fromDt = fromDate.atStartOfDay();
    LocalDateTime toDt = toDate.plusDays(1).atStartOfDay();

    var rows = appointmentRepository.productivityByDoctor(fromDt, toDt);
    List<List<Object>> data = new ArrayList<>();
    for (var r : rows) {
      long total = r.getTotal() == null ? 0L : r.getTotal();
      long completed = r.getCompleted() == null ? 0L : r.getCompleted();
      long cancelled = r.getCancelled() == null ? 0L : r.getCancelled();
      long noShow = r.getNoShow() == null ? 0L : r.getNoShow();
      double denom = Math.max(1.0, (double) total);
      double completionRate = (completed / denom) * 100.0;
      double cancelRate = (cancelled / denom) * 100.0;
      double noShowRate = (noShow / denom) * 100.0;
      double avgDuration = (r.getAvgDuration() == null) ? 0.0 : r.getAvgDuration();
      data.add(List.of(
          r.getDoctorId(),
          r.getDoctorName(),
          total,
          completed,
          cancelled,
          noShow,
          completionRate,
          cancelRate,
          noShowRate,
          avgDuration
      ));
    }
    writeXlsx(response,
        "praecura_report_productividad_" + fromDate + "_" + toDate + ".xlsx",
        "Productividad",
        List.of("doctor_id", "doctor_name", "total", "completed", "cancelled", "no_show",
            "completion_rate_pct", "cancel_rate_pct", "no_show_rate_pct", "avg_duration_minutes"),
        data);
  }

  @GetMapping(value = "/reports/productivity/services/export.csv", produces = "text/csv")
  public void exportProductivityServicesCsv(@RequestParam(required = false) LocalDate from,
                                            @RequestParam(required = false) LocalDate to,
                                            HttpServletResponse response) throws Exception {
    LocalDate today = LocalDate.now();
    LocalDate fromDate = (from != null) ? from : today.minusDays(30);
    LocalDate toDate = (to != null) ? to : today;

    LocalDateTime fromDt = fromDate.atStartOfDay();
    LocalDateTime toDt = toDate.plusDays(1).atStartOfDay();

    var rows = appointmentRepository.productivityByService(fromDt, toDt);
    prepareCsvResponse(response, "praecura_report_productividad_servicios_" + fromDate + "_" + toDate + ".csv");
    try (PrintWriter out = csvWriter(response)) {
      writeBom(out);
      out.println(String.join(",",
          "service_id",
          "service_name",
          "total",
          "completed",
          "cancelled",
          "no_show",
          "completion_rate_pct",
          "cancel_rate_pct",
          "no_show_rate_pct",
          "avg_duration_minutes"
      ));
      for (var r : rows) {
        long total = r.getTotal() == null ? 0L : r.getTotal();
        long completed = r.getCompleted() == null ? 0L : r.getCompleted();
        long cancelled = r.getCancelled() == null ? 0L : r.getCancelled();
        long noShow = r.getNoShow() == null ? 0L : r.getNoShow();
        double denom = Math.max(1.0, (double) total);
        double completionRate = (completed / denom) * 100.0;
        double cancelRate = (cancelled / denom) * 100.0;
        double noShowRate = (noShow / denom) * 100.0;
        double avgDuration = (r.getAvgDuration() == null) ? 0.0 : r.getAvgDuration();
        writeCsvLine(out,
            r.getServiceId(),
            r.getServiceName(),
            total,
            completed,
            cancelled,
            noShow,
            String.format("%.2f", completionRate),
            String.format("%.2f", cancelRate),
            String.format("%.2f", noShowRate),
            String.format("%.1f", avgDuration)
        );
      }
      out.flush();
    }
  }

  @GetMapping(value = "/reports/productivity/services/export.xlsx", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
  public void exportProductivityServicesXlsx(@RequestParam(required = false) LocalDate from,
                                             @RequestParam(required = false) LocalDate to,
                                             HttpServletResponse response) throws Exception {
    LocalDate today = LocalDate.now();
    LocalDate fromDate = (from != null) ? from : today.minusDays(30);
    LocalDate toDate = (to != null) ? to : today;

    LocalDateTime fromDt = fromDate.atStartOfDay();
    LocalDateTime toDt = toDate.plusDays(1).atStartOfDay();

    var rows = appointmentRepository.productivityByService(fromDt, toDt);
    List<List<Object>> data = new ArrayList<>();
    for (var r : rows) {
      long total = r.getTotal() == null ? 0L : r.getTotal();
      long completed = r.getCompleted() == null ? 0L : r.getCompleted();
      long cancelled = r.getCancelled() == null ? 0L : r.getCancelled();
      long noShow = r.getNoShow() == null ? 0L : r.getNoShow();
      double denom = Math.max(1.0, (double) total);
      double completionRate = (completed / denom) * 100.0;
      double cancelRate = (cancelled / denom) * 100.0;
      double noShowRate = (noShow / denom) * 100.0;
      double avgDuration = (r.getAvgDuration() == null) ? 0.0 : r.getAvgDuration();
      data.add(List.of(
          r.getServiceId(),
          r.getServiceName(),
          total,
          completed,
          cancelled,
          noShow,
          completionRate,
          cancelRate,
          noShowRate,
          avgDuration
      ));
    }
    writeXlsx(response,
        "praecura_report_productividad_servicios_" + fromDate + "_" + toDate + ".xlsx",
        "ProductividadServicios",
        List.of("service_id", "service_name", "total", "completed", "cancelled", "no_show",
            "completion_rate_pct", "cancel_rate_pct", "no_show_rate_pct", "avg_duration_minutes"),
        data);
  }

  @GetMapping("/reports/messages")
  public String messages(@RequestParam(required = false) MessageChannel channel,
                         @RequestParam(required = false) MessageStatus status,
                         @RequestParam(required = false) LocalDate from,
                         @RequestParam(required = false) LocalDate to,
                         @RequestParam(defaultValue = "0") int page,
                         @RequestParam(defaultValue = "20") int size,
                         Model model) {
    LocalDate today = LocalDate.now();
    LocalDate fromDate = (from != null) ? from : today.minusDays(30);
    LocalDate toDate = (to != null) ? to : today;

    LocalDateTime fromDt = fromDate.atStartOfDay();
    LocalDateTime toDt = toDate.plusDays(1).atStartOfDay();

    Page<MessageLog> logs = messageLogRepository.search(
        channel,
        status,
        fromDt,
        toDt,
        PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
    );

    model.addAttribute("channel", channel);
    model.addAttribute("status", status);
    model.addAttribute("from", fromDate);
    model.addAttribute("to", toDate);
    model.addAttribute("size", size);
    model.addAttribute("channels", MessageChannel.values());
    model.addAttribute("statuses", MessageStatus.values());
    model.addAttribute("logsPage", logs);
    return "reports/messages";
  }

  @GetMapping("/reports/compliance")
  public String compliance(@RequestParam(required = false) String username,
                           @RequestParam(required = false) LocalDate from,
                           @RequestParam(required = false) LocalDate to,
                           @RequestParam(defaultValue = "0") int page,
                           @RequestParam(defaultValue = "20") int size,
                           Model model) {
    LocalDate today = LocalDate.now();
    LocalDate fromDate = (from != null) ? from : today.minusDays(30);
    LocalDate toDate = (to != null) ? to : today;
    int safeSize = Math.max(1, Math.min(size, 100));

    OperationalComplianceService.ComplianceSnapshot snapshot =
        operationalComplianceService.load(fromDate, toDate, username, page, safeSize);

    model.addAttribute("from", snapshot.fromDate());
    model.addAttribute("to", snapshot.toDate());
    model.addAttribute("username", username);
    model.addAttribute("size", safeSize);
    model.addAttribute("snapshot", snapshot);
    model.addAttribute("findings", snapshot.findings());
    model.addAttribute("criticalEventsPage", snapshot.criticalEventsPage());
    return "reports/compliance";
  }

  @GetMapping(value = "/reports/compliance/export.csv", produces = "text/csv")
  public void exportComplianceCsv(@RequestParam(required = false) String username,
                                  @RequestParam(required = false) LocalDate from,
                                  @RequestParam(required = false) LocalDate to,
                                  HttpServletResponse response) throws Exception {
    LocalDate today = LocalDate.now();
    LocalDate fromDate = (from != null) ? from : today.minusDays(30);
    LocalDate toDate = (to != null) ? to : today;

    OperationalComplianceService.ComplianceSnapshot snapshot =
        operationalComplianceService.load(fromDate, toDate, username, 0, 500, 200);
    List<AuditLog> criticalEvents =
        operationalComplianceService.loadCriticalEventsForExport(fromDate, toDate, username);

    String userPart = (username == null || username.isBlank()) ? "all" : username.trim().replace(" ", "_");
    String filename = "praecura_cumplimiento_" + fromDate + "_" + toDate + "_" + userPart + ".csv";
    prepareCsvResponse(response, filename);

    try (PrintWriter out = csvWriter(response)) {
      writeBom(out);

      writeCsvLine(out, "section", "level", "category", "code", "detail", "reference", "href");
      for (OperationalComplianceService.ComplianceFinding finding : snapshot.findings()) {
        writeCsvLine(out,
            "finding",
            finding.level(),
            finding.category(),
            finding.code(),
            finding.detail(),
            finding.reference(),
            finding.href());
      }

      out.println();
      writeCsvLine(out, "section", "created_at", "username", "action", "entity", "entity_id", "ip_address", "request_id", "detail");
      DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
      for (AuditLog log : criticalEvents) {
        writeCsvLine(out,
            "critical_event",
            log.getCreatedAt() != null ? dtf.format(log.getCreatedAt()) : null,
            log.getUsername(),
            log.getAction(),
            log.getEntity(),
            log.getEntityId(),
            log.getIpAddress(),
            log.getRequestId(),
            log.getDetail());
      }
      out.flush();
    }
  }

  @GetMapping(value = "/reports/compliance/export.xlsx", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
  public void exportComplianceXlsx(@RequestParam(required = false) String username,
                                   @RequestParam(required = false) LocalDate from,
                                   @RequestParam(required = false) LocalDate to,
                                   HttpServletResponse response) throws Exception {
    LocalDate today = LocalDate.now();
    LocalDate fromDate = (from != null) ? from : today.minusDays(30);
    LocalDate toDate = (to != null) ? to : today;

    OperationalComplianceService.ComplianceSnapshot snapshot =
        operationalComplianceService.load(fromDate, toDate, username, 0, 500, 200);
    List<AuditLog> criticalEvents =
        operationalComplianceService.loadCriticalEventsForExport(fromDate, toDate, username);

    DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    List<List<Object>> findingRows = new ArrayList<>();
    for (OperationalComplianceService.ComplianceFinding finding : snapshot.findings()) {
      findingRows.add(List.of(
          finding.level(),
          finding.category(),
          finding.code(),
          finding.detail(),
          finding.reference(),
          finding.href()
      ));
    }

    List<List<Object>> eventRows = new ArrayList<>();
    for (AuditLog log : criticalEvents) {
      eventRows.add(List.of(
          log.getCreatedAt() != null ? dtf.format(log.getCreatedAt()) : "",
          log.getUsername() != null ? log.getUsername() : "",
          log.getAction() != null ? log.getAction() : "",
          log.getEntity() != null ? log.getEntity() : "",
          log.getEntityId() != null ? log.getEntityId() : "",
          log.getIpAddress() != null ? log.getIpAddress() : "",
          log.getRequestId() != null ? log.getRequestId() : "",
          log.getDetail() != null ? log.getDetail() : ""
      ));
    }

    String userPart = (username == null || username.isBlank()) ? "all" : username.trim().replace(" ", "_");
    String filename = "praecura_cumplimiento_" + fromDate + "_" + toDate + "_" + userPart + ".xlsx";
    writeXlsxMulti(response,
        filename,
        List.of(
            new SheetData(
                "Hallazgos",
                List.of("level", "category", "code", "detail", "reference", "href"),
                findingRows
            ),
            new SheetData(
                "EventosCriticos",
                List.of("created_at", "username", "action", "entity", "entity_id", "ip_address", "request_id", "detail"),
                eventRows
            )
        ));
  }

  @GetMapping("/reports/audit")
  public String audit(@RequestParam(required = false) String entity,
                      @RequestParam(required = false) String username,
                      @RequestParam(required = false) LocalDate from,
                      @RequestParam(required = false) LocalDate to,
                      @RequestParam(defaultValue = "0") int page,
                      @RequestParam(defaultValue = "20") int size,
                      Model model) {

    // Avoid untyped NULL parameters in JPQL when filters are optional.
    // The repository query expects non-null bounds.
    LocalDateTime fromDt = (from != null)
        ? from.atStartOfDay()
        : LocalDate.of(1970, 1, 1).atStartOfDay();
    LocalDateTime toDt = (to != null)
        ? to.plusDays(1).atStartOfDay()
        : LocalDate.of(3000, 1, 1).atStartOfDay();

    // Repository query is written to treat empty strings as "no filter".
    // This prevents PostgreSQL from receiving untyped NULLs that can be
    // inferred as BYTEA in certain JPQL constructs (e.g., :p is null OR ...).
    String entityFilter = (entity == null) ? "" : entity.trim();
    String usernameFilter = (username == null) ? "" : username.trim();

    Page<AuditLog> logs = auditLogRepository.search(
        entityFilter,
        usernameFilter,
        fromDt,
        toDt,
        PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
    );

    model.addAttribute("logsPage", logs);
    model.addAttribute("entity", entity);
    model.addAttribute("username", username);
    model.addAttribute("from", from);
    model.addAttribute("to", to);
    model.addAttribute("size", size);
    model.addAttribute("retentionDays", defaultRetentionDays);

    return "reports/audit";
  }

  /**
   * Exporta la auditoría en CSV (UTF-8 con BOM para compatibilidad con Excel).
   * Respetando filtros de entidad/usuario y rango de fechas.
   */
  @GetMapping(value = "/reports/audit/export.csv", produces = "text/csv")
  public void exportAuditCsv(@RequestParam(required = false) String entity,
                             @RequestParam(required = false) String username,
                             @RequestParam(required = false) LocalDate from,
                             @RequestParam(required = false) LocalDate to,
                             HttpServletResponse response) throws Exception {

    LocalDateTime fromDt = (from != null)
        ? from.atStartOfDay()
        : LocalDate.of(1970, 1, 1).atStartOfDay();
    LocalDateTime toDt = (to != null)
        ? to.plusDays(1).atStartOfDay()
        : LocalDate.of(3000, 1, 1).atStartOfDay();

    String entityFilter = (entity == null) ? "" : entity.trim();
    String usernameFilter = (username == null) ? "" : username.trim();

    List<AuditLog> logs = auditLogRepository.searchForExport(entityFilter, usernameFilter, fromDt, toDt);

    String fromPart = (from != null) ? from.toString() : "all";
    String toPart = (to != null) ? to.toString() : "all";
    String filename = "praecura_auditoria_" + fromPart + "_" + toPart + ".csv";

    prepareCsvResponse(response, filename);
    try (java.io.PrintWriter out = csvWriter(response)) {
      writeBom(out);
      writeCsvLine(out, "id", "created_at", "username", "action", "entity", "entity_id",
          "ip_address", "request_id", "user_agent", "detail", "signature");
      DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
      for (AuditLog l : logs) {
        writeCsvLine(out,
            l.getId(),
            l.getCreatedAt() != null ? dtf.format(l.getCreatedAt()) : null,
            l.getUsername(),
            l.getAction(),
            l.getEntity(),
            l.getEntityId(),
            l.getIpAddress(),
            l.getRequestId(),
            l.getUserAgent(),
            l.getDetail(),
            sign(l)
        );
      }
      out.flush();
    }
  }

  @GetMapping(value = "/reports/audit/export.xlsx", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
  public void exportAuditXlsx(@RequestParam(required = false) String entity,
                              @RequestParam(required = false) String username,
                              @RequestParam(required = false) LocalDate from,
                              @RequestParam(required = false) LocalDate to,
                              HttpServletResponse response) throws Exception {

    LocalDateTime fromDt = (from != null)
        ? from.atStartOfDay()
        : LocalDate.of(1970, 1, 1).atStartOfDay();
    LocalDateTime toDt = (to != null)
        ? to.plusDays(1).atStartOfDay()
        : LocalDate.of(3000, 1, 1).atStartOfDay();

    String entityFilter = (entity == null) ? "" : entity.trim();
    String usernameFilter = (username == null) ? "" : username.trim();

    List<AuditLog> logs = auditLogRepository.searchForExport(entityFilter, usernameFilter, fromDt, toDt);

    String fromPart = (from != null) ? from.toString() : "all";
    String toPart = (to != null) ? to.toString() : "all";
    String filename = "praecura_auditoria_" + fromPart + "_" + toPart + ".xlsx";

    DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    List<List<Object>> rows = new ArrayList<>();
    for (AuditLog l : logs) {
      rows.add(List.of(
          l.getId(),
          l.getCreatedAt() != null ? dtf.format(l.getCreatedAt()) : "",
          l.getUsername(),
          l.getAction(),
          l.getEntity(),
          l.getEntityId(),
          l.getIpAddress(),
          l.getRequestId(),
          l.getUserAgent(),
          l.getDetail(),
          sign(l)
      ));
    }

    writeXlsx(response, filename, "Auditoria",
        List.of("id", "created_at", "username", "action", "entity", "entity_id",
            "ip_address", "request_id", "user_agent", "detail", "signature"),
        rows);
  }

  /**
   * Purga registros de auditoría más antiguos que N días.
   * N se puede enviar como parámetro; si no, se usa praecura.audit.retention-days (default 180).
   */
  @PostMapping("/reports/audit/purge")
  public String purgeAudit(@RequestParam(required = false) Integer days,
                           RedirectAttributes ra) {
    int d = (days == null || days < 1) ? defaultRetentionDays : days;
    LocalDateTime cutoff = LocalDateTime.now().minusDays(d);
    long deleted = auditLogRepository.deleteByCreatedAtBefore(cutoff);
    ra.addFlashAttribute("success", "Se purgaron " + deleted + " registros de auditoría (más antiguos que " + d + " días).");
    return "redirect:/reports/audit";
  }

  /**
   * Escapado CSV según convención estándar: si contiene coma, comillas o saltos de línea, se entrecomilla
   * y las comillas internas se duplican.
   */
  private void prepareCsvResponse(HttpServletResponse response, String filename) {
    tabularExportService.prepareCsvResponse(response, filename);
  }

  private java.io.PrintWriter csvWriter(HttpServletResponse response) throws Exception {
    return tabularExportService.csvWriter(response);
  }

  private void writeBom(java.io.PrintWriter out) {
    tabularExportService.writeBom(out);
  }

  private void writeCsvLine(java.io.PrintWriter out, Object... values) {
    tabularExportService.writeCsvLine(out, false, values);
  }

  private void writeXlsx(HttpServletResponse response,
                         String filename,
                         String sheetName,
                         List<String> headers,
                         List<? extends List<?>> rows) throws Exception {
    tabularExportService.writeXlsx(response, filename, sheetName, headers, rows, false);
  }

  private void writeXlsxMulti(HttpServletResponse response,
                              String filename,
                              List<SheetData> sheets) throws Exception {
    List<TabularExportService.SheetData> mapped = sheets.stream()
        .map(s -> new TabularExportService.SheetData(s.name(), s.headers(), s.rows()))
        .toList();
    tabularExportService.writeXlsxMulti(response, filename, mapped, false);
  }

  private static int resolveLimit(Integer limit, int defaultLimit) {
    if (limit == null) return defaultLimit;
    if (limit <= 0) return 0;
    return Math.min(limit, 500);
  }

  private static <T> List<T> limitList(List<T> rows, int limit) {
    if (rows == null || rows.isEmpty()) return rows;
    if (limit <= 0) return rows;
    return rows.size() <= limit ? rows : rows.subList(0, limit);
  }

  private static BigDecimal nz(BigDecimal value) {
    return value == null ? BigDecimal.ZERO : value;
  }

  private static double percentChange(BigDecimal current, BigDecimal previous) {
    if (previous == null || previous.compareTo(BigDecimal.ZERO) == 0) return 0.0;
    BigDecimal delta = current.subtract(previous);
    return delta.multiply(new BigDecimal("100"))
        .divide(previous, 2, RoundingMode.HALF_UP)
        .doubleValue();
  }

  private String sign(AuditLog l) {
    if (auditExportSecret == null || auditExportSecret.isBlank()) return "";
    try {
      String payload = String.join("|",
          String.valueOf(l.getId()),
          String.valueOf(l.getCreatedAt()),
          String.valueOf(l.getUsername()),
          String.valueOf(l.getAction()),
          String.valueOf(l.getEntity()),
          String.valueOf(l.getEntityId()),
          String.valueOf(l.getIpAddress()),
          String.valueOf(l.getRequestId()),
          String.valueOf(l.getUserAgent()),
          String.valueOf(l.getDetail())
      );
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(auditExportSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
      byte[] raw = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
      StringBuilder sb = new StringBuilder();
      for (byte b : raw) {
        sb.append(String.format("%02x", b));
      }
      return sb.toString();
    } catch (Exception ex) {
      return "";
    }
  }

  public record Metrics(
      long total,
      long pendientes,
      long programadas,
      long confirmadas,
      long completadas,
      long canceladas,
      long noAsistio,
      double noShowRatePct,
      long medicosActivos
  ) {}

  public record HourStats(
      long total,
      String peakHour,
      double avgPerHour
  ) {}

  public record DaySummary(
      String label,
      long total
  ) {}

  public record DayStats(
      long total,
      String peakDay,
      double avgPerDay
  ) {}

  public record DoctorProductivityView(
      Long doctorId,
      String doctorName,
      long total,
      long completed,
      long cancelled,
      long noShow,
      double completionRate,
      double cancelRate,
      double noShowRate,
      double avgDuration,
      double completionDelta,
      boolean highCancel,
      boolean highNoShow,
      boolean lowCompletion
  ) {}

  public record ServiceProductivityView(
      Long serviceId,
      String serviceName,
      long total,
      long completed,
      long cancelled,
      long noShow,
      double completionRate,
      double cancelRate,
      double noShowRate,
      double avgDuration,
      double completionDelta,
      boolean highCancel,
      boolean highNoShow,
      boolean lowCompletion
  ) {}

  public record AnalyticsInsights(
      long total,
      double cancelRate,
      double noShowRate,
      double completionRate,
      String topDoctor,
      String topService,
      boolean highCancel,
      boolean highNoShow
  ) {}

  public record SeriesPoint(
      String day,
      long total,
      long canceladas,
      long noAsistio
  ) {}

  public record RevenueStats(
      BigDecimal totalRevenue,
      BigDecimal avgTicket,
      long completedCount,
      double revenueDeltaPct,
      double ticketDeltaPct,
      double completedDeltaPct,
      boolean revenueDown,
      boolean hasPrevious
  ) {}

  public record CashStats(
      BigDecimal captured,
      double capturedDeltaPct,
      boolean capturedDown,
      boolean hasPrevious
  ) {}

  public record BenchmarkRow(
      String label,
      long currentValue,
      long previousValue,
      double deltaPct
  ) {}

  private record SheetData(
      String name,
      List<String> headers,
      List<? extends List<?>> rows
  ) {}

  private List<SeriesPoint> toSeriesPoints(List<MetricsDaily> rows) {
    return rows.stream()
        .map(r -> new SeriesPoint(
            r.getDay() != null ? r.getDay().toString() : "",
            r.getTotal(),
            r.getCanceladas(),
            r.getNoAsistio()
        ))
        .toList();
  }

  public record AlertItem(
      String level,
      String message
  ) {}

  public record ActionItem(
      String title,
      String detail,
      String href
  ) {}
}
