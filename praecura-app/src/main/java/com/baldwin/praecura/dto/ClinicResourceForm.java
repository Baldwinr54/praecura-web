package com.baldwin.praecura.dto;

import com.baldwin.praecura.entity.ResourceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ClinicResourceForm {

  private Long id;

  @NotBlank
  @Size(min = 2, max = 120)
  private String name;

  @NotNull
  private ResourceType type;

  @NotNull
  private Long siteId;

  @Size(max = 500)
  private String notes;
}
