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
@Table(name = "clinical_notes")
@Getter
@Setter
public class ClinicalNote {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false, fetch = FetchType.EAGER)
  @JoinColumn(name = "patient_id", nullable = false)
  private Patient patient;

  @Column(nullable = false, length = 200)
  private String title;

  @Column(columnDefinition = "text", nullable = false)
  private String note;

  @Column(name = "recorded_at", nullable = false)
  private LocalDateTime recordedAt = LocalDateTime.now();

  @Column(name = "created_by", length = 120)
  private String createdBy;

  @Column(name = "updated_by", length = 120)
  private String updatedBy;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt = LocalDateTime.now();

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt = LocalDateTime.now();
}
