package com.baldwin.praecura.controller;

import com.baldwin.praecura.entity.AppUser;
import com.baldwin.praecura.repository.UserRepository;
import com.baldwin.praecura.security.PasswordPolicy;
import com.baldwin.praecura.service.AuditService;
import java.security.Principal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/account")
public class AccountController {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final AuditService auditService;

  public AccountController(UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           AuditService auditService) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
    this.auditService = auditService;
  }

  @GetMapping("/password")
  public String form(@RequestParam(required = false) String force, Model model) {
    model.addAttribute("force", force != null);
    model.addAttribute("policyHint", PasswordPolicy.policyHint());
    return "account/password";
  }

  @PostMapping("/password")
  public String change(@RequestParam String currentPassword,
                       @RequestParam String newPassword,
                       @RequestParam String confirmPassword,
                       Principal principal,
                       Model model,
                       RedirectAttributes ra) {
    if (principal == null || principal.getName() == null) {
      ra.addFlashAttribute("error", "No se pudo validar el usuario.");
      return "redirect:/login";
    }

    AppUser user = userRepository.findByUsername(principal.getName()).orElse(null);
    if (user == null) {
      ra.addFlashAttribute("error", "Usuario no encontrado.");
      return "redirect:/login";
    }

    if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
      model.addAttribute("error", "La contraseña actual es incorrecta.");
      model.addAttribute("policyHint", PasswordPolicy.policyHint());
      return "account/password";
    }

    if (!newPassword.equals(confirmPassword)) {
      model.addAttribute("error", "La nueva contraseña y la confirmación no coinciden.");
      model.addAttribute("policyHint", PasswordPolicy.policyHint());
      return "account/password";
    }

    String policyError = PasswordPolicy.validate(newPassword);
    if (policyError != null) {
      model.addAttribute("error", policyError);
      model.addAttribute("policyHint", PasswordPolicy.policyHint());
      return "account/password";
    }

    user.setPasswordHash(passwordEncoder.encode(newPassword));
    user.setForcePasswordChange(false);
    user.setFailedLoginAttempts(0);
    user.setLockedUntil(null);
    userRepository.save(user);

    auditService.log("CHANGE_PASSWORD", "User", user.getId(), "Usuario cambió su contraseña.");
    ra.addFlashAttribute("success", "Contraseña actualizada.");
    return "redirect:/";
  }
}
