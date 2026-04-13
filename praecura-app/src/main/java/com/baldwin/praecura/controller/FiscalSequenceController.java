package com.baldwin.praecura.controller;

import com.baldwin.praecura.entity.FiscalSequence;
import com.baldwin.praecura.security.SecurityRoleUtils;
import com.baldwin.praecura.service.AuditService;
import com.baldwin.praecura.service.BillingSupervisorAccessService;
import com.baldwin.praecura.service.FiscalSequenceService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/billing/fiscal")
public class FiscalSequenceController {

  private final FiscalSequenceService fiscalSequenceService;
  private final BillingSupervisorAccessService billingSupervisorAccessService;
  private final AuditService auditService;

  public FiscalSequenceController(FiscalSequenceService fiscalSequenceService,
                                  BillingSupervisorAccessService billingSupervisorAccessService,
                                  AuditService auditService) {
    this.fiscalSequenceService = fiscalSequenceService;
    this.billingSupervisorAccessService = billingSupervisorAccessService;
    this.auditService = auditService;
  }

  @GetMapping
  public String index(Model model) {
    model.addAttribute("sequences", fiscalSequenceService.listAll());
    return "billing/fiscal";
  }

  @PostMapping
  public String create(@RequestParam String typeCode,
                       @RequestParam(required = false) String description,
                       @RequestParam long startNumber,
                       @RequestParam(required = false) Long endNumber,
                       @RequestParam(required = false) Integer numberLength,
                       @RequestParam(required = false) String expiresAt,
                       Authentication authentication,
                       RedirectAttributes ra,
                       HttpServletRequest request) {
    if (!isSupervisor(authentication)) {
      ra.addFlashAttribute("error", "No tienes permiso para crear secuencias fiscales. Requiere supervisor financiero.");
      return "redirect:/billing/fiscal";
    }
    try {
      FiscalSequence seq = new FiscalSequence();
      seq.setTypeCode(typeCode);
      seq.setDescription(description);
      seq.setStartNumber(startNumber);
      seq.setNextNumber(startNumber);
      seq.setEndNumber(endNumber);
      if (numberLength != null) seq.setNumberLength(numberLength);
      if (expiresAt != null && !expiresAt.isBlank()) {
        seq.setExpiresAt(java.time.LocalDate.parse(expiresAt));
      }
      seq.setCreatedBy(request.getRemoteUser());
      seq.setUpdatedBy(request.getRemoteUser());
      FiscalSequence created = fiscalSequenceService.create(seq);
      auditService.log("FISCAL_SEQUENCE_CREATED",
          "FiscalSequence",
          created.getId(),
          "type=" + created.getTypeCode() + ", start=" + created.getStartNumber() + ", end=" + created.getEndNumber());
      ra.addFlashAttribute("success", "Secuencia fiscal creada.");
    } catch (Exception ex) {
      ra.addFlashAttribute("error", ex.getMessage());
    }
    return "redirect:/billing/fiscal";
  }

  @PostMapping("/{id}/deactivate")
  public String deactivate(@PathVariable Long id,
                           Authentication authentication,
                           RedirectAttributes ra) {
    if (!isSupervisor(authentication)) {
      ra.addFlashAttribute("error", "No tienes permiso para desactivar secuencias fiscales. Requiere supervisor financiero.");
      return "redirect:/billing/fiscal";
    }
    try {
      fiscalSequenceService.deactivate(id);
      auditService.log("FISCAL_SEQUENCE_DEACTIVATED",
          "FiscalSequence",
          id,
          "Secuencia desactivada");
      ra.addFlashAttribute("success", "Secuencia desactivada.");
    } catch (Exception ex) {
      ra.addFlashAttribute("error", ex.getMessage());
    }
    return "redirect:/billing/fiscal";
  }

  private boolean isSupervisor(Authentication authentication) {
    boolean isAdmin = SecurityRoleUtils.hasAdminAuthority(authentication);
    if (isAdmin) return true;
    if (authentication == null || !authentication.isAuthenticated()) return false;
    return billingSupervisorAccessService.isSupervisor(authentication.getName());
  }
}
