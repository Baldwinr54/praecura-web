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
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "electronic_fiscal_documents")
@Getter
@Setter
public class ElectronicFiscalDocument {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false, fetch = FetchType.EAGER)
  @JoinColumn(name = "invoice_id", nullable = false)
  private Invoice invoice;

  @Column(name = "e_ncf", length = 25)
  private String eNcf;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 40)
  private EcfStatus status = EcfStatus.PENDING;

  @Column(name = "security_code", length = 120)
  private String securityCode;

  @Column(name = "verification_url", length = 500)
  private String verificationUrl;

  @Column(name = "dgii_track_id", length = 120)
  private String dgiiTrackId;

  @Column(name = "dgii_status_code", length = 60)
  private String dgiiStatusCode;

  @Column(name = "dgii_message", length = 1000)
  private String dgiiMessage;

  @Column(name = "signed_xml", columnDefinition = "TEXT")
  private String signedXml;

  @Column(name = "sent_at")
  private LocalDateTime sentAt;

  @Column(name = "accepted_at")
  private LocalDateTime acceptedAt;

  @Column(name = "last_checked_at")
  private LocalDateTime lastCheckedAt;

  @Column(name = "attempt_count", nullable = false)
  private int attemptCount = 0;

  @Column(name = "last_attempt_at")
  private LocalDateTime lastAttemptAt;

  @Column(name = "next_retry_at")
  private LocalDateTime nextRetryAt;

  @Column(name = "last_error", length = 1000)
  private String lastError;

  @Column(name = "created_by", length = 120)
  private String createdBy;

  @Column(name = "updated_by", length = 120)
  private String updatedBy;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt = LocalDateTime.now();

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt = LocalDateTime.now();
}
