package com.baldwin.praecura.controller;

import com.baldwin.praecura.dto.PatientForm;
import com.baldwin.praecura.entity.AuditLog;
import com.baldwin.praecura.entity.Patient;
import com.baldwin.praecura.entity.Appointment;
import com.baldwin.praecura.service.AppointmentService;
import com.baldwin.praecura.service.PatientService;
import com.baldwin.praecura.repository.AuditLogRepository;
import com.baldwin.praecura.repository.AppointmentRepository;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Controller
@RequestMapping("/patients")
public class PatientController {

  private final PatientService patientService;
  private final AppointmentService appointmentService;
  private final AuditLogRepository auditLogRepository;
  private final AppointmentRepository appointmentRepository;
  private final com.baldwin.praecura.service.ClinicalService clinicalService;
  private final com.baldwin.praecura.service.PatientConsentService patientConsentService;

  private static final List<Integer> PAGE_SIZES = List.of(25, 50, 100);

  public PatientController(PatientService patientService,
                           AppointmentService appointmentService,
                           AuditLogRepository auditLogRepository,
                           AppointmentRepository appointmentRepository,
                           com.baldwin.praecura.service.ClinicalService clinicalService,
                           com.baldwin.praecura.service.PatientConsentService patientConsentService) {
    this.patientService = patientService;
    this.appointmentService = appointmentService;
    this.auditLogRepository = auditLogRepository;
    this.appointmentRepository = appointmentRepository;
    this.clinicalService = clinicalService;
    this.patientConsentService = patientConsentService;
  }

