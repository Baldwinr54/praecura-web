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
@Table(name = "clinical_orders")
@Getter
@Setter
public class ClinicalOrder {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.EAGER, optional = false)
  @JoinColumn(name = "encounter_id", nullable = false)
  private ClinicalEncounter encounter;

  @Enumerated(EnumType.STRING)
  @Column(name = "order_type", nullable = false, length = 40)
  private ClinicalOrderType orderType;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 40)
  private ClinicalOrderStatus status = ClinicalOrderStatus.ORDERED;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private ClinicalOrderPriority priority = ClinicalOrderPriority.ROUTINE;

  @Column(name = "order_name", nullable = false, length = 220)
  private String orderName;

  @Column(length = 600)
  private String instructions;

  @Column(name = "due_at")
  private LocalDateTime dueAt;

  @Column(name = "cost_estimate", nullable = false, precision = 12, scale = 2)
  private BigDecimal costEstimate = BigDecimal.ZERO;

  @Column(name = "result_summary", columnDefinition = "text")
  private String resultSummary;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt = LocalDateTime.now();

  @Column(name = "completed_at")
  private LocalDateTime completedAt;
}
