package com.baldwin.praecura.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "insurance_plans")
@Getter
@Setter
public class InsurancePlan {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.EAGER, optional = false)
  @JoinColumn(name = "payer_id", nullable = false)
  private InsurancePayer payer;

  @Column(name = "plan_code", nullable = false, length = 40)
  private String planCode;

  @Column(nullable = false, length = 160)
  private String name;

  @Column(name = "coverage_percent", nullable = false, precision = 5, scale = 2)
  private BigDecimal coveragePercent = BigDecimal.ZERO;

  @Column(name = "copay_percent", nullable = false, precision = 5, scale = 2)
  private BigDecimal copayPercent = BigDecimal.ZERO;

  @Column(name = "deductible_amount", nullable = false, precision = 12, scale = 2)
  private BigDecimal deductibleAmount = BigDecimal.ZERO;

  @Column(name = "requires_authorization", nullable = false)
  private boolean requiresAuthorization = false;

  @Column(nullable = false)
  private boolean active = true;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt = LocalDateTime.now();

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt = LocalDateTime.now();
}
