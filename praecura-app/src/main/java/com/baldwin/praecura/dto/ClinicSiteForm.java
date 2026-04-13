package com.baldwin.praecura.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ClinicSiteForm {

  private Long id;

  @NotBlank
  @Size(min = 2, max = 120)
  private String name;

  @Size(max = 30)
  private String code;

  @Size(max = 200)
  private String address;

  @Size(max = 12, message = "Máximo 12 caracteres.")
  @Pattern(
      regexp = "^$|^(\\d{10}|\\d{3}-\\d{3}-\\d{4})$",
      message = "Teléfono inválido. Usa 10 dígitos (con o sin guiones)."
  )
  private String phone;

  @Size(max = 500)
  private String notes;
}
