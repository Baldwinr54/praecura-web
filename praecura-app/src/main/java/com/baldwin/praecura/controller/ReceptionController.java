package com.baldwin.praecura.controller;

import com.baldwin.praecura.entity.Appointment;
import com.baldwin.praecura.entity.AppointmentStatus;
import com.baldwin.praecura.service.AppointmentService;
import com.baldwin.praecura.service.ClinicSiteService;
import com.baldwin.praecura.service.DoctorService;
import java.net.URI;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.format.annotation.DateTimeFormat;
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
@RequestMapping("/reception")
public class ReceptionController {

  private final AppointmentService appointmentService;
  private final DoctorService doctorService;
  private final ClinicSiteService clinicSiteService;

  public ReceptionController(AppointmentService appointmentService,
                             DoctorService doctorService,
                             ClinicSiteService clinicSiteService) {
    this.appointmentService = appointmentService;
    this.doctorService = doctorService;
    this.clinicSiteService = clinicSiteService;
  }

  @GetMapping
  public String desk(@RequestParam(value = "date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                     @RequestParam(value = "doctorId", required = false) Long doctorId,
                     @RequestParam(value = "siteId", required = false) Long siteId,
                     Model model) {
    LocalDate day = (date != null) ? date : LocalDate.now();
    List<Appointment> queue = appointmentService.receptionQueue(day, doctorId, siteId);
    var stats = appointmentService.buildReceptionStats(queue);

    LocalDateTime now = LocalDateTime.now();
    List<Appointment> pendingCheckIn = new ArrayList<>();
    List<Appointment> waiting = new ArrayList<>();
    List<Appointment> inAttention = new ArrayList<>();
    List<Appointment> completed = new ArrayList<>();
    List<Appointment> noShow = new ArrayList<>();
    List<Appointment> cancelled = new ArrayList<>();

    Map<Long, Long> waitMinutesMap = new HashMap<>();
    Map<Long, Long> attentionMinutesMap = new HashMap<>();

    for (Appointment a : queue) {
      AppointmentStatus st = a.getStatus();
      if (st == AppointmentStatus.CANCELADA) {
        cancelled.add(a);
        continue;
      }
      if (st == AppointmentStatus.NO_ASISTIO) {
        noShow.add(a);
        continue;
      }
      if (st == AppointmentStatus.COMPLETADA) {
        completed.add(a);
        attentionMinutesMap.put(a.getId(), appointmentService.attentionMinutes(a, now));
        continue;
      }
      if (a.getStartedAt() != null) {
        inAttention.add(a);
        attentionMinutesMap.put(a.getId(), appointmentService.attentionMinutes(a, now));
        continue;
      }
      if (a.getCheckedInAt() != null) {
        waiting.add(a);
        waitMinutesMap.put(a.getId(), appointmentService.waitingMinutes(a, now));
        continue;
      }
      pendingCheckIn.add(a);
    }

    long avgWaitMinutes = waiting.isEmpty()
        ? 0
        : Math.round(waiting.stream()
            .mapToLong(a -> appointmentService.waitingMinutes(a, now))
            .average()
            .orElse(0));

    model.addAttribute("day", day);
    model.addAttribute("doctorId", doctorId);
    model.addAttribute("siteId", siteId);
    model.addAttribute("stats", stats);
    model.addAttribute("avgWaitMinutes", avgWaitMinutes);
    model.addAttribute("pendingCheckIn", pendingCheckIn);
    model.addAttribute("waiting", waiting);
    model.addAttribute("inAttention", inAttention);
    model.addAttribute("completed", completed);
    model.addAttribute("noShow", noShow);
    model.addAttribute("cancelled", cancelled);
    model.addAttribute("waitMinutesMap", waitMinutesMap);
    model.addAttribute("attentionMinutesMap", attentionMinutesMap);
    model.addAttribute("doctors", doctorService.listActiveForSelect());
    model.addAttribute("sites", clinicSiteService.listActiveForSelect());
    return "reception/desk";
  }

  @PostMapping("/{id}/check-in")
  public String checkIn(@PathVariable Long id,
                        RedirectAttributes ra,
                        @RequestHeader(value = "Referer", required = false) String referer) {
    try {
      appointmentService.checkIn(id);
      ra.addFlashAttribute("success", "Check-in aplicado.");
    } catch (IllegalArgumentException ex) {
      ra.addFlashAttribute("error", ex.getMessage());
    }
    return redirectBack(referer, "/reception");
  }

  @PostMapping("/{id}/undo-check-in")
  public String undoCheckIn(@PathVariable Long id,
                            RedirectAttributes ra,
                            @RequestHeader(value = "Referer", required = false) String referer) {
    try {
      appointmentService.undoCheckIn(id);
      ra.addFlashAttribute("success", "Check-in revertido.");
    } catch (IllegalArgumentException ex) {
      ra.addFlashAttribute("error", ex.getMessage());
    }
    return redirectBack(referer, "/reception");
  }

  @PostMapping("/{id}/start")
  public String start(@PathVariable Long id,
                      RedirectAttributes ra,
                      @RequestHeader(value = "Referer", required = false) String referer) {
    try {
      appointmentService.startAttention(id);
      ra.addFlashAttribute("success", "Atencion iniciada.");
    } catch (IllegalArgumentException ex) {
      ra.addFlashAttribute("error", ex.getMessage());
    }
    return redirectBack(referer, "/reception");
  }

  @PostMapping("/{id}/complete")
  public String complete(@PathVariable Long id,
                         RedirectAttributes ra,
                         @RequestHeader(value = "Referer", required = false) String referer) {
    try {
      appointmentService.complete(id);
      ra.addFlashAttribute("success", "Cita completada.");
    } catch (IllegalArgumentException ex) {
      ra.addFlashAttribute("error", ex.getMessage());
    }
    return redirectBack(referer, "/reception");
  }

  @PostMapping("/{id}/no-show")
  public String noShow(@PathVariable Long id,
                       RedirectAttributes ra,
                       @RequestHeader(value = "Referer", required = false) String referer) {
    try {
      appointmentService.markNoShow(id);
      ra.addFlashAttribute("success", "Cita marcada como no-show.");
    } catch (IllegalArgumentException ex) {
      ra.addFlashAttribute("error", ex.getMessage());
    }
    return redirectBack(referer, "/reception");
  }

  @PostMapping("/{id}/cancel")
  public String cancel(@PathVariable Long id,
                       RedirectAttributes ra,
                       @RequestHeader(value = "Referer", required = false) String referer) {
    try {
      appointmentService.cancel(id);
      ra.addFlashAttribute("success", "Cita cancelada.");
    } catch (IllegalArgumentException ex) {
      ra.addFlashAttribute("error", ex.getMessage());
    }
    return redirectBack(referer, "/reception");
  }

  private String redirectBack(String referer, String fallbackPath) {
    if (fallbackPath == null || fallbackPath.isBlank()) fallbackPath = "/";
    if (referer == null || referer.isBlank()) return "redirect:" + fallbackPath;
    if (referer.startsWith("/")) return "redirect:" + referer;
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
