package com.baldwin.praecura.controller;

import com.baldwin.praecura.dto.AppointmentForm;
import com.baldwin.praecura.dto.AppointmentRescheduleForm;
import com.baldwin.praecura.entity.Appointment;
import com.baldwin.praecura.entity.AppointmentStatus;
import com.baldwin.praecura.entity.TriageLevel;
import com.baldwin.praecura.service.AppointmentService;
import com.baldwin.praecura.service.ClinicResourceService;
import com.baldwin.praecura.service.ClinicSiteService;
import com.baldwin.praecura.service.DoctorService;
import com.baldwin.praecura.service.MedicalServiceService;
import com.baldwin.praecura.service.MessageService;
import com.baldwin.praecura.service.PatientService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.net.URI;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Controller
@RequestMapping("/appointments")
public class AppointmentController {

  private final AppointmentService appointmentService;
  private final PatientService patientService;
  private final DoctorService doctorService;
  private final MedicalServiceService medicalServiceService;
  private final ClinicSiteService clinicSiteService;
  private final ClinicResourceService clinicResourceService;
  private final MessageService messageService;

  public AppointmentController(AppointmentService appointmentService,
                               PatientService patientService,
                               DoctorService doctorService,
                               MedicalServiceService medicalServiceService,
                               ClinicSiteService clinicSiteService,
                               ClinicResourceService clinicResourceService,
                               MessageService messageService) {
    this.appointmentService = appointmentService;
    this.patientService = patientService;
    this.doctorService = doctorService;
    this.medicalServiceService = medicalServiceService;
    this.clinicSiteService = clinicSiteService;
    this.clinicResourceService = clinicResourceService;
    this.messageService = messageService;
  }

  @GetMapping
  public String list(@RequestParam(value = "q", required = false) String q,
                     @RequestParam(value = "doctorId", required = false) Long doctorId,
                     @RequestParam(value = "siteId", required = false) Long siteId,
                     @RequestParam(value = "status", required = false) AppointmentStatus status,
                     @RequestParam(value = "date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                     @RequestParam(value = "page", required = false, defaultValue = "0") int page,
                     @RequestParam(value = "size", required = false, defaultValue = "10") int size,
                     Model model) {

    LocalDateTime fromDt = null;
    LocalDateTime toDt = null;
    if (date != null) {
      fromDt = date.atStartOfDay();
      toDt = fromDt.plusDays(1);
    }

    PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.max(5, Math.min(size, 50)),
        Sort.by(Sort.Direction.DESC, "scheduledAt"));
    Page<Appointment> appts = appointmentService.search(q, doctorId, siteId, status, fromDt, toDt, pageable);

    model.addAttribute("appointmentsPage", appts);
    model.addAttribute("q", q == null ? "" : q);
    model.addAttribute("doctorId", doctorId);
    model.addAttribute("siteId", siteId);
    model.addAttribute("status", status);
    model.addAttribute("date", date);
    model.addAttribute("size", pageable.getPageSize());

    model.addAttribute("doctors", doctorService.listActiveForSelect());
    model.addAttribute("sites", clinicSiteService.listActiveForSelect());
    model.addAttribute("statuses", AppointmentStatus.values());

    return "appointments/list";
  }

  @GetMapping("/new")
  public String newForm(@RequestParam(required = false) Long patientId,
                        @RequestParam(required = false) Long doctorId,
                        Model model) {
    AppointmentForm form = new AppointmentForm();
    form.setStatus(AppointmentStatus.PROGRAMADA);
    if (patientId != null) {
      form.setPatientId(patientId);
    }
    if (doctorId != null) {
      form.setDoctorId(doctorId);
    }
    model.addAttribute("form", form);
    loadFormLists(model);
    return "appointments/form";
  }
  @GetMapping("/{id}")
  public String view(@PathVariable Long id) {
    return "redirect:/appointments/" + id + "/edit";
  }



