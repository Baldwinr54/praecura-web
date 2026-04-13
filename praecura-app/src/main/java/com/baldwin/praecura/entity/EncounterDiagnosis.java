package com.baldwin.praecura.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "encounter_diagnoses")
@Getter
@Setter
public class EncounterDiagnosis {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.EAGER, optional = false)
  @JoinColumn(name = "encounter_id", nullable = false)
  private ClinicalEncounter encounter;

  @Column(name = "icd10_code", length = 20)
  private String icd10Code;

  @Column(nullable = false, length = 400)
  private String description;

  @Column(name = "primary_diagnosis", nullable = false)
  private boolean primaryDiagnosis;

  @Column(length = 500)
  private String notes;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt = LocalDateTime.now();
}
