package com.baldwin.praecura.controller;

import com.baldwin.praecura.entity.Appointment;
import com.baldwin.praecura.entity.AppointmentStatus;
import com.baldwin.praecura.service.ClinicSiteService;
import com.baldwin.praecura.service.DoctorService;
import com.baldwin.praecura.service.AppointmentService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.DayOfWeek;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/agenda")
public class AgendaController {

  private final AppointmentService appointmentService;
  private final DoctorService doctorService;
  private final ClinicSiteService clinicSiteService;

  public AgendaController(AppointmentService appointmentService,
                          DoctorService doctorService,
                          ClinicSiteService clinicSiteService) {
    this.appointmentService = appointmentService;
    this.doctorService = doctorService;
    this.clinicSiteService = clinicSiteService;
  }

  @GetMapping
  public String day(@RequestParam(value = "date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                    @RequestParam(value = "doctorId", required = false) Long doctorId,
                    @RequestParam(value = "siteId", required = false) Long siteId,
                    @RequestParam(value = "status", required = false) AppointmentStatus status,
                    Model model) {

    LocalDate day = (date == null) ? LocalDate.now() : date;
    List<Appointment> appts = appointmentService.agendaForDay(day, doctorId, siteId, status);

    model.addAttribute("day", day);
    model.addAttribute("doctorId", doctorId);
    model.addAttribute("siteId", siteId);
    model.addAttribute("status", status);
    model.addAttribute("appointments", appts);
    model.addAttribute("doctors", doctorService.listActiveForSelect());
    model.addAttribute("sites", clinicSiteService.listActiveForSelect());
    model.addAttribute("statuses", AppointmentStatus.values());

    return "agenda/day";
  }

  @GetMapping("/week")
  public String week(@RequestParam(value = "date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                     @RequestParam(value = "doctorId", required = false) Long doctorId,
                     @RequestParam(value = "siteId", required = false) Long siteId,
                     @RequestParam(value = "status", required = false) AppointmentStatus status,
                     @RequestParam(value = "limit", defaultValue = "25") int limit,
                     Model model) {

    LocalDate base = (date == null) ? LocalDate.now() : date;

    // Semana ISO (Lunes–Domingo)
    int dow = base.getDayOfWeek().getValue(); // 1..7
    LocalDate start = base.minusDays(dow - DayOfWeek.MONDAY.getValue());
    LocalDate end = start.plusDays(6);

    List<Appointment> appts = appointmentService.agendaForRange(start, end, doctorId, siteId, status);

    List<LocalDate> days = new ArrayList<>();
    Map<LocalDate, List<Appointment>> byDay = new HashMap<>();
    for (int i = 0; i < 7; i++) {
      LocalDate d = start.plusDays(i);
      days.add(d);
      byDay.put(d, new ArrayList<>());
    }
    for (Appointment a : appts) {
      if (a.getScheduledAt() == null) continue;
      LocalDate d = a.getScheduledAt().toLocalDate();
      if (byDay.containsKey(d)) {
        byDay.get(d).add(a);
      }
    }

    int safeLimit = (limit == 50 || limit == 100) ? limit : 25;

    // Ordenar y limitar por día (evita que la vista semanal se rompa con cientos de citas por día)
    Map<LocalDate, Integer> dayCounts = new HashMap<>();
    Map<LocalDate, Integer> dayOverflow = new HashMap<>();
    Map<LocalDate, List<Appointment>> byDayLimited = new HashMap<>();

    for (LocalDate d : days) {
      List<Appointment> list = byDay.get(d);
      list.sort(Comparator.comparing(Appointment::getScheduledAt));
      dayCounts.put(d, list.size());
      int overflow = Math.max(0, list.size() - safeLimit);
      dayOverflow.put(d, overflow);
      if (overflow > 0) {
        byDayLimited.put(d, list.subList(0, safeLimit));
      } else {
        byDayLimited.put(d, list);
      }
    }


    model.addAttribute("baseDate", base);
    model.addAttribute("base", base);
    model.addAttribute("start", start);
    model.addAttribute("end", end);
    model.addAttribute("doctorId", doctorId);
    model.addAttribute("siteId", siteId);
    model.addAttribute("status", status);
    model.addAttribute("days", days);
    model.addAttribute("byDay", byDayLimited);
    model.addAttribute("dayCounts", dayCounts);
    model.addAttribute("dayOverflow", dayOverflow);
    model.addAttribute("limit", safeLimit);
    model.addAttribute("limitOptions", List.of(25, 50, 100));
    model.addAttribute("doctors", doctorService.listActiveForSelect());
    model.addAttribute("sites", clinicSiteService.listActiveForSelect());
    model.addAttribute("statuses", AppointmentStatus.values());

    return "agenda/week";
  }

  @GetMapping("/week-grid")
  public String weekGrid(@RequestParam(value = "date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                         @RequestParam(value = "doctorId", required = false) Long doctorId,
                         @RequestParam(value = "siteId", required = false) Long siteId,
                         @RequestParam(value = "status", required = false) AppointmentStatus status,
                         Model model) {

    LocalDate base = (date == null) ? LocalDate.now() : date;
    int dow = base.getDayOfWeek().getValue();
    LocalDate start = base.minusDays(dow - DayOfWeek.MONDAY.getValue());

    List<LocalDate> days = new ArrayList<>();
    for (int i = 0; i < 7; i++) days.add(start.plusDays(i));

    List<Appointment> appts = appointmentService.agendaForRange(start, start.plusDays(6), doctorId, siteId, status);

    // Build slots per day (08:00–18:00, 30-min steps)
    List<LocalTime> slots = new ArrayList<>();
    LocalTime t = LocalTime.of(8, 0);
    LocalTime end = LocalTime.of(18, 0);
    while (!t.isAfter(end)) {
      slots.add(t);
      t = t.plusMinutes(30);
    }

    Map<LocalDate, Map<LocalTime, List<Appointment>>> grid = new HashMap<>();
    for (LocalDate d : days) {
      Map<LocalTime, List<Appointment>> map = new HashMap<>();
      for (LocalTime s : slots) map.put(s, new ArrayList<>());
      grid.put(d, map);
    }

    for (Appointment a : appts) {
      if (a.getScheduledAt() == null) continue;
      LocalDate d = a.getScheduledAt().toLocalDate();
      LocalTime s = a.getScheduledAt().toLocalTime().withMinute(a.getScheduledAt().getMinute() < 30 ? 0 : 30);
      if (grid.containsKey(d) && grid.get(d).containsKey(s)) {
        grid.get(d).get(s).add(a);
      }
    }

    model.addAttribute("base", base);
    model.addAttribute("start", start);
    model.addAttribute("days", days);
    model.addAttribute("slots", slots);
    model.addAttribute("grid", grid);
    model.addAttribute("doctorId", doctorId);
    model.addAttribute("siteId", siteId);
    model.addAttribute("status", status);
    model.addAttribute("doctors", doctorService.listActiveForSelect());
    model.addAttribute("sites", clinicSiteService.listActiveForSelect());
    model.addAttribute("statuses", AppointmentStatus.values());

    return "agenda/week-grid";
  }

  @GetMapping(value = "/export.csv", produces = "text/csv")
  public void exportCsv(@RequestParam(value = "date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                        @RequestParam(value = "doctorId", required = false) Long doctorId,
                        @RequestParam(value = "siteId", required = false) Long siteId,
                        @RequestParam(value = "status", required = false) AppointmentStatus status,
                        HttpServletResponse response) throws Exception {

    LocalDate day = (date == null) ? LocalDate.now() : date;

    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
    response.setContentType("text/csv; charset=UTF-8");
    response.setHeader("Content-Disposition", "attachment; filename=agenda-" + day + ".csv");

    var out = response.getWriter();
    out.write("Fecha,Hora,Paciente,Médico,Sede,Recurso,Servicio,Estado,Duración (min),Motivo\n");
    appointmentService.forEachAgendaForDay(day, doctorId, siteId, status, a -> {
      String fecha = (a.getScheduledAt() != null) ? a.getScheduledAt().toLocalDate().toString() : "";
      String hora = (a.getScheduledAt() != null) ? a.getScheduledAt().toLocalTime().toString() : "";

      out.append(csv(fecha)).append(',')
          .append(csv(hora)).append(',')
          .append(csv(a.getPatient() != null ? a.getPatient().getFullName() : "")).append(',')
          .append(csv(a.getDoctor() != null ? a.getDoctor().getFullName() : "")).append(',')
          .append(csv(a.getSite() != null ? a.getSite().getName() : "")).append(',')
          .append(csv(a.getResource() != null ? a.getResource().getName() : "")).append(',')
          .append(csv(a.getService() != null ? a.getService().getName() : "")).append(',')
          .append(csv(a.getStatus() != null ? a.getStatus().label() : "")).append(',')
          .append(String.valueOf(a.getDurationMinutes())).append(',')
          .append(csv(a.getReason() != null ? a.getReason() : ""))
          .append('\n');
    });
    out.flush();
  }

  @GetMapping(value = "/export.ics", produces = "text/calendar")
  public void exportIcs(@RequestParam(value = "date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                        @RequestParam(value = "doctorId", required = false) Long doctorId,
                        @RequestParam(value = "siteId", required = false) Long siteId,
                        @RequestParam(value = "status", required = false) AppointmentStatus status,
                        HttpServletResponse response) throws Exception {

    LocalDate day = (date == null) ? LocalDate.now() : date;

    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
    response.setContentType("text/calendar; charset=UTF-8");
    response.setHeader("Content-Disposition", "attachment; filename=agenda-" + day + ".ics");

    var out = response.getWriter();
    out.write("BEGIN:VCALENDAR\n");
    out.write("VERSION:2.0\n");
    out.write("PRODID:-//PraeCura//Agenda//ES\n");
    out.write("CALSCALE:GREGORIAN\n");

    appointmentService.forEachAgendaForDay(day, doctorId, siteId, status, a -> {
      if (a.getScheduledAt() == null) return;

      LocalDateTime start = a.getScheduledAt();
      LocalDateTime end = start.plusMinutes(a.getDurationMinutes());
      String uid = "praecura-" + a.getId() + "@local";

      String summary = (a.getPatient() != null ? a.getPatient().getFullName() : "Paciente") +
          " - " +
          (a.getDoctor() != null ? a.getDoctor().getFullName() : "Médico");

      String desc = "Estado: " + (a.getStatus() != null ? a.getStatus().label() : "") +
          (a.getSite() != null ? "\nSede: " + a.getSite().getName() : "") +
          (a.getResource() != null ? "\nRecurso: " + a.getResource().getName() : "") +
          (a.getService() != null ? "\nServicio: " + a.getService().getName() : "") +
          (a.getReason() != null ? "\nMotivo: " + a.getReason() : "");

      out.append("BEGIN:VEVENT\n");
      out.append("UID:").append(uid).append('\n');
      out.append("DTSTAMP:").append(icsTs(LocalDateTime.now())).append('\n');
      out.append("DTSTART:").append(icsTs(start)).append('\n');
      out.append("DTEND:").append(icsTs(end)).append('\n');
      out.append("SUMMARY:").append(icsEscape(summary)).append('\n');
      out.append("DESCRIPTION:").append(icsEscape(desc)).append('\n');
      out.append("END:VEVENT\n");
    });

    out.write("END:VCALENDAR\n");
    out.flush();
  }



@GetMapping(value = "/export-range.csv", produces = "text/csv")
public void exportCsvRange(
    @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
    @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
    @RequestParam(value = "doctorId", required = false) Long doctorId,
    @RequestParam(value = "siteId", required = false) Long siteId,
    @RequestParam(value = "status", required = false) AppointmentStatus status,
    HttpServletResponse response
) throws Exception {

  LocalDate from = (fromDate != null) ? fromDate : LocalDate.now();
  LocalDate to = (toDate != null) ? toDate : from;

  response.setCharacterEncoding(StandardCharsets.UTF_8.name());
  response.setContentType("text/csv; charset=UTF-8");
  response.setHeader("Content-Disposition", "attachment; filename=" + exportName("agenda", from, to, doctorId, "csv"));

  var out = response.getWriter();
  out.write("Fecha,Hora,Paciente,Médico,Sede,Recurso,Servicio,Estado,Duración (min),Motivo\n");
  appointmentService.forEachAgendaForRange(from, to, doctorId, siteId, status, a -> {
    String fecha = (a.getScheduledAt() != null) ? a.getScheduledAt().toLocalDate().toString() : "";
    String hora = (a.getScheduledAt() != null) ? a.getScheduledAt().toLocalTime().toString() : "";

    out.append(csv(fecha)).append(',')
        .append(csv(hora)).append(',')
        .append(csv(a.getPatient() != null ? a.getPatient().getFullName() : "")).append(',')
        .append(csv(a.getDoctor() != null ? a.getDoctor().getFullName() : "")).append(',')
        .append(csv(a.getSite() != null ? a.getSite().getName() : "")).append(',')
        .append(csv(a.getResource() != null ? a.getResource().getName() : "")).append(',')
        .append(csv(a.getService() != null ? a.getService().getName() : "")).append(',')
        .append(csv(a.getStatus() != null ? a.getStatus().label() : "")).append(',')
        .append(String.valueOf(a.getDurationMinutes())).append(',')
        .append(csv(a.getReason() != null ? a.getReason() : ""))
        .append('\n');
  });
  out.flush();
}

@GetMapping(value = "/export-range.ics", produces = "text/calendar")
public void exportIcsRange(
    @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
    @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
    @RequestParam(value = "doctorId", required = false) Long doctorId,
    @RequestParam(value = "siteId", required = false) Long siteId,
    @RequestParam(value = "status", required = false) AppointmentStatus status,
    HttpServletResponse response
) throws Exception {

  LocalDate from = (fromDate != null) ? fromDate : LocalDate.now();
  LocalDate to = (toDate != null) ? toDate : from;

  response.setCharacterEncoding(StandardCharsets.UTF_8.name());
  response.setContentType("text/calendar; charset=UTF-8");
  response.setHeader("Content-Disposition", "attachment; filename=" + exportName("agenda", from, to, doctorId, "ics"));

  var out = response.getWriter();
  out.write("BEGIN:VCALENDAR\n");
  out.write("VERSION:2.0\n");
  out.write("PRODID:-//PraeCura//Agenda//ES\n");
  out.write("CALSCALE:GREGORIAN\n");
  appointmentService.forEachAgendaForRange(from, to, doctorId, siteId, status, a -> {
    if (a.getScheduledAt() == null) return;

    LocalDateTime start = a.getScheduledAt();
    LocalDateTime end = start.plusMinutes(a.getDurationMinutes());
    String uid = "praecura-" + a.getId() + "@local";

    String summary = (a.getPatient() != null ? a.getPatient().getFullName() : "Paciente") +
        " - " +
        (a.getDoctor() != null ? a.getDoctor().getFullName() : "Médico");

    String desc = "Estado: " + (a.getStatus() != null ? a.getStatus().label() : "") +
        (a.getSite() != null ? "\nSede: " + a.getSite().getName() : "") +
        (a.getResource() != null ? "\nRecurso: " + a.getResource().getName() : "") +
        (a.getService() != null ? "\nServicio: " + a.getService().getName() : "") +
        (a.getReason() != null ? "\nMotivo: " + a.getReason() : "");

    out.append("BEGIN:VEVENT\n");
    out.append("UID:").append(uid).append('\n');
    out.append("DTSTAMP:").append(icsTs(LocalDateTime.now())).append('\n');
    out.append("DTSTART:").append(icsTs(start)).append('\n');
    out.append("DTEND:").append(icsTs(end)).append('\n');
    out.append("SUMMARY:").append(icsEscape(summary)).append('\n');
    out.append("DESCRIPTION:").append(icsEscape(desc)).append('\n');
    out.append("END:VEVENT\n");
  });

  out.write("END:VCALENDAR\n");
  out.flush();
}

private static String exportName(String prefix, LocalDate from, LocalDate to, Long doctorId, String ext) {
  String range = from.equals(to) ? from.toString() : (from + "_to_" + to);
  String who = (doctorId != null) ? ("-doctor-" + doctorId) : "";
  return prefix + "-" + range + who + "." + ext;
}

  private static String csv(String v) {
    if (v == null) return "";
    String s = v.replace("\r", " ").replace("\n", " ");
    // CSV escaping: wrap in quotes if needed, and escape quotes by doubling them.
    boolean needQuotes = s.contains(",") || s.contains("\"") || s.contains(";");
    s = s.replace("\"", "\"\"");
    return needQuotes ? ("\"" + s + "\"") : s;
  }

  private static final DateTimeFormatter ICS_FMT = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss");

  private static String icsTs(LocalDateTime dt) {
    // Sin zona horaria (floating time) para compatibilidad básica.
    return dt.format(ICS_FMT);
  }

  private static String icsEscape(String v) {
    if (v == null) return "";
    return v.replace("\\", "\\\\")
        .replace(";", "\\;")
        .replace(",", "\\,")
        .replace("\n", "\\n");
  }
}