  @GetMapping("/{id}/edit")
  public String edit(@PathVariable Long id, Model model) {
    Appointment a = appointmentService.get(id);
    AppointmentForm f = new AppointmentForm();
    f.setId(a.getId());
    f.setPatientId(a.getPatient().getId());
    f.setDoctorId(a.getDoctor().getId());
    f.setServiceId(a.getService() != null ? a.getService().getId() : null);
    f.setSiteId(a.getSite() != null ? a.getSite().getId() : null);
    f.setResourceId(a.getResource() != null ? a.getResource().getId() : null);
    f.setScheduledAt(a.getScheduledAt());
    f.setReason(a.getReason());
    f.setDurationMinutes(a.getDurationMinutes());

    // Si la duración coincide con la del servicio, mantenemos auto=true; si no, preservamos la duración personalizada.
    if (a.getService() != null) {
      f.setDurationAuto(a.getDurationMinutes() == a.getService().getDurationMinutes());
    } else {
      f.setDurationAuto(false);
    }

    f.setNotes(a.getNotes());
    f.setTriageLevel(a.getTriageLevel());
    f.setTriageNotes(a.getTriageNotes());
    f.setCheckedInAt(a.getCheckedInAt());
    f.setStartedAt(a.getStartedAt());
    f.setCompletedAt(a.getCompletedAt());
    f.setStatus(a.getStatus());

    model.addAttribute("form", f);
    model.addAttribute("editMode", true);
    loadFormLists(model);
    return "appointments/form";
  }
  @PostMapping("/{id}")
  public String update(@PathVariable Long id,
      @Valid @ModelAttribute("form") AppointmentForm form,
      BindingResult binding,
      Model model,
      RedirectAttributes ra) {
    form.setId(id);
    // save(form, br, ra, model)
    return save(form, binding, ra, model);
  }



  @PostMapping
  public String save(@Valid @ModelAttribute("form") AppointmentForm form,
                     BindingResult br,
                     RedirectAttributes ra,
                     Model model) {
    if (br.hasErrors()) {
      model.addAttribute("editMode", form.getId() != null);
      loadFormLists(model);
      return "appointments/form";
    }

    try {
      appointmentService.saveOrUpdate(form);
      ra.addFlashAttribute("success", form.getId() == null ? "Cita creada" : "Cita actualizada");
      return "redirect:/appointments";
    } catch (IllegalArgumentException ex) {
      if (ex.getMessage() != null && ex.getMessage().toLowerCase().contains("contacto")) {
        br.rejectValue("patientId", "contact.required", ex.getMessage());
      } else if (ex.getMessage() != null && ex.getMessage().toLowerCase().contains("servicio")) {
        br.rejectValue("serviceId", "service.required", ex.getMessage());
      } else if (ex.getMessage() != null && ex.getMessage().toLowerCase().contains("sede")) {
        br.rejectValue("siteId", "site.required", ex.getMessage());
      } else if (ex.getMessage() != null && ex.getMessage().toLowerCase().contains("recurso")) {
        br.rejectValue("resourceId", "resource.required", ex.getMessage());
      } else if (ex.getMessage() != null && ex.getMessage().toLowerCase().contains("duración")) {
        br.rejectValue("durationMinutes", "duration.invalid", ex.getMessage());
      } else if (ex.getMessage() != null && ex.getMessage().toLowerCase().contains("horario")) {
        br.rejectValue("scheduledAt", "schedule.invalid", ex.getMessage());
      } else {
        br.reject("error.appointment", ex.getMessage());
      }
      model.addAttribute("editMode", form.getId() != null);
      loadFormLists(model);
      return "appointments/form";
    }
  }

  @GetMapping("/{id}/reschedule")
  public String reschedule(@PathVariable Long id, Model model) {
    Appointment a = appointmentService.get(id);
    AppointmentRescheduleForm f = new AppointmentRescheduleForm();
    f.setId(a.getId());
    f.setDoctorId(a.getDoctor().getId());
    f.setScheduledAt(a.getScheduledAt());
    model.addAttribute("form", f);
    model.addAttribute("appointment", a);
    model.addAttribute("doctors", doctorService.listActiveForSelect());
    return "appointments/reschedule";
  }

  @PostMapping("/{id}/reschedule")
  public String rescheduleSave(@PathVariable Long id,
                               @Valid @ModelAttribute("form") AppointmentRescheduleForm form,
                               BindingResult br,
                               RedirectAttributes ra,
                               Model model) {
    if (br.hasErrors()) {
      model.addAttribute("appointment", appointmentService.get(id));
      model.addAttribute("doctors", doctorService.listActiveForSelect());
      return "appointments/reschedule";
    }
    try {
      appointmentService.reschedule(id, form.getDoctorId(), form.getScheduledAt());
      ra.addFlashAttribute("success", "Cita reprogramada");
      return "redirect:/appointments";
    } catch (IllegalArgumentException ex) {
      br.reject("error.reschedule", ex.getMessage());
      model.addAttribute("appointment", appointmentService.get(id));
      model.addAttribute("doctors", doctorService.listActiveForSelect());
      return "appointments/reschedule";
    }
  }

