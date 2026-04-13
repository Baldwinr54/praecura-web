package com.baldwin.praecura.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "operational_alerts")
@Getter
@Setter
public class OperationalAlert {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "alert_type", nullable = false, length = 60)
  private String alertType;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private AlertSeverity severity = AlertSeverity.WARNING;

  @Column(nullable = false, length = 160)
  private String title;

  @Column(nullable = false, length = 600)
  private String message;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private AlertStatus status = AlertStatus.OPEN;

  @Column(name = "metadata_json", columnDefinition = "text")
  private String metadataJson;

  @Column(name = "detected_at", nullable = false)
  private LocalDateTime detectedAt = LocalDateTime.now();

  @Column(name = "resolved_at")
  private LocalDateTime resolvedAt;
}
