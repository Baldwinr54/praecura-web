package com.baldwin.praecura.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "patients")
public class Patient {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @NotBlank
  @Size(min = 3, max = 120)
  @Column(name = "full_name", nullable = false, length = 120)
  private String fullName;

  @Column(length = 30)
  private String phone;

  @Column(length = 20)
  private String cedula;

  @Column(length = 160)
  private String email;

  @Column(name = "consent_sms", nullable = false)
  private boolean consentSms = false;

  @Column(name = "consent_email", nullable = false)
  private boolean consentEmail = false;

  @Column(name = "consent_whatsapp", nullable = false)
  private boolean consentWhatsapp = false;

  @Enumerated(EnumType.STRING)
  @Column(name = "preferred_channel", length = 20)
  private MessageChannel preferredChannel;

  @Column(length = 220)
  private String flags;

  @Column(columnDefinition = "text")
  private String notes;

  @Column(name = "billing_name", length = 160)
  private String billingName;

  @Column(name = "billing_tax_id", length = 20)
  private String billingTaxId;

  @Column(name = "billing_address", length = 200)
  private String billingAddress;

  @Column(nullable = false)
  private boolean active = true;
}
