package com.baldwin.praecura.dto;

import com.baldwin.praecura.entity.AppointmentStatus;
import com.baldwin.praecura.entity.TriageLevel;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Getter
@Setter
public class AppointmentForm {

  private Long id;

  @NotNull
  private Long patientId;

  @NotNull
  private Long doctorId;

  @NotNull(message = "Selecciona un servicio.")
  private Long serviceId;

  @NotNull(message = "Selecciona una sede.")
  private Long siteId;

  @NotNull(message = "Selecciona un recurso.")
  private Long resourceId;

  @NotNull
  @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
  private LocalDateTime scheduledAt;

  private String reason;

  @Min(1)
  private int durationMinutes = 30;

  /**
   * Si está en true, la duración se sincroniza con la duración del servicio seleccionado.
   * Si el usuario modifica manualmente la duración, se marca false.
   */
  private boolean durationAuto = true;

  private String notes;

  private TriageLevel triageLevel;

  private String triageNotes;

  @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
  private LocalDateTime checkedInAt;

  @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
  private LocalDateTime startedAt;

  @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
  private LocalDateTime completedAt;

  @NotNull
  private AppointmentStatus status = AppointmentStatus.PROGRAMADA;
}
