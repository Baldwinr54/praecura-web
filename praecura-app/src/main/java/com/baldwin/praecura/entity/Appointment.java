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
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "appointments")
public class Appointment {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false, fetch = FetchType.EAGER)
  @JoinColumn(name = "patient_id", nullable = false)
  private Patient patient;

  @ManyToOne(optional = false, fetch = FetchType.EAGER)
  @JoinColumn(name = "doctor_id", nullable = false)
  private Doctor doctor;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "service_id")
  private MedicalService service;

  // HTML5 datetime-local posts values in the form yyyy-MM-dd'T'HH:mm
  @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
  @Column(name = "scheduled_at", nullable = false)
  private LocalDateTime scheduledAt;

  @Column(length = 200)
  private String reason;

  @Column(name = "duration_minutes", nullable = false)
  private int durationMinutes = 30;

  @Column(length = 500)
  private String notes;

  @Column(name = "reminded_at")
  private LocalDateTime remindedAt;

  @Column(nullable = false)
  private boolean active = true;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private AppointmentStatus status = AppointmentStatus.PROGRAMADA;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "site_id")
  private ClinicSite site;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "resource_id")
  private ClinicResource resource;

  @Enumerated(EnumType.STRING)
  @Column(name = "triage_level", length = 20)
  private TriageLevel triageLevel;

  @Column(name = "triage_notes", length = 500)
  private String triageNotes;

  @Column(name = "checked_in_at")
  private LocalDateTime checkedInAt;

  @Column(name = "started_at")
  private LocalDateTime startedAt;

  @Column(name = "completed_at")
  private LocalDateTime completedAt;
}
