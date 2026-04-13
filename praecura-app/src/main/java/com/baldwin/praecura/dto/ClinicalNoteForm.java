package com.baldwin.praecura.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ClinicalNoteForm {
  @NotBlank
  @Size(max = 200)
  private String title;

  @NotBlank
  private String note;

  private LocalDateTime recordedAt;
}
