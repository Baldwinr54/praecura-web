package com.baldwin.praecura.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ConditionForm {
  @NotBlank
  @Size(max = 200)
  private String name;

  @Size(max = 20)
  private String icd10Code;

  private LocalDate onsetDate;

  private String notes;
}
