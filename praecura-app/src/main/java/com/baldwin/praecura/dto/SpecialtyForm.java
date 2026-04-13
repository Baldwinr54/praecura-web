package com.baldwin.praecura.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SpecialtyForm {

  private Long id;

  @NotBlank(message = "El nombre es obligatorio")
  @Size(min = 2, max = 120, message = "El nombre debe tener entre 2 y 120 caracteres")
  private String name;

  private boolean active = true;
}
