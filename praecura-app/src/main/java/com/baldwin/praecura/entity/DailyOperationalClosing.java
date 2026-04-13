package com.baldwin.praecura.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "daily_operational_closings")
@Getter
@Setter
public class DailyOperationalClosing {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "closing_date", nullable = false)
  private LocalDate closingDate;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private DailyClosingStatus status = DailyClosingStatus.DRAFT;

  @Column(name = "total_appointments", nullable = false)
  private long totalAppointments;

  @Column(name = "completed_appointments", nullable = false)
  private long completedAppointments;

  @Column(name = "total_collected", nullable = false, precision = 14, scale = 2)
  private BigDecimal totalCollected = BigDecimal.ZERO;

  @Column(name = "total_pending", nullable = false, precision = 14, scale = 2)
  private BigDecimal totalPending = BigDecimal.ZERO;

  @Column(name = "total_refunds", nullable = false, precision = 14, scale = 2)
  private BigDecimal totalRefunds = BigDecimal.ZERO;

  @Column(name = "open_alerts", nullable = false)
  private long openAlerts;

  @Column(length = 500)
  private String notes;

  @Column(name = "generated_at", nullable = false)
  private LocalDateTime generatedAt = LocalDateTime.now();

  @Column(name = "generated_by", length = 120)
  private String generatedBy;
}