  @GetMapping
  public String list(@RequestParam(required = false) String q,
                     @RequestParam(required = false) String name,
                     @RequestParam(required = false) String cedula,
                     @RequestParam(required = false) String phone,
                     @RequestParam(required = false) String email,
                     @RequestParam(required = false) Boolean missingPhone,
                     @RequestParam(required = false) Boolean missingEmail,
                     @RequestParam(defaultValue = "false") boolean includeInactive,
                     @RequestParam(defaultValue = "0") int page,
                     @RequestParam(defaultValue = "25") int size,
                     Model model) {

    int safeSize = normalizeSize(size);
    int safePage = Math.max(page, 0);

    PageRequest pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.ASC, "fullName"));
    Page<Patient> patients = patientService.searchAdvanced(q, name, cedula, phone, email, missingPhone, missingEmail, includeInactive, pageable);

    model.addAttribute("patients", patients);
    model.addAttribute("q", q);
    model.addAttribute("name", name);
    model.addAttribute("cedula", cedula);
    model.addAttribute("phone", phone);
    model.addAttribute("email", email);
    model.addAttribute("missingPhone", missingPhone);
    model.addAttribute("missingEmail", missingEmail);
    model.addAttribute("includeInactive", includeInactive);

    model.addAttribute("page", safePage);
    model.addAttribute("size", safeSize);
    model.addAttribute("sizeOptions", PAGE_SIZES);

    model.addAttribute("segments", buildSegmentsForPage(patients.getContent()));

    return "patients/list";
  }

  @GetMapping("/new")
  public String newForm(Model model) {
    model.addAttribute("patientForm", new PatientForm());
    return "patients/form";
  }

  @PostMapping
  public String create(@Valid @ModelAttribute("patientForm") PatientForm form, BindingResult bindingResult) {
    if (bindingResult.hasErrors()) {
      return "patients/form";
    }
    try {
      patientService.saveOrUpdate(form);
      return "redirect:/patients";
    } catch (IllegalArgumentException ex) {
      applyFieldError(bindingResult, ex.getMessage());
      return "patients/form";
    }
  }

  @GetMapping("/{id}")
  public String profile(@PathVariable Long id,
                        @RequestParam(defaultValue = "0") int apptPage,
                        @RequestParam(defaultValue = "10") int apptSize,
                        Model model) {
    Patient p = patientService.findById(id);
    PageRequest pageable = PageRequest.of(Math.max(apptPage, 0), Math.min(Math.max(apptSize, 5), 50), Sort.by(Sort.Direction.DESC, "scheduledAt"));
    model.addAttribute("patient", p);
    model.addAttribute("apptHistory", appointmentService.historyForPatient(id, pageable));
    model.addAttribute("apptPage", Math.max(apptPage, 0));
    model.addAttribute("apptSize", Math.min(Math.max(apptSize, 5), 50));
    int timelineLimit = 15;
    java.util.List<Appointment> recent = appointmentService.historyForPatient(id, PageRequest.of(0, timelineLimit, Sort.by(Sort.Direction.DESC, "scheduledAt")))
        .getContent();
    Appointment last = recent.stream().findFirst().orElse(null);
    model.addAttribute("timeline", buildTimeline(p, last, recent, timelineLimit));
    model.addAttribute("segment", buildSegment(p));
    model.addAttribute("clinicalSummary", clinicalService.summary(id));
    model.addAttribute("consentLogs", patientConsentService.recentForPatient(id, 8));
    return "patients/profile";
  }

  @GetMapping("/{id}/edit")
  public String editForm(@PathVariable Long id, Model model) {
    Patient p = patientService.findById(id);
    model.addAttribute("patientForm", toForm(p));
    return "patients/form";
  }

  @PostMapping("/{id}")
  public String update(@PathVariable Long id,
                       @Valid @ModelAttribute("patientForm") PatientForm form,
                       BindingResult bindingResult) {
    if (bindingResult.hasErrors()) {
      return "patients/form";
    }
    try {
      form.setId(id);
      patientService.saveOrUpdate(form);
      return "redirect:/patients/" + id;
    } catch (IllegalArgumentException ex) {
      applyFieldError(bindingResult, ex.getMessage());
      return "patients/form";
    }
  }

  @PostMapping("/{id}/deactivate")
  public String deactivate(@PathVariable Long id,
                           @RequestParam(required = false) String q,
                           @RequestParam(required = false) String name,
                           @RequestParam(required = false) String cedula,
                           @RequestParam(required = false) String phone,
                           @RequestParam(required = false) String email,
                           @RequestParam(required = false) Boolean missingPhone,
                           @RequestParam(required = false) Boolean missingEmail,
                           @RequestParam(defaultValue = "false") boolean includeInactive,
                           @RequestParam(defaultValue = "0") int page,
                           @RequestParam(defaultValue = "25") int size) {

    patientService.deactivate(id);
    return "redirect:" + buildRedirectUrl(q, name, cedula, phone, email, missingPhone, missingEmail, includeInactive, page, size);
  }

  private PatientForm toForm(Patient p) {
    PatientForm f = new PatientForm();
    f.setId(p.getId());

    String full = p.getFullName() == null ? "" : p.getFullName().trim();
    String[] parts = full.split("\\s+");
    if (parts.length >= 2) {
      f.setFirstName(parts[0]);
      f.setLastName(full.substring(parts[0].length()).trim());
    } else {
      f.setFirstName(full);
      f.setLastName("");
    }

    f.setPhone(p.getPhone());
    f.setCedula(p.getCedula());
    f.setEmail(p.getEmail());
    f.setConsentSms(p.isConsentSms());
    f.setConsentEmail(p.isConsentEmail());
    f.setConsentWhatsapp(p.isConsentWhatsapp());
    f.setPreferredChannel(p.getPreferredChannel());
    f.setFlags(p.getFlags());
    f.setNotes(p.getNotes());
    f.setBillingName(p.getBillingName());
    f.setBillingTaxId(p.getBillingTaxId());
    f.setBillingAddress(p.getBillingAddress());
    return f;
  }

  private PatientTimeline buildTimeline(Patient p, Appointment lastAppointment, java.util.List<Appointment> recentAppointments, int limit) {
    java.util.List<TimelineEvent> events = new java.util.ArrayList<>();
    if (lastAppointment != null && lastAppointment.getScheduledAt() != null) {
      events.add(new TimelineEvent(lastAppointment.getScheduledAt(), "Última cita", "Última atención registrada"));
    }
    if (recentAppointments != null) {
      for (Appointment a : recentAppointments) {
        if (a.getScheduledAt() == null) continue;
        String detail = (a.getDoctor() != null ? a.getDoctor().getFullName() : "Médico")
            + " · " + (a.getService() != null ? a.getService().getName() : "Servicio")
            + " · " + (a.getStatus() != null ? a.getStatus().label() : "Estado");
        events.add(new TimelineEvent(a.getScheduledAt(), "Cita", detail));
      }
    }
    var auditPage = auditLogRepository.findByEntityAndEntityIdOrderByCreatedAtDesc("Patient", p.getId(), PageRequest.of(0, limit));
    for (AuditLog a : auditPage.getContent()) {
      events.add(new TimelineEvent(a.getCreatedAt(), "Auditoría: " + a.getAction(), a.getDetail() != null ? a.getDetail() : "Cambio registrado"));
    }
    if (p.getNotes() != null && !p.getNotes().isBlank()) {
      events.add(new TimelineEvent(java.time.LocalDateTime.now(), "Notas", "Tiene notas clínicas/operativas"));
    }
    if (events.isEmpty()) {
      events.add(new TimelineEvent(java.time.LocalDateTime.now(), "Sin eventos", "No hay historial aún"));
    }
    events.sort((a, b) -> b.at.compareTo(a.at));
    boolean hasMore = (recentAppointments != null && recentAppointments.size() >= limit) || auditPage.getTotalElements() > limit;
    return new PatientTimeline(events, hasMore, limit);
  }

  private java.util.Map<Long, PatientSegment> buildSegmentsForPage(java.util.List<Patient> patients) {
    java.util.Map<Long, PatientSegment> out = new java.util.HashMap<>();
    if (patients == null || patients.isEmpty()) return out;

    java.util.List<Long> ids = patients.stream().map(Patient::getId).toList();
    java.time.LocalDate today = java.time.LocalDate.now();
    java.time.LocalDate from = today.minusDays(180);
    java.time.LocalDateTime fromDt = from.atStartOfDay();
    java.time.LocalDateTime toDt = today.plusDays(1).atStartOfDay();

    java.util.Map<Long, Long> totalMap = new java.util.HashMap<>();
    java.util.Map<Long, Long> cancelMap = new java.util.HashMap<>();
    java.util.Map<Long, Long> noShowMap = new java.util.HashMap<>();

    for (var r : appointmentRepository.countByPatients(ids, fromDt, toDt)) {
      totalMap.put(r.getId(), r.getTotal());
    }
    for (var r : appointmentRepository.countByPatientsAndStatus(ids, com.baldwin.praecura.entity.AppointmentStatus.CANCELADA, fromDt, toDt)) {
      cancelMap.put(r.getId(), r.getTotal());
    }
    for (var r : appointmentRepository.countByPatientsAndStatus(ids, com.baldwin.praecura.entity.AppointmentStatus.NO_ASISTIO, fromDt, toDt)) {
      noShowMap.put(r.getId(), r.getTotal());
    }

    for (Patient p : patients) {
      long total = totalMap.getOrDefault(p.getId(), 0L);
      long canceladas = cancelMap.getOrDefault(p.getId(), 0L);
      long noAsistio = noShowMap.getOrDefault(p.getId(), 0L);
      out.put(p.getId(), segmentFromCounts(p, total, canceladas, noAsistio));
    }
    return out;
  }

  private PatientSegment buildSegment(Patient p) {
    java.time.LocalDate today = java.time.LocalDate.now();
    java.time.LocalDate from = today.minusDays(180);
    java.time.LocalDateTime fromDt = from.atStartOfDay();
    java.time.LocalDateTime toDt = today.plusDays(1).atStartOfDay();

    long total = appointmentRepository.countByPatientAndRange(p.getId(), fromDt, toDt);
    long canceladas = appointmentRepository.countByPatientAndStatus(p.getId(), com.baldwin.praecura.entity.AppointmentStatus.CANCELADA, fromDt, toDt);
    long noAsistio = appointmentRepository.countByPatientAndStatus(p.getId(), com.baldwin.praecura.entity.AppointmentStatus.NO_ASISTIO, fromDt, toDt);

    long denom = Math.max(1, total - canceladas);
    double noShowRate = ((double) noAsistio / (double) denom) * 100.0;

    boolean missingPhone = p.getPhone() == null || p.getPhone().isBlank();
    boolean missingEmail = p.getEmail() == null || p.getEmail().isBlank();

    boolean chronicNoShow = (total >= 3 && noShowRate >= 35.0);
    boolean inactive = total == 0;

    return new PatientSegment(total, noShowRate, missingPhone, missingEmail, chronicNoShow, inactive);
  }

  private PatientSegment segmentFromCounts(Patient p, long total, long canceladas, long noAsistio) {
    long denom = Math.max(1, total - canceladas);
    double noShowRate = ((double) noAsistio / (double) denom) * 100.0;
    boolean missingPhone = p.getPhone() == null || p.getPhone().isBlank();
    boolean missingEmail = p.getEmail() == null || p.getEmail().isBlank();
    boolean chronicNoShow = (total >= 3 && noShowRate >= 35.0);
    boolean inactive = total == 0;
    return new PatientSegment(total, noShowRate, missingPhone, missingEmail, chronicNoShow, inactive);
  }

  private void applyFieldError(BindingResult bindingResult, String message) {
    String msg = message == null ? "No se pudo completar la operación" : message;
    String low = msg.toLowerCase();
    if (low.contains("cédula") || low.contains("cedula")) {
      bindingResult.rejectValue("cedula", "dup", msg);
    } else if (low.contains("correo") || low.contains("email")) {
      bindingResult.rejectValue("email", "dup", msg);
    } else if (low.contains("teléfono") || low.contains("telefono") || low.contains("contacto")) {
      bindingResult.rejectValue("phone", "required", msg);
    } else {
      bindingResult.reject("global", msg);
    }
  }

  private int normalizeSize(int requested) {
    if (PAGE_SIZES.contains(requested)) return requested;
    return 25;
  }

  private String buildRedirectUrl(String q,
                                  String name,
                                  String cedula,
                                  String phone,
                                  String email,
                                  Boolean missingPhone,
                                  Boolean missingEmail,
                                  boolean includeInactive,
                                  int page,
                                  int size) {
    StringBuilder sb = new StringBuilder("/patients?page=").append(Math.max(page, 0)).append("&size=").append(normalizeSize(size));
    append(sb, "q", q);
    append(sb, "name", name);
    append(sb, "cedula", cedula);
    append(sb, "phone", phone);
    append(sb, "email", email);
    if (missingPhone != null && missingPhone) sb.append("&missingPhone=true");
    if (missingEmail != null && missingEmail) sb.append("&missingEmail=true");
    if (includeInactive) sb.append("&includeInactive=true");
    return sb.toString();
  }

  private void append(StringBuilder sb, String key, String value) {
    if (value == null || value.trim().isEmpty()) return;
    sb.append("&").append(key).append("=").append(URLEncoder.encode(value.trim(), StandardCharsets.UTF_8));
  }

  public record PatientTimeline(java.util.List<TimelineEvent> events, boolean hasMore, int limit) {}
  public record TimelineEvent(java.time.LocalDateTime at, String title, String detail) {}
  public record PatientSegment(long total, double noShowRate, boolean missingPhone, boolean missingEmail, boolean chronicNoShow, boolean inactive) {}
}
