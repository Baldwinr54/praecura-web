package com.baldwin.praecura.controller;

import com.baldwin.praecura.entity.InsuranceAuthorizationStatus;
import com.baldwin.praecura.entity.InsuranceClaimStatus;
import com.baldwin.praecura.repository.InvoiceRepository;
import com.baldwin.praecura.repository.PatientRepository;
import com.baldwin.praecura.service.InsuranceService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/insurance")
public class InsuranceController {

  private final InsuranceService insuranceService;
  private final PatientRepository patientRepository;
  private final InvoiceRepository invoiceRepository;

  public InsuranceController(InsuranceService insuranceService,
                             PatientRepository patientRepository,
                             InvoiceRepository invoiceRepository) {
    this.insuranceService = insuranceService;
    this.patientRepository = patientRepository;
    this.invoiceRepository = invoiceRepository;
  }

  @GetMapping
  public String index(Model model) {
    model.addAttribute("payers", insuranceService.listPayers());
    model.addAttribute("plans", insuranceService.listPlans());
    model.addAttribute("coverages", insuranceService.listCoverages());
    model.addAttribute("authorizations", insuranceService.listAuthorizations());
    model.addAttribute("claims", insuranceService.listClaims());
    model.addAttribute("patients", patientRepository.findAll());
    model.addAttribute("invoices", invoiceRepository.findAll());
    model.addAttribute("authorizationStatuses", InsuranceAuthorizationStatus.values());
    model.addAttribute("claimStatuses", InsuranceClaimStatus.values());
    return "insurance/index";
  }

  @PostMapping("/payers")
  public String createPayer(@RequestParam String code,
                            @RequestParam String name,
                            @RequestParam(required = false) String rnc,
                            @RequestParam(required = false) String contactPhone,
                            @RequestParam(required = false) String contactEmail,
                            @RequestParam(required = false) String notes,
                            RedirectAttributes ra,
                            @RequestHeader(value = "Referer", required = false) String referer) {
    try {
      insuranceService.createPayer(code, name, rnc, contactPhone, contactEmail, notes);
      ra.addFlashAttribute("success", "ARS registrada correctamente.");
    } catch (IllegalArgumentException ex) {
      ra.addFlashAttribute("error", ex.getMessage());
    }
    return redirectBack(referer, "/insurance");
  }

  @PostMapping("/plans")
  public String createPlan(@RequestParam Long payerId,
                           @RequestParam String planCode,
                           @RequestParam String name,
                           @RequestParam(required = false) BigDecimal coveragePercent,
                           @RequestParam(required = false) BigDecimal copayPercent,
                           @RequestParam(required = false) BigDecimal deductibleAmount,
                           @RequestParam(defaultValue = "false") boolean requiresAuthorization,
                           RedirectAttributes ra,
                           @RequestHeader(value = "Referer", required = false) String referer) {
    try {
      insuranceService.createPlan(payerId, planCode, name, coveragePercent, copayPercent, deductibleAmount, requiresAuthorization);
      ra.addFlashAttribute("success", "Plan ARS registrado.");
    } catch (IllegalArgumentException ex) {
      ra.addFlashAttribute("error", ex.getMessage());
    }
    return redirectBack(referer, "/insurance");
  }

  @PostMapping("/coverages")
  public String createCoverage(@RequestParam Long patientId,
                               @RequestParam Long planId,
                               @RequestParam(required = false) String policyNumber,
                               @RequestParam(required = false) String affiliateNumber,
                               @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate validFrom,
                               @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate validTo,
                               RedirectAttributes ra,
                               @RequestHeader(value = "Referer", required = false) String referer) {
    try {
      insuranceService.createCoverage(patientId, planId, policyNumber, affiliateNumber, validFrom, validTo);
      ra.addFlashAttribute("success", "Cobertura asignada al paciente.");
    } catch (IllegalArgumentException ex) {
      ra.addFlashAttribute("error", ex.getMessage());
    }
    return redirectBack(referer, "/insurance");
  }

  @PostMapping("/authorizations")
  public String createAuthorization(@RequestParam Long coverageId,
                                    @RequestParam(required = false) Long appointmentId,
                                    @RequestParam(required = false) String authorizationNumber,
                                    @RequestParam(required = false) BigDecimal requestedAmount,
                                    @RequestParam(required = false) BigDecimal approvedAmount,
                                    @RequestParam(required = false) InsuranceAuthorizationStatus status,
                                    @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm") LocalDateTime expiresAt,
                                    @RequestParam(required = false) String notes,
                                    RedirectAttributes ra,
                                    @RequestHeader(value = "Referer", required = false) String referer) {
    try {
      insuranceService.createAuthorization(coverageId, appointmentId, authorizationNumber, requestedAmount, approvedAmount, status, expiresAt, notes);
      ra.addFlashAttribute("success", "Autorización ARS registrada.");
    } catch (IllegalArgumentException ex) {
      ra.addFlashAttribute("error", ex.getMessage());
    }
    return redirectBack(referer, "/insurance");
  }

  @PostMapping("/claims")
  public String createClaim(@RequestParam Long invoiceId,
                            @RequestParam(required = false) Long coverageId,
                            @RequestParam(required = false) Long authorizationId,
                            @RequestParam(required = false) InsuranceClaimStatus status,
                            @RequestParam(required = false) BigDecimal claimedAmount,
                            @RequestParam(required = false) BigDecimal approvedAmount,
                            @RequestParam(required = false) BigDecimal deniedAmount,
                            @RequestParam(required = false) String denialReason,
                            @RequestParam(required = false) String notes,
                            RedirectAttributes ra,
                            @RequestHeader(value = "Referer", required = false) String referer) {
    try {
      insuranceService.createOrUpdateClaim(invoiceId, coverageId, authorizationId, status, claimedAmount, approvedAmount, deniedAmount, denialReason, notes);
      ra.addFlashAttribute("success", "Reclamo ARS guardado.");
    } catch (IllegalArgumentException ex) {
      ra.addFlashAttribute("error", ex.getMessage());
    }
    return redirectBack(referer, "/insurance");
  }

  private String redirectBack(String referer, String fallback) {
    if (referer == null || referer.isBlank()) {
      return "redirect:" + fallback;
    }
    if (referer.startsWith("/") && !referer.startsWith("//")) {
      return "redirect:" + referer;
    }
    return "redirect:" + fallback;
  }
}
