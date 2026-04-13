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
@Table(name = "invoice_items")
@Getter
@Setter
public class InvoiceItem {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "invoice_id", nullable = false)
  private Invoice invoice;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "service_id")
  private MedicalService service;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "appointment_id")
  private Appointment appointment;

  @Column(nullable = false, length = 200)
  private String description;

  @Column(nullable = false)
  private int quantity = 1;

  @Column(name = "unit_price", precision = 12, scale = 2, nullable = false)
  private BigDecimal unitPrice = BigDecimal.ZERO;

  @Column(precision = 12, scale = 2, nullable = false)
  private BigDecimal total = BigDecimal.ZERO;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt = LocalDateTime.now();
}
