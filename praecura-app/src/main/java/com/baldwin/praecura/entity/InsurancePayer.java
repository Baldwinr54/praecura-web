package com.baldwin.praecura.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "insurance_payers")
@Getter
@Setter
public class InsurancePayer {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 30)
  private String code;

  @Column(nullable = false, length = 160)
  private String name;

  @Column(length = 20)
  private String rnc;

  @Column(name = "contact_phone", length = 40)
  private String contactPhone;

  @Column(name = "contact_email", length = 160)
  private String contactEmail;

  @Column(length = 500)
  private String notes;

  @Column(nullable = false)
  private boolean active = true;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt = LocalDateTime.now();

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt = LocalDateTime.now();
}
