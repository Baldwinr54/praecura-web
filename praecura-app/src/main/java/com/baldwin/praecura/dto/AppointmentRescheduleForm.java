package com.baldwin.praecura.dto;

import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

/**
 * Formulario reducido para reprogramación de citas.
 * Mantiene la operación acotada (médico + fecha/hora).
 */
public class AppointmentRescheduleForm {

  private Long id;

  @NotNull(message = "Seleccione un médico")
  private Long doctorId;

  @NotNull(message = "Seleccione la fecha y hora")
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private LocalDateTime scheduledAt;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Long getDoctorId() {
    return doctorId;
  }

  public void setDoctorId(Long doctorId) {
    this.doctorId = doctorId;
  }

  public LocalDateTime getScheduledAt() {
    return scheduledAt;
  }

  public void setScheduledAt(LocalDateTime scheduledAt) {
    this.scheduledAt = scheduledAt;
  }
}
