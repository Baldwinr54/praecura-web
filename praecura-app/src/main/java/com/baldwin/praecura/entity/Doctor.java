package com.baldwin.praecura.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalTime;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "doctors")
public class Doctor {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @NotBlank
  @Size(min = 3, max = 120)
  @Column(name = "full_name", nullable = false, length = 120)
  private String fullName;

  /**
   * Campo legacy (texto libre) para compatibilidad.
   * Se mantiene mientras migramos totalmente a catálogo/relación.
   */
  @Column(name = "specialty", length = 120)
  private String specialtyText;

  /**
   * Especialidad desde catálogo (FK opcional).
   */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "specialty_id")
  private Specialty specialty;

  /**
   * Servicios que ofrece el médico.
   */
  @ManyToMany(fetch = FetchType.LAZY)
  @JoinTable(
      name = "doctor_services",
      joinColumns = @JoinColumn(name = "doctor_id"),
      inverseJoinColumns = @JoinColumn(name = "service_id")
  )
  private Set<MedicalService> services = new HashSet<>();

  @Column(name = "license_no", length = 60)
  private String licenseNo;

  @Column(length = 30)
  private String phone;
  /**
   * Buffer/holgura (minutos) entre citas para este médico.
   * Se usa para evitar que las citas queden pegadas sin tiempo de transición.
   */
  @Column(name = "buffer_minutes", nullable = false)
  private int bufferMinutes = 5;

  @Column(name = "work_start", nullable = false)
  private LocalTime workStart = LocalTime.of(8, 0);

  @Column(name = "work_end", nullable = false)
  private LocalTime workEnd = LocalTime.of(17, 0);

  @Column(name = "work_days", nullable = false, length = 32)
  private String workDays = "MON,TUE,WED,THU,FRI";



  @Column(nullable = false)
  private boolean active = true;
}
