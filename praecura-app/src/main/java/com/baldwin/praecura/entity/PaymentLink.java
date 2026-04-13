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
@Table(name = "payment_links")
@Getter
@Setter
public class PaymentLink {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false, fetch = FetchType.EAGER)
  @JoinColumn(name = "invoice_id", nullable = false)
  private Invoice invoice;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private PaymentLinkProvider provider;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private PaymentLinkStatus status = PaymentLinkStatus.CREATED;

  @Column(precision = 12, scale = 2, nullable = false)
  private BigDecimal amount = BigDecimal.ZERO;

  @Column(nullable = false, length = 3)
  private String currency = "DOP";

  @Column(columnDefinition = "text")
  private String url;

  @Column(name = "external_id", length = 120)
  private String externalId;

  @Column(name = "session_id", length = 120)
  private String sessionId;

  @Column(name = "session_key", length = 120)
  private String sessionKey;

  @Column(length = 500)
  private String notes;

  @Column(name = "created_by", length = 120)
  private String createdBy;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt = LocalDateTime.now();

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt = LocalDateTime.now();

  @Column(name = "expires_at")
  private LocalDateTime expiresAt;
}
