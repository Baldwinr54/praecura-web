package com.baldwin.praecura.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalTime;
import java.util.List;

@Getter
@Setter
public class DoctorForm {

  private Long id;

  @NotBlank
  @Size(min = 3, max = 120)
  private String fullName;

  @Size(max = 120)
  private String specialty;

  /**
   * FK opcional al catálogo de especialidades.
   * Si se selecciona, se prioriza sobre el texto libre.
   */
  private Long specialtyId;

  /**
   * Servicios ofrecidos por el médico.
   */
  private java.util.List<Long> serviceIds;

  @Size(max = 60)
  private String licenseNo;

  @Size(max = 12, message = "Máximo 12 caracteres.")
  @Pattern(
      regexp = "^$|^(\\d{10}|\\d{3}-\\d{3}-\\d{4})$",
      message = "Teléfono inválido. Usa 10 dígitos (con o sin guiones)."
  )
  private String phone;
  /**
   * Buffer/holgura (minutos) entre citas para este médico.
   * Recomendado: 5 (por defecto), con opción 0/10/15 según la operación.
   */
  @Min(0)
  @Max(60)
  private Integer bufferMinutes;

  /**
   * Días laborales (MON, TUE, WED, THU, FRI, SAT, SUN).
   */
  private List<String> workDays;

  @DateTimeFormat(pattern = "HH:mm")
  private LocalTime workStart;

  @DateTimeFormat(pattern = "HH:mm")
  private LocalTime workEnd;

}
