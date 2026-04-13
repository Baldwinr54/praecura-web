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
@Table(name = "patient_consent_logs")
@Getter
@Setter
public class PatientConsentLog {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "patient_id", nullable = false)
  private Patient patient;

  @Enumerated(EnumType.STRING)
  @Column(name = "consent_type", nullable = false, length = 30)
  private ConsentType consentType;

  @Column(nullable = false)
  private boolean granted;

  @Column(length = 40)
  private String source;

  @Column(name = "captured_by", length = 120)
  private String capturedBy;

  @Column(name = "captured_at", nullable = false)
  private LocalDateTime capturedAt = LocalDateTime.now();

  @Column(name = "ip_address", length = 80)
  private String ipAddress;

  @Column(name = "user_agent", length = 255)
  private String userAgent;

  @Column(columnDefinition = "text")
  private String notes;
}
