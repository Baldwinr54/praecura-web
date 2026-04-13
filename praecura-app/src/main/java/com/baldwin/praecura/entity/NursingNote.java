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
@Table(name = "nursing_notes")
@Getter
@Setter
public class NursingNote {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.EAGER, optional = false)
  @JoinColumn(name = "admission_id", nullable = false)
  private PatientAdmission admission;

  @Column(nullable = false, length = 30)
  private String shift = "AM";

  @Column(name = "recorded_at", nullable = false)
  private LocalDateTime recordedAt = LocalDateTime.now();

  @Column(name = "recorded_by", length = 120)
  private String recordedBy;

  @Column(columnDefinition = "text", nullable = false)
  private String notes;

  @Column(name = "vitals_snapshot", length = 500)
  private String vitalsSnapshot;

  @Column(name = "medication_administered", length = 500)
  private String medicationAdministered;

  @Column(name = "adverse_event", nullable = false)
  private boolean adverseEvent;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt = LocalDateTime.now();
}
