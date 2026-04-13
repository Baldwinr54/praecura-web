package com.baldwin.praecura.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VitalForm {
  private LocalDateTime recordedAt;

  @DecimalMin(value = "0.0")
  @DecimalMax(value = "500.0")
  private BigDecimal weightKg;

  @DecimalMin(value = "0.0")
  @DecimalMax(value = "300.0")
  private BigDecimal heightCm;

  @DecimalMin(value = "30.0")
  @DecimalMax(value = "45.0")
  private BigDecimal temperatureC;

  @Min(0)
  private Integer heartRate;

  @Min(0)
  private Integer respiratoryRate;

  private String bloodPressure;

  @Min(0)
  private Integer oxygenSaturation;

  private String notes;
}
