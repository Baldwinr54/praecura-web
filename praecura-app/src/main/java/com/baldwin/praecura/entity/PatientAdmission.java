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
@Table(name = "patient_admissions")
@Getter
@Setter
public class PatientAdmission {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.EAGER, optional = false)
  @JoinColumn(name = "patient_id", nullable = false)
  private Patient patient;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "bed_id")
  private InpatientBed bed;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "doctor_id")
  private Doctor doctor;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 40)
  private AdmissionStatus status = AdmissionStatus.ADMITTED;

  @Column(name = "admitted_at", nullable = false)
  private LocalDateTime admittedAt = LocalDateTime.now();

  @Column(name = "expected_discharge_at")
  private LocalDateTime expectedDischargeAt;

  @Column(name = "discharged_at")
  private LocalDateTime dischargedAt;

  @Column(name = "admission_reason", length = 500)
  private String admissionReason;

  @Column(name = "discharge_summary", columnDefinition = "text")
  private String dischargeSummary;

  @Column(name = "created_by", length = 120)
  private String createdBy;

  @Column(name = "updated_by", length = 120)
  private String updatedBy;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt = LocalDateTime.now();

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt = LocalDateTime.now();
}
