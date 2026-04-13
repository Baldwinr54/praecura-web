package com.baldwin.praecura.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * Catálogo de especialidades médicas.
 */
@Entity
@Table(name = "specialties")
@Getter
@Setter
public class Specialty {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "name", nullable = false, length = 120)
  private String name;

  @Column(name = "active", nullable = false)
  private boolean active = true;
}
