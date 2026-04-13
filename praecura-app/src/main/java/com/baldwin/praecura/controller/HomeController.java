package com.baldwin.praecura.controller;

import com.baldwin.praecura.service.DashboardService;
import java.time.LocalDate;
import org.springframework.ui.Model;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class HomeController {
  private final DashboardService dashboardService;

  public HomeController(DashboardService dashboardService) {
    this.dashboardService = dashboardService;
  }

  // Soporte de ruta histórica: algunas versiones/redirecciones usan "/dashboard".
  // Mantenerla evita 404 post-login si existe un SavedRequest con esa URL.
  @GetMapping({"/", "/dashboard"})
  public String home(@RequestParam(required = false) LocalDate from,
                     @RequestParam(required = false) LocalDate to,
                     Model model) {
    LocalDate today = LocalDate.now();
    LocalDate fromDate = (from != null) ? from : today.minusDays(30);
    LocalDate toDate = (to != null) ? to : today;

    var summary = dashboardService.loadTodaySummary(today);
    var series = dashboardService.loadMetricsSeries(fromDate, toDate);
    model.addAttribute("dashboard", summary);
    model.addAttribute("series", series);
    model.addAttribute("from", fromDate);
    model.addAttribute("to", toDate);
    return "home";
  }

  @GetMapping("/login")
  public String login() { return "login"; }

  @GetMapping("/access-denied")
  public String accessDenied() { return "access-denied"; }
}
