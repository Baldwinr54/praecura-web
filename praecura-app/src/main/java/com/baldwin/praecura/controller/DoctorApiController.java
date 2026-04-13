package com.baldwin.praecura.controller;

import com.baldwin.praecura.entity.Doctor;
import com.baldwin.praecura.entity.MedicalService;
import com.baldwin.praecura.service.DoctorService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.List;

@RestController
@RequestMapping("/api/doctors")
public class DoctorApiController {

  private final DoctorService doctorService;

  public DoctorApiController(DoctorService doctorService) {
    this.doctorService = doctorService;
  }

  @GetMapping("/{id}/services")
  public List<ServiceDto> services(@PathVariable Long id) {
    Doctor d = doctorService.getActiveWithRelations(id);
    return d.getServices().stream()
        .filter(MedicalService::isActive)
        .sorted(Comparator.comparing(MedicalService::getName, String.CASE_INSENSITIVE_ORDER))
        .map(s -> new ServiceDto(s.getId(), s.getName(), s.getDurationMinutes()))
        .toList();
  }

  public record ServiceDto(Long id, String name, int durationMinutes) {}
}
