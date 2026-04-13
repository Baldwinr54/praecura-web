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
@Table(name = "insurance_authorizations")
@Getter
@Setter
public class InsuranceAuthorization {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.EAGER, optional = false)
  @JoinColumn(name = "coverage_id", nullable = false)
  private PatientInsuranceCoverage coverage;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "appointment_id")
  private Appointment appointment;

  @Column(name = "authorization_number", length = 80)
  private String authorizationNumber;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 40)
  private InsuranceAuthorizationStatus status = InsuranceAuthorizationStatus.REQUESTED;

  @Column(name = "requested_amount", nullable = false, precision = 12, scale = 2)
  private BigDecimal requestedAmount = BigDecimal.ZERO;

  @Column(name = "approved_amount", nullable = false, precision = 12, scale = 2)
  private BigDecimal approvedAmount = BigDecimal.ZERO;

  @Column(name = "requested_at", nullable = false)
  private LocalDateTime requestedAt = LocalDateTime.now();

  @Column(name = "expires_at")
  private LocalDateTime expiresAt;

  @Column(length = 500)
  private String notes;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt = LocalDateTime.now();
}
