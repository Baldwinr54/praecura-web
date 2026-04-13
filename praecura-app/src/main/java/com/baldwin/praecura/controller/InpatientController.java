package com.baldwin.praecura.controller;

import com.baldwin.praecura.entity.AdmissionStatus;
import com.baldwin.praecura.entity.SurgeryStatus;
import com.baldwin.praecura.repository.ClinicResourceRepository;
import com.baldwin.praecura.repository.ClinicSiteRepository;
import com.baldwin.praecura.repository.DoctorRepository;
import com.baldwin.praecura.repository.PatientRepository;
import com.baldwin.praecura.service.InpatientService;
import java.time.LocalDateTime;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/inpatient")
public class InpatientController {

  private final InpatientService inpatientService;
  private final ClinicSiteRepository clinicSiteRepository;
  private final PatientRepository patientRepository;
  private final DoctorRepository doctorRepository;
  private final ClinicResourceRepository clinicResourceRepository;

  public InpatientController(InpatientService inpatientService,
                             ClinicSiteRepository clinicSiteRepository,
                             PatientRepository patientRepository,
                             DoctorRepository doctorRepository,
                             ClinicResourceRepository clinicResourceRepository) {
    this.inpatientService = inpatientService;
    this.clinicSiteRepository = clinicSiteRepository;
    this.patientRepository = patientRepository;
    this.doctorRepository = doctorRepository;
    this.clinicResourceRepository = clinicResourceRepository;
  }

  @GetMapping
  public String index(@RequestParam(required = false) Long admissionId, Model model) {
    model.addAttribute("beds", inpatientService.listBeds());
    model.addAttribute("admissions", inpatientService.listAdmissions());
    model.addAttribute("surgeries", inpatientService.listSurgeries());
    model.addAttribute("nursingNotes", inpatientService.listNursingNotes(admissionId));
    model.addAttribute("selectedAdmissionId", admissionId);

    model.addAttribute("sites", clinicSiteRepository.findAll());
    model.addAttribute("patients", patientRepository.findAll());
    model.addAttribute("doctors", doctorRepository.findAll());
    model.addAttribute("resources", clinicResourceRepository.findAll());
    model.addAttribute("admissionStatuses", AdmissionStatus.values());
    model.addAttribute("surgeryStatuses", SurgeryStatus.values());
    return "inpatient/index";
  }

  @PostMapping("/beds")
  public String createBed(@RequestParam Long siteId,
                          @RequestParam String code,
                          @RequestParam(required = false) String ward,
                          @RequestParam(required = false) String bedType,
                          @RequestParam(required = false) String notes,
                          RedirectAttributes ra,
                          @RequestHeader(value = "Referer", required = false) String referer) {
    try {
      inpatientService.createBed(siteId, code, ward, bedType, notes);
      ra.addFlashAttribute("success", "Cama registrada.");
    } catch (IllegalArgumentException ex) {
      ra.addFlashAttribute("error", ex.getMessage());
    }
    return redirectBack(referer, "/inpatient");
  }

  @PostMapping("/admissions")
  public String admit(@RequestParam Long patientId,
                      @RequestParam(required = false) Long bedId,
                      @RequestParam(required = false) Long doctorId,
                      @RequestParam(required = false) String admissionReason,
                      @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm") LocalDateTime expectedDischargeAt,
                      Authentication authentication,
                      RedirectAttributes ra,
                      @RequestHeader(value = "Referer", required = false) String referer) {
    try {
      var admission = inpatientService.admitPatient(patientId, bedId, doctorId, admissionReason, expectedDischargeAt, authentication);
      ra.addFlashAttribute("success", "Paciente ingresado.");
      return "redirect:/inpatient?admissionId=" + admission.getId();
    } catch (IllegalArgumentException ex) {
      ra.addFlashAttribute("error", ex.getMessage());
      return redirectBack(referer, "/inpatient");
    }
  }

  @PostMapping("/admissions/{id}/status")
  public String updateAdmissionStatus(@PathVariable Long id,
                                      @RequestParam AdmissionStatus status,
                                      @RequestParam(required = false) String dischargeSummary,
                                      Authentication authentication,
                                      RedirectAttributes ra,
                                      @RequestHeader(value = "Referer", required = false) String referer) {
    try {
      inpatientService.updateAdmissionStatus(id, status, dischargeSummary, authentication);
      ra.addFlashAttribute("success", "Estado de admisión actualizado.");
    } catch (IllegalArgumentException ex) {
      ra.addFlashAttribute("error", ex.getMessage());
    }
    return redirectBack(referer, "/inpatient?admissionId=" + id);
  }

  @PostMapping("/surgeries")
  public String createSurgery(@RequestParam Long patientId,
                              @RequestParam(required = false) Long admissionId,
                              @RequestParam(required = false) Long doctorId,
                              @RequestParam(required = false) Long siteId,
                              @RequestParam(required = false) Long resourceId,
                              @RequestParam String procedureName,
                              @RequestParam(required = false) String anesthesiaType,
                              @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm") LocalDateTime scheduledAt,
                              @RequestParam(required = false) Integer estimatedMinutes,
                              @RequestParam(required = false) String notes,
                              Authentication authentication,
                              RedirectAttributes ra,
                              @RequestHeader(value = "Referer", required = false) String referer) {
    try {
      inpatientService.scheduleSurgery(patientId, admissionId, doctorId, siteId, resourceId, procedureName,
          anesthesiaType, scheduledAt, estimatedMinutes, notes, authentication);
      ra.addFlashAttribute("success", "Cirugía programada.");
    } catch (IllegalArgumentException ex) {
      ra.addFlashAttribute("error", ex.getMessage());
    }
    return redirectBack(referer, "/inpatient");
  }

  @PostMapping("/surgeries/{id}/status")
  public String updateSurgeryStatus(@PathVariable Long id,
                                    @RequestParam SurgeryStatus status,
                                    @RequestParam(required = false) String notes,
                                    RedirectAttributes ra,
                                    @RequestHeader(value = "Referer", required = false) String referer) {
    try {
      inpatientService.updateSurgeryStatus(id, status, notes);
      ra.addFlashAttribute("success", "Estado de cirugía actualizado.");
    } catch (IllegalArgumentException ex) {
      ra.addFlashAttribute("error", ex.getMessage());
    }
    return redirectBack(referer, "/inpatient");
  }

  @PostMapping("/nursing-notes")
  public String addNursingNote(@RequestParam Long admissionId,
                               @RequestParam(required = false) String shift,
                               @RequestParam String notes,
                               @RequestParam(required = false) String vitalsSnapshot,
                               @RequestParam(required = false) String medicationAdministered,
                               @RequestParam(defaultValue = "false") boolean adverseEvent,
                               Authentication authentication,
                               RedirectAttributes ra,
                               @RequestHeader(value = "Referer", required = false) String referer) {
    try {
      inpatientService.addNursingNote(admissionId, shift, notes, vitalsSnapshot, medicationAdministered, adverseEvent, authentication);
      ra.addFlashAttribute("success", "Nota de enfermería registrada.");
      return "redirect:/inpatient?admissionId=" + admissionId;
    } catch (IllegalArgumentException ex) {
      ra.addFlashAttribute("error", ex.getMessage());
      return redirectBack(referer, "/inpatient");
    }
  }

  private String redirectBack(String referer, String fallback) {
    if (referer == null || referer.isBlank()) return "redirect:" + fallback;
    if (referer.startsWith("/") && !referer.startsWith("//")) return "redirect:" + referer;
    return "redirect:" + fallback;
  }
}
