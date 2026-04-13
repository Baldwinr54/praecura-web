package com.baldwin.praecura.controller;

import com.baldwin.praecura.service.ExecutiveOpsService;
import java.time.LocalDate;
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
@RequestMapping("/reports")
public class ExecutiveOpsController {

  private final ExecutiveOpsService executiveOpsService;

  public ExecutiveOpsController(ExecutiveOpsService executiveOpsService) {
    this.executiveOpsService = executiveOpsService;
  }

  @GetMapping("/executive")
  public String executive(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                          Model model) {
    LocalDate start = from != null ? from : LocalDate.now().minusDays(30);
    LocalDate end = to != null ? to : LocalDate.now();

    model.addAttribute("from", start);
    model.addAttribute("to", end);
    model.addAttribute("summary", executiveOpsService.summary(start, end));
    model.addAttribute("alerts", executiveOpsService.listAlerts());
    model.addAttribute("closings", executiveOpsService.listClosings());
    return "reports/executive";
  }

  @PostMapping("/alerts/generate")
  public String generateAlerts(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate day,
                               RedirectAttributes ra,
                               @RequestHeader(value = "Referer", required = false) String referer) {
    var alerts = executiveOpsService.generateAlerts(day);
    ra.addFlashAttribute("success", "Alertas evaluadas. Nuevas alertas: " + alerts.size() + ".");
    return redirectBack(referer, "/reports/executive");
  }

  @PostMapping("/alerts/{id}/resolve")
  public String resolveAlert(@PathVariable Long id,
                             RedirectAttributes ra,
                             @RequestHeader(value = "Referer", required = false) String referer) {
    try {
      executiveOpsService.resolveAlert(id);
      ra.addFlashAttribute("success", "Alerta marcada como resuelta.");
    } catch (IllegalArgumentException ex) {
      ra.addFlashAttribute("error", ex.getMessage());
    }
    return redirectBack(referer, "/reports/executive");
  }

  @PostMapping("/closings/generate")
  public String generateClosing(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate day,
                                Authentication authentication,
                                RedirectAttributes ra,
                                @RequestHeader(value = "Referer", required = false) String referer) {
    String username = authentication != null && authentication.isAuthenticated() ? authentication.getName() : "system";
    var closing = executiveOpsService.generateDailyClosing(day, username);
    ra.addFlashAttribute("success", "Cierre diario generado para " + closing.getClosingDate() + ".");
    return redirectBack(referer, "/reports/executive");
  }

  @PostMapping("/closings/{id}/finalize")
  public String finalizeClosing(@PathVariable Long id,
                                @RequestParam(required = false) String notes,
                                Authentication authentication,
                                RedirectAttributes ra,
                                @RequestHeader(value = "Referer", required = false) String referer) {
    try {
      String username = authentication != null && authentication.isAuthenticated() ? authentication.getName() : "system";
      executiveOpsService.finalizeClosing(id, notes, username);
      ra.addFlashAttribute("success", "Cierre diario finalizado.");
    } catch (IllegalArgumentException ex) {
      ra.addFlashAttribute("error", ex.getMessage());
    }
    return redirectBack(referer, "/reports/executive");
  }

  private String redirectBack(String referer, String fallback) {
    if (referer == null || referer.isBlank()) return "redirect:" + fallback;
    if (referer.startsWith("/") && !referer.startsWith("//")) return "redirect:" + referer;
    return "redirect:" + fallback;
  }
}
