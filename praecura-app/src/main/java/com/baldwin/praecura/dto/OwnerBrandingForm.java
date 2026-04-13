package com.baldwin.praecura.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OwnerBrandingForm {

  @NotBlank(message = "El nombre visible del sistema es obligatorio.")
  @Size(max = 80, message = "Máximo 80 caracteres.")
  private String appDisplayName;

  @Size(max = 120, message = "Máximo 120 caracteres.")
  private String appTagline;

  @NotBlank(message = "El nombre de la empresa es obligatorio.")
  @Size(max = 160, message = "Máximo 160 caracteres.")
  private String companyName;

  @Size(max = 160, message = "Máximo 160 caracteres.")
  private String companyTradeName;

  @Size(max = 13, message = "Máximo 13 caracteres.")
  @Pattern(
      regexp = "^$|^(\\d{9}|\\d{11}|\\d{3}-\\d{5}-\\d|\\d{3}-\\d{7}-\\d)$",
      message = "RNC/Cédula inválido. Usa 9 u 11 dígitos (con o sin guiones)."
  )
  private String companyRnc;

  @Size(max = 200, message = "Máximo 200 caracteres.")
  private String companyAddress;

  @Size(max = 12, message = "Máximo 12 caracteres.")
  @Pattern(
      regexp = "^$|^(\\d{10}|\\d{3}-\\d{3}-\\d{4})$",
      message = "Teléfono inválido. Usa 10 dígitos (con o sin guiones)."
  )
  private String companyPhone;

  @Size(max = 160, message = "Máximo 160 caracteres.")
  @Pattern(
      regexp = "^$|^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$",
      message = "Correo inválido."
  )
  private String companyEmail;

  @Size(max = 240, message = "Máximo 240 caracteres.")
  private String invoiceFooter;
}