  @PostMapping("/{id}/cancel")
  public String cancel(@PathVariable Long id,
                       RedirectAttributes ra,
                       @RequestHeader(value = "Referer", required = false) String referer) {
    appointmentService.cancel(id);
    ra.addFlashAttribute("success", "Cita cancelada");
    return redirectBack(referer, "/appointments");
  }

  @PostMapping("/{id}/confirm")
  public String confirm(@PathVariable Long id,
                        RedirectAttributes ra,
                        @RequestHeader(value = "Referer", required = false) String referer) {
    appointmentService.confirm(id);
    ra.addFlashAttribute("success", "Cita confirmada");
    return redirectBack(referer, "/appointments");
  }

  @PostMapping("/{id}/complete")
  public String complete(@PathVariable Long id,
                         RedirectAttributes ra,
                         @RequestHeader(value = "Referer", required = false) String referer) {
    appointmentService.complete(id);
    ra.addFlashAttribute("success", "Cita completada");
    return redirectBack(referer, "/appointments");
  }

  @PostMapping("/{id}/no-show")
  public String noShow(@PathVariable Long id,
                       RedirectAttributes ra,
                       @RequestHeader(value = "Referer", required = false) String referer) {
    appointmentService.markNoShow(id);
    ra.addFlashAttribute("success", "Cita marcada como no-show");
    return redirectBack(referer, "/appointments");
  }

  @PostMapping("/{id}/remind")
  public String remind(@PathVariable Long id,
                       RedirectAttributes ra,
                       @RequestHeader(value = "Referer", required = false) String referer) {
    try {
      var result = messageService.sendAppointmentReminder(id);
      if (result.status() == com.baldwin.praecura.entity.MessageStatus.SENT) {
        ra.addFlashAttribute("success", "Recordatorio enviado por " + result.channel());
      } else if (result.status() == com.baldwin.praecura.entity.MessageStatus.SKIPPED) {
        ra.addFlashAttribute("warning", "Recordatorio registrado. Mensajería deshabilitada.");
      } else {
        ra.addFlashAttribute("error", "No se pudo enviar el recordatorio.");
      }
    } catch (IllegalArgumentException ex) {
      ra.addFlashAttribute("error", ex.getMessage());
    }
    return redirectBack(referer, "/appointments");
  }

  @PostMapping("/{id}/delete")
  public String delete(@PathVariable Long id,
                       RedirectAttributes ra,
                       @RequestHeader(value = "Referer", required = false) String referer) {
    appointmentService.delete(id);
    ra.addFlashAttribute("success", "Cita archivada");
    return redirectBack(referer, "/appointments");
  }

  private void loadFormLists(Model model) {
    model.addAttribute("patients", patientService.listActiveForSelect());
    model.addAttribute("doctors", doctorService.listActiveForSelect());
    model.addAttribute("services", medicalServiceService.listActiveForSelect());
    model.addAttribute("sites", clinicSiteService.listActiveForSelect());
    model.addAttribute("resources", clinicResourceService.listActiveForSelect());
    model.addAttribute("triageLevels", TriageLevel.values());
    model.addAttribute("statuses", AppointmentStatus.values());
  }

  /**
   * Redirige de vuelta a la pantalla previa (ej. Agenda día/semana) usando Referer.
   * Para evitar open-redirect, solo se conserva path + query del referer.
   */
  private String redirectBack(String referer, String fallbackPath) {
    if (fallbackPath == null || fallbackPath.isBlank()) fallbackPath = "/";

    if (referer == null || referer.isBlank()) {
      return "redirect:" + fallbackPath;
    }

    // Si ya es relativo, úsalo directo
    if (referer.startsWith("/")) {
      return "redirect:" + referer;
    }

    try {
      URI uri = URI.create(referer);
      String path = uri.getPath();
      if (path == null || path.isBlank() || !path.startsWith("/")) {
        return "redirect:" + fallbackPath;
      }
      String query = uri.getQuery();
      return "redirect:" + path + (query != null && !query.isBlank() ? "?" + query : "");
    } catch (Exception ex) {
      return "redirect:" + fallbackPath;
    }
  }
}
