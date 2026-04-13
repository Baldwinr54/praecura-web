package com.baldwin.praecura.controller;

import com.baldwin.praecura.dto.OwnerBrandingForm;
import com.baldwin.praecura.security.SecurityRoleUtils;
import com.baldwin.praecura.service.OwnerAccessService;
import com.baldwin.praecura.service.SystemBrandingService;
import jakarta.validation.Valid;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/owner")
public class OwnerSettingsController {

  private final SystemBrandingService systemBrandingService;
  private final OwnerAccessService ownerAccessService;

  public OwnerSettingsController(SystemBrandingService systemBrandingService,
                                 OwnerAccessService ownerAccessService) {
    this.systemBrandingService = systemBrandingService;
    this.ownerAccessService = ownerAccessService;
  }

  @GetMapping("/branding")
  public String form(Authentication authentication, Model model) {
    requireOwner(authentication);
    if (!model.containsAttribute("form")) {
      model.addAttribute("form", systemBrandingService.toForm());
    }
    return "owner/branding";
  }

  @PostMapping("/branding")
  public String save(Authentication authentication,
                     @Valid @ModelAttribute("form") OwnerBrandingForm form,
                     BindingResult bindingResult,
                     RedirectAttributes ra) {
    requireOwner(authentication);

    if (bindingResult.hasErrors()) {
      return "owner/branding";
    }

    systemBrandingService.save(form, authentication != null ? authentication.getName() : null);
    ra.addFlashAttribute("success", "Configuración de marca y facturación actualizada.");
    return "redirect:/owner/branding";
  }

  private void requireOwner(Authentication authentication) {
    if (authentication == null || !authentication.isAuthenticated()) {
      throw new AccessDeniedException("Solo el usuario propietario puede acceder a esta sección.");
    }
    boolean isAdmin = SecurityRoleUtils.hasAdminAuthority(authentication);
    if (!isAdmin && !ownerAccessService.isOwner(authentication.getName())) {
      throw new AccessDeniedException("Solo el usuario propietario puede acceder a esta sección.");
    }
  }
}
