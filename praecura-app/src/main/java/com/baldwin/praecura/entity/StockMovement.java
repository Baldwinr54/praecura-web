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
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "stock_movements")
@Getter
@Setter
public class StockMovement {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.EAGER, optional = false)
  @JoinColumn(name = "item_id", nullable = false)
  private InventoryItem item;

  @Enumerated(EnumType.STRING)
  @Column(name = "movement_type", nullable = false, length = 40)
  private StockMovementType movementType;

  @Column(nullable = false, precision = 12, scale = 2)
  private BigDecimal quantity;

  @Column(name = "unit_cost", nullable = false, precision = 12, scale = 2)
  private BigDecimal unitCost = BigDecimal.ZERO;

  @Column(name = "reference_type", length = 60)
  private String referenceType;

  @Column(name = "reference_id")
  private Long referenceId;

  @Column(name = "lot_number", length = 80)
  private String lotNumber;

  @Column(name = "expires_at")
  private LocalDate expiresAt;

  @Column(length = 500)
  private String notes;

  @Column(name = "created_by", length = 120)
  private String createdBy;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt = LocalDateTime.now();
}
