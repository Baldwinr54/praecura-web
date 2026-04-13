package com.baldwin.praecura.dto;

import com.baldwin.praecura.entity.MedicationStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MedicationForm {
  @NotBlank
  @Size(max = 200)
  private String name;

  @Size(max = 60)
  private String dosage;

  @Size(max = 60)
  private String frequency;

  private LocalDate startDate;

  private LocalDate endDate;

  private MedicationStatus status = MedicationStatus.ACTIVE;

  private String notes;
}
