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
@Table(name = "surgery_schedules")
@Getter
@Setter
public class SurgerySchedule {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "admission_id")
  private PatientAdmission admission;

  @ManyToOne(fetch = FetchType.EAGER, optional = false)
  @JoinColumn(name = "patient_id", nullable = false)
  private Patient patient;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "doctor_id")
  private Doctor doctor;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "site_id")
  private ClinicSite site;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "resource_id")
  private ClinicResource resource;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 40)
  private SurgeryStatus status = SurgeryStatus.SCHEDULED;

  @Column(name = "procedure_name", nullable = false, length = 220)
  private String procedureName;

  @Column(name = "anesthesia_type", length = 120)
  private String anesthesiaType;

  @Column(name = "scheduled_at", nullable = false)
  private LocalDateTime scheduledAt;

  @Column(name = "estimated_minutes", nullable = false)
  private int estimatedMinutes = 60;

  @Column(name = "started_at")
  private LocalDateTime startedAt;

  @Column(name = "ended_at")
  private LocalDateTime endedAt;

  @Column(length = 600)
  private String notes;

  @Column(name = "created_by", length = 120)
  private String createdBy;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt = LocalDateTime.now();

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt = LocalDateTime.now();
}
