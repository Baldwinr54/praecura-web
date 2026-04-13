package com.baldwin.praecura.dto;

import com.baldwin.praecura.entity.AllergySeverity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AllergyForm {
  @NotBlank
  @Size(max = 200)
  private String allergen;

  @Size(max = 200)
  private String reaction;

  private AllergySeverity severity = AllergySeverity.UNKNOWN;

  private String notes;
}
