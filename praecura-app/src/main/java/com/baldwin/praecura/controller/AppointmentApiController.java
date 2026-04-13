package com.baldwin.praecura.controller;

import com.baldwin.praecura.service.AppointmentService;
import java.time.LocalDateTime;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/appointments")
public class AppointmentApiController {

  private final AppointmentService appointmentService;

  public AppointmentApiController(AppointmentService appointmentService) {
    this.appointmentService = appointmentService;
  }

  @GetMapping("/check")
  public AvailabilityResponse check(
      @RequestParam Long doctorId,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime scheduledAt,
      @RequestParam(required = false, defaultValue = "30") Integer durationMinutes,
      @RequestParam(required = false) Long appointmentId,
      @RequestParam(required = false) Long resourceId
  ) {
    int duration = durationMinutes == null ? 30 : durationMinutes;
    var result = appointmentService.checkAvailability(doctorId, scheduledAt, duration, appointmentId, resourceId);
    return new AvailabilityResponse(result.ok(), result.message(), result.suggestedAt());
  }

  public record AvailabilityResponse(boolean ok, String message, LocalDateTime suggestedAt) {}
}
