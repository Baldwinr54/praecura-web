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
@Table(name = "message_logs")
@Getter
@Setter
public class MessageLog {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt = LocalDateTime.now();

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private MessageChannel channel;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private MessageStatus status;

  @Column(name = "to_address", nullable = false, length = 160)
  private String toAddress;

  @Column(length = 160)
  private String subject;

  @Column(columnDefinition = "text")
  private String body;

  @Column(columnDefinition = "text")
  private String error;

  @Column(name = "appointment_id")
  private Long appointmentId;

  @Column(name = "patient_id")
  private Long patientId;

  @Column(length = 120)
  private String username;

  @Column(name = "request_id", length = 120)
  private String requestId;

  @Column(name = "ip_address", length = 80)
  private String ipAddress;

  @Column(name = "user_agent", length = 255)
  private String userAgent;
}
