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
@Table(name = "inpatient_beds")
@Getter
@Setter
public class InpatientBed {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.EAGER, optional = false)
  @JoinColumn(name = "site_id", nullable = false)
  private ClinicSite site;

  @Column(nullable = false, length = 40)
  private String code;

  @Column(length = 120)
  private String ward;

  @Column(name = "bed_type", nullable = false, length = 40)
  private String bedType = "GENERAL";

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 40)
  private BedStatus status = BedStatus.AVAILABLE;

  @Column(nullable = false)
  private boolean active = true;

  @Column(length = 500)
  private String notes;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt = LocalDateTime.now();

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt = LocalDateTime.now();
}
