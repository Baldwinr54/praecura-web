package com.baldwin.praecura.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class MedicalServiceForm {

  private Long id;

  @NotBlank
  @Size(min = 2, max = 120)
  private String name;

  @Min(1)
  private int durationMinutes;

  private BigDecimal price;
}
