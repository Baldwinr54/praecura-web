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
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "purchase_order_items")
@Getter
@Setter
public class PurchaseOrderItem {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.EAGER, optional = false)
  @JoinColumn(name = "purchase_order_id", nullable = false)
  private PurchaseOrder purchaseOrder;

  @ManyToOne(fetch = FetchType.EAGER, optional = false)
  @JoinColumn(name = "item_id", nullable = false)
  private InventoryItem item;

  @Column(nullable = false, precision = 12, scale = 2)
  private BigDecimal quantity;

  @Column(name = "received_quantity", nullable = false, precision = 12, scale = 2)
  private BigDecimal receivedQuantity = BigDecimal.ZERO;

  @Column(name = "unit_cost", nullable = false, precision = 12, scale = 2)
  private BigDecimal unitCost;

  @Column(name = "tax_rate", nullable = false, precision = 6, scale = 4)
  private BigDecimal taxRate = BigDecimal.ZERO;

  @Column(nullable = false, precision = 12, scale = 2)
  private BigDecimal total = BigDecimal.ZERO;
}
