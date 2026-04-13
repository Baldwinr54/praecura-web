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
@Table(name = "payments")
@Getter
@Setter
public class Payment {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "invoice_id", nullable = false)
  private Invoice invoice;

  @ManyToOne(optional = false, fetch = FetchType.EAGER)
  @JoinColumn(name = "patient_id", nullable = false)
  private Patient patient;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "appointment_id")
  private Appointment appointment;

  @Column(precision = 12, scale = 2, nullable = false)
  private BigDecimal amount = BigDecimal.ZERO;

  @Column(name = "cash_received", precision = 12, scale = 2)
  private BigDecimal cashReceived;

  @Column(name = "cash_change", precision = 12, scale = 2)
  private BigDecimal cashChange;

  @Column(nullable = false, length = 3)
  private String currency = "DOP";

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private PaymentMethod method = PaymentMethod.CASH;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private PaymentChannel channel = PaymentChannel.IN_PERSON;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private PaymentStatus status = PaymentStatus.CAPTURED;

  @Column(length = 60)
  private String provider;

  @Column(name = "external_id", length = 120)
  private String externalId;

  @Column(name = "auth_code", length = 60)
  private String authCode;

  @Column(name = "card_brand", length = 40)
  private String cardBrand;

  @Column(length = 8)
  private String last4;

  @Column(name = "terminal_id", length = 60)
  private String terminalId;

  @Column(name = "batch_id", length = 60)
  private String batchId;

  @Column(name = "rrn", length = 60)
  private String rrn;

  @Column(length = 500)
  private String notes;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt = LocalDateTime.now();

  @Column(name = "paid_at")
  private LocalDateTime paidAt;

  @Column(name = "refunded_amount", precision = 12, scale = 2, nullable = false)
  private BigDecimal refundedAmount = BigDecimal.ZERO;

  @Column(name = "refunded_at")
  private LocalDateTime refundedAt;

  @Enumerated(EnumType.STRING)
  @Column(name = "refund_method", length = 20)
  private PaymentMethod refundMethod;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "cash_session_id")
  private CashSession cashSession;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "refund_session_id")
  private CashSession refundSession;

  @Column(name = "refund_reference", length = 120)
  private String refundReference;

  @Column(length = 120)
  private String username;

  @Column(name = "request_id", length = 120)
  private String requestId;

  @Column(name = "ip_address", length = 80)
  private String ipAddress;

  @Column(name = "user_agent", length = 255)
  private String userAgent;
}
