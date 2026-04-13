package com.baldwin.praecura.dto;

import com.baldwin.praecura.entity.MessageChannel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Form DTO para la gestión de pacientes.
 *
 * Se mantiene la persistencia en el campo "fullName" de la entidad, pero a nivel
 * de interfaz se separa en Nombre(s) y Apellido(s) para un modelo más profesional.
 */
public class PatientForm {

    private Long id;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 60, message = "El nombre es demasiado largo")
    private String firstName;

    @NotBlank(message = "Los apellidos son obligatorios")
    @Size(max = 80, message = "Los apellidos son demasiado largos")
    private String lastName;

    /** 10 dígitos con guiones: 809-000-0000 */
    @Pattern(regexp = "^$|^(\\d{10}|\\d{3}-\\d{3}-\\d{4})$", message = "Formato de teléfono inválido")
    private String phone;

    /** 11 dígitos con guiones: 000-0000000-0 */
    @Pattern(regexp = "^$|^(\\d{11}|\\d{3}-\\d{7}-\\d)$", message = "Formato de cédula inválido")
    private String cedula;


    @Email(message = "Correo electrónico inválido")
    @Size(max = 160, message = "El correo es demasiado largo")
    private String email;

    private boolean consentSms;

    private boolean consentEmail;

    private boolean consentWhatsapp;

    private MessageChannel preferredChannel;

    /** Etiquetas separadas por coma: VIP, Primera vez, Crónico... */
    @Size(max = 220, message = "Las etiquetas son demasiado largas")
    private String flags;

    @Size(max = 2000, message = "Las notas son demasiado largas")
  private String notes;

  @Size(max = 160, message = "El nombre fiscal es demasiado largo")
  private String billingName;

  @Size(max = 13, message = "El RNC/Cédula fiscal es demasiado largo")
  @Pattern(
      regexp = "^$|^(\\d{9}|\\d{11}|\\d{3}-\\d{5}-\\d|\\d{3}-\\d{7}-\\d)$",
      message = "Formato de RNC/Cédula fiscal inválido"
  )
  private String billingTaxId;

  @Size(max = 200, message = "La dirección fiscal es demasiado larga")
  private String billingAddress;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getCedula() {
        return cedula;
    }

    public void setCedula(String cedula) {
        this.cedula = cedula;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFlags() {
        return flags;
    }

    public void setFlags(String flags) {
        this.flags = flags;
    }

  public String getNotes() {
    return notes;
  }

  public void setNotes(String notes) {
    this.notes = notes;
  }

  public String getBillingName() {
    return billingName;
  }

  public void setBillingName(String billingName) {
    this.billingName = billingName;
  }

  public String getBillingTaxId() {
    return billingTaxId;
  }

  public void setBillingTaxId(String billingTaxId) {
    this.billingTaxId = billingTaxId;
  }

  public String getBillingAddress() {
    return billingAddress;
  }

  public void setBillingAddress(String billingAddress) {
    this.billingAddress = billingAddress;
  }

    public boolean isConsentSms() {
        return consentSms;
    }

    public void setConsentSms(boolean consentSms) {
        this.consentSms = consentSms;
    }

    public boolean isConsentEmail() {
        return consentEmail;
    }

    public void setConsentEmail(boolean consentEmail) {
        this.consentEmail = consentEmail;
    }

    public boolean isConsentWhatsapp() {
        return consentWhatsapp;
    }

    public void setConsentWhatsapp(boolean consentWhatsapp) {
        this.consentWhatsapp = consentWhatsapp;
    }

    public MessageChannel getPreferredChannel() {
        return preferredChannel;
    }

    public void setPreferredChannel(MessageChannel preferredChannel) {
        this.preferredChannel = preferredChannel;
    }
}
