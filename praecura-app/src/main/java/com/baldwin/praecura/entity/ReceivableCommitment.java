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
@Table(name = "receivable_commitments")
@Getter
@Setter
public class ReceivableCommitment {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false, fetch = FetchType.EAGER)
  @JoinColumn(name = "invoice_id", nullable = false)
  private Invoice invoice;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private ReceivableCommitmentStatus status = ReceivableCommitmentStatus.PENDING;

  @Column(name = "promised_amount", nullable = false, precision = 12, scale = 2)
  private BigDecimal promisedAmount = BigDecimal.ZERO;

  @Column(name = "promised_date", nullable = false)
  private LocalDateTime promisedDate;

  @Column(length = 500)
  private String notes;

  @Column(name = "created_by", length = 120)
  private String createdBy;

  @Column(name = "updated_by", length = 120)
  private String updatedBy;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt = LocalDateTime.now();

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt = LocalDateTime.now();

  @Column(name = "fulfilled_at")
  private LocalDateTime fulfilledAt;

  @Column(name = "canceled_at")
  private LocalDateTime canceledAt;

  @Column(name = "broken_at")
  private LocalDateTime brokenAt;
}
