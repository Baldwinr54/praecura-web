package com.baldwin.praecura.controller;

import com.baldwin.praecura.entity.ClinicalEncounterStatus;
import com.baldwin.praecura.entity.ClinicalOrderPriority;
import com.baldwin.praecura.entity.ClinicalOrderStatus;
import com.baldwin.praecura.entity.ClinicalOrderType;
import com.baldwin.praecura.repository.AppointmentRepository;
import com.baldwin.praecura.repository.PatientRepository;
import com.baldwin.praecura.service.ClinicalWorkflowService;
import java.math.BigDecimal;
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
@RequestMapping("/clinical-workflow")
public class ClinicalWorkflowController {

  private final ClinicalWorkflowService clinicalWorkflowService;
  private final PatientRepository patientRepository;
  private final AppointmentRepository appointmentRepository;

  public ClinicalWorkflowController(ClinicalWorkflowService clinicalWorkflowService,
                                    PatientRepository patientRepository,
                                    AppointmentRepository appointmentRepository) {
    this.clinicalWorkflowService = clinicalWorkflowService;
    this.patientRepository = patientRepository;
    this.appointmentRepository = appointmentRepository;
  }

  @GetMapping
  public String index(@RequestParam(required = false) Long patientId,
                      @RequestParam(required = false) Long encounterId,
                      Model model) {
    var encounters = clinicalWorkflowService.listEncounters(patientId);

    Long activeEncounterId = encounterId;
    if (activeEncounterId == null && !encounters.isEmpty()) {
      activeEncounterId = encounters.get(0).getId();
    }

    model.addAttribute("patients", patientRepository.findAll());
    model.addAttribute("appointments", appointmentRepository.findAll());
    model.addAttribute("encounters", encounters);
    model.addAttribute("selectedPatientId", patientId);
    model.addAttribute("activeEncounterId", activeEncounterId);
    model.addAttribute("diagnoses", clinicalWorkflowService.listDiagnoses(activeEncounterId));
    model.addAttribute("orders", clinicalWorkflowService.listOrders(activeEncounterId));
    model.addAttribute("encounterStatuses", ClinicalEncounterStatus.values());
    model.addAttribute("orderTypes", ClinicalOrderType.values());
    model.addAttribute("orderPriorities", ClinicalOrderPriority.values());
    model.addAttribute("orderStatuses", ClinicalOrderStatus.values());
    return "clinical-workflow/index";
  }

  @PostMapping("/encounters")
  public String createEncounter(@RequestParam Long patientId,
                                @RequestParam(required = false) Long appointmentId,
                                @RequestParam(required = false) String chiefComplaint,
                                @RequestParam(required = false) String subjective,
                                @RequestParam(required = false) String objective,
                                @RequestParam(required = false) String assessment,
                                @RequestParam(required = false) String plan,
                                Authentication authentication,
                                RedirectAttributes ra,
                                @RequestHeader(value = "Referer", required = false) String referer) {
    try {
      var encounter = clinicalWorkflowService.createEncounter(patientId, appointmentId, chiefComplaint, subjective, objective, assessment, plan, authentication);
      ra.addFlashAttribute("success", "Encuentro clínico SOAP creado.");
      return "redirect:/clinical-workflow?encounterId=" + encounter.getId();
    } catch (IllegalArgumentException ex) {
      ra.addFlashAttribute("error", ex.getMessage());
      return redirectBack(referer, "/clinical-workflow");
    }
  }

  @PostMapping("/encounters/{id}/soap")
  public String updateSoap(@PathVariable Long id,
                           @RequestParam(required = false) String chiefComplaint,
                           @RequestParam(required = false) String subjective,
                           @RequestParam(required = false) String objective,
                           @RequestParam(required = false) String assessment,
                           @RequestParam(required = false) String plan,
                           @RequestParam(required = false) ClinicalEncounterStatus status,
                           Authentication authentication,
                           RedirectAttributes ra,
                           @RequestHeader(value = "Referer", required = false) String referer) {
    try {
      clinicalWorkflowService.updateSoap(id, chiefComplaint, subjective, objective, assessment, plan, status, authentication);
      ra.addFlashAttribute("success", "SOAP actualizado.");
    } catch (IllegalArgumentException ex) {
      ra.addFlashAttribute("error", ex.getMessage());
    }
    return redirectBack(referer, "/clinical-workflow?encounterId=" + id);
  }

  @PostMapping("/encounters/{id}/diagnoses")
  public String addDiagnosis(@PathVariable Long id,
                             @RequestParam(required = false) String icd10Code,
                             @RequestParam String description,
                             @RequestParam(defaultValue = "false") boolean primaryDiagnosis,
                             @RequestParam(required = false) String notes,
                             RedirectAttributes ra,
                             @RequestHeader(value = "Referer", required = false) String referer) {
    try {
      clinicalWorkflowService.addDiagnosis(id, icd10Code, description, primaryDiagnosis, notes);
      ra.addFlashAttribute("success", "Diagnóstico agregado.");
    } catch (IllegalArgumentException ex) {
      ra.addFlashAttribute("error", ex.getMessage());
    }
    return redirectBack(referer, "/clinical-workflow?encounterId=" + id);
  }

  @PostMapping("/encounters/{id}/orders")
  public String addOrder(@PathVariable Long id,
                         @RequestParam ClinicalOrderType orderType,
                         @RequestParam(required = false) ClinicalOrderPriority priority,
                         @RequestParam String orderName,
                         @RequestParam(required = false) String instructions,
                         @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm") LocalDateTime dueAt,
                         @RequestParam(required = false) BigDecimal costEstimate,
                         RedirectAttributes ra,
                         @RequestHeader(value = "Referer", required = false) String referer) {
    try {
      clinicalWorkflowService.addOrder(id, orderType, priority, orderName, instructions, dueAt, costEstimate);
      ra.addFlashAttribute("success", "Orden clínica agregada.");
    } catch (IllegalArgumentException ex) {
      ra.addFlashAttribute("error", ex.getMessage());
    }
    return redirectBack(referer, "/clinical-workflow?encounterId=" + id);
  }

  @PostMapping("/orders/{id}/status")
  public String updateOrderStatus(@PathVariable Long id,
                                  @RequestParam ClinicalOrderStatus status,
                                  @RequestParam(required = false) String resultSummary,
                                  RedirectAttributes ra,
                                  @RequestHeader(value = "Referer", required = false) String referer) {
    try {
      clinicalWorkflowService.updateOrderStatus(id, status, resultSummary);
      ra.addFlashAttribute("success", "Estado de orden actualizado.");
    } catch (IllegalArgumentException ex) {
      ra.addFlashAttribute("error", ex.getMessage());
    }
    return redirectBack(referer, "/clinical-workflow");
  }

  private String redirectBack(String referer, String fallback) {
    if (referer == null || referer.isBlank()) return "redirect:" + fallback;
    if (referer.startsWith("/") && !referer.startsWith("//")) return "redirect:" + referer;
    return "redirect:" + fallback;
  }
}
