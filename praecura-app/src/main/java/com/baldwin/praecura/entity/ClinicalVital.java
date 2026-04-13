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
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "clinical_vitals")
@Getter
@Setter
public class ClinicalVital {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false, fetch = FetchType.EAGER)
  @JoinColumn(name = "patient_id", nullable = false)
  private Patient patient;

  @Column(name = "recorded_at", nullable = false)
  private LocalDateTime recordedAt = LocalDateTime.now();

  @Column(name = "weight_kg", precision = 6, scale = 2)
  private BigDecimal weightKg;

  @Column(name = "height_cm", precision = 6, scale = 2)
  private BigDecimal heightCm;

  @Column(name = "temperature_c", precision = 4, scale = 1)
  private BigDecimal temperatureC;

  @Column(name = "heart_rate")
  private Integer heartRate;

  @Column(name = "resp_rate")
  private Integer respiratoryRate;

  @Column(name = "blood_pressure", length = 20)
  private String bloodPressure;

  @Column(name = "oxygen_saturation")
  private Integer oxygenSaturation;

  @Column(columnDefinition = "text")
  private String notes;

  @Column(name = "created_by", length = 120)
  private String createdBy;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt = LocalDateTime.now();
}
