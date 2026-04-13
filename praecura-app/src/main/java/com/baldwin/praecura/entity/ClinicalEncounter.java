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
@Table(name = "clinical_encounters")
@Getter
@Setter
public class ClinicalEncounter {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.EAGER, optional = false)
  @JoinColumn(name = "patient_id", nullable = false)
  private Patient patient;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "appointment_id")
  private Appointment appointment;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "doctor_id")
  private Doctor doctor;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private ClinicalEncounterStatus status = ClinicalEncounterStatus.OPEN;

  @Column(name = "encounter_at", nullable = false)
  private LocalDateTime encounterAt = LocalDateTime.now();

  @Column(name = "chief_complaint", length = 400)
  private String chiefComplaint;

  @Column(columnDefinition = "text")
  private String subjective;

  @Column(columnDefinition = "text")
  private String objective;

  @Column(columnDefinition = "text")
  private String assessment;

  @Column(columnDefinition = "text")
  private String plan;

  @Column(name = "created_by", length = 120)
  private String createdBy;

  @Column(name = "updated_by", length = 120)
  private String updatedBy;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt = LocalDateTime.now();

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt = LocalDateTime.now();
}
