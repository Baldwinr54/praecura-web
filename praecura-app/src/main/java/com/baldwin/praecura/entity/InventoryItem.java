package com.baldwin.praecura.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "inventory_items")
@Getter
@Setter
public class InventoryItem {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(length = 60)
  private String sku;

  @Column(nullable = false, length = 200)
  private String name;

  @Column(length = 100)
  private String category;

  @Column(length = 120)
  private String presentation;

  @Column(nullable = false, length = 30)
  private String unit = "UNIDAD";

  @Column(name = "cost_price", nullable = false, precision = 12, scale = 2)
  private BigDecimal costPrice = BigDecimal.ZERO;

  @Column(name = "sale_price", nullable = false, precision = 12, scale = 2)
  private BigDecimal salePrice = BigDecimal.ZERO;

  @Column(name = "tax_rate", nullable = false, precision = 6, scale = 4)
  private BigDecimal taxRate = BigDecimal.ZERO;

  @Column(name = "min_stock", nullable = false, precision = 12, scale = 2)
  private BigDecimal minStock = BigDecimal.ZERO;

  @Column(nullable = false)
  private boolean active = true;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt = LocalDateTime.now();

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt = LocalDateTime.now();
}
