package com.baldwin.praecura.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "fiscal_sequences")
@Getter
@Setter
public class FiscalSequence {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "type_code", nullable = false, length = 10, unique = true)
  private String typeCode;

  @Column(length = 160)
  private String description;

  @Column(name = "start_number", nullable = false)
  private long startNumber;

  @Column(name = "end_number")
  private Long endNumber;

  @Column(name = "next_number", nullable = false)
  private long nextNumber;

  @Column(name = "number_length", nullable = false)
  private int numberLength = 8;

  @Column(nullable = false)
  private boolean active = true;

  @Column(name = "expires_at")
  private LocalDate expiresAt;

  @Column(name = "created_by", length = 120)
  private String createdBy;

  @Column(name = "updated_by", length = 120)
  private String updatedBy;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt = LocalDateTime.now();

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt = LocalDateTime.now();
}
