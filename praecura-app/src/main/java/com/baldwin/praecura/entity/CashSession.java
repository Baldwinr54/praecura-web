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
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "cash_sessions")
@Getter
@Setter
public class CashSession {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private CashSessionStatus status = CashSessionStatus.OPEN;

  @Column(name = "opening_amount", precision = 12, scale = 2, nullable = false)
  private BigDecimal openingAmount = BigDecimal.ZERO;

  @Column(name = "closing_amount", precision = 12, scale = 2)
  private BigDecimal closingAmount;

  @Column(name = "opened_at", nullable = false)
  private LocalDateTime openedAt = LocalDateTime.now();

  @Column(name = "closed_at")
  private LocalDateTime closedAt;

  @Column(name = "opened_by", length = 120)
  private String openedBy;

  @Column(name = "closed_by", length = 120)
  private String closedBy;

  @Column(length = 500)
  private String notes;
}
