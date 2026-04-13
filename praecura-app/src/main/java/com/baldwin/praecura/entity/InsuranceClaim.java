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
@Table(name = "insurance_claims")
@Getter
@Setter
public class InsuranceClaim {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.EAGER, optional = false)
  @JoinColumn(name = "invoice_id", nullable = false)
  private Invoice invoice;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "coverage_id")
  private PatientInsuranceCoverage coverage;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "authorization_id")
  private InsuranceAuthorization authorization;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 40)
  private InsuranceClaimStatus status = InsuranceClaimStatus.DRAFT;

  @Column(name = "claimed_amount", nullable = false, precision = 12, scale = 2)
  private BigDecimal claimedAmount = BigDecimal.ZERO;

  @Column(name = "approved_amount", nullable = false, precision = 12, scale = 2)
  private BigDecimal approvedAmount = BigDecimal.ZERO;

  @Column(name = "denied_amount", nullable = false, precision = 12, scale = 2)
  private BigDecimal deniedAmount = BigDecimal.ZERO;

  @Column(name = "denial_reason", length = 500)
  private String denialReason;

  @Column(name = "submitted_at")
  private LocalDateTime submittedAt;

  @Column(name = "resolved_at")
  private LocalDateTime resolvedAt;

  @Column(length = 500)
  private String notes;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt = LocalDateTime.now();

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt = LocalDateTime.now();
}
