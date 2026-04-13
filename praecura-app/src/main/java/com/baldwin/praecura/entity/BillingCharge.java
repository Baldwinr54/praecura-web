package com.baldwin.praecura.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "billing_charges")
@Getter
@Setter
public class BillingCharge {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false, fetch = FetchType.EAGER)
  @JoinColumn(name = "patient_id", nullable = false)
  private Patient patient;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "appointment_id")
  private Appointment appointment;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "service_id")
  private MedicalService service;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "invoice_id")
  private Invoice invoice;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private BillingChargeCategory category = BillingChargeCategory.CONSULTATION;

  @Column(nullable = false, length = 220)
  private String description;

  @Column(nullable = false)
  private int quantity = 1;

  @Column(name = "unit_price", precision = 12, scale = 2, nullable = false)
  private BigDecimal unitPrice = BigDecimal.ZERO;

  @Column(precision = 12, scale = 2, nullable = false)
  private BigDecimal subtotal = BigDecimal.ZERO;

  @Column(precision = 12, scale = 2, nullable = false)
  private BigDecimal tax = BigDecimal.ZERO;

  @Column(precision = 12, scale = 2, nullable = false)
  private BigDecimal discount = BigDecimal.ZERO;

  @Column(precision = 12, scale = 2, nullable = false)
  private BigDecimal total = BigDecimal.ZERO;

  @Column(nullable = false, length = 3)
  private String currency = "DOP";

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private BillingChargeStatus status = BillingChargeStatus.OPEN;

  @Column(nullable = false, length = 30)
  private String source = "MANUAL";

  @Column(length = 500)
  private String notes;

  @Column(name = "performed_at")
  private LocalDateTime performedAt;

  @Column(name = "created_by", length = 120)
  private String createdBy;

  @Column(name = "updated_by", length = 120)
  private String updatedBy;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt = LocalDateTime.now();

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt = LocalDateTime.now();
}
