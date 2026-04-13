package com.baldwin.praecura.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "metrics_daily")
@Getter
@Setter
public class MetricsDaily {

  @Id
  @Column(name = "day", nullable = false)
  private LocalDate day;

  @Column(nullable = false)
  private long total;

  @Column(nullable = false)
  private long pendientes;

  @Column(nullable = false)
  private long programadas;

  @Column(nullable = false)
  private long confirmadas;

  @Column(nullable = false)
  private long completadas;

  @Column(nullable = false)
  private long canceladas;

  @Column(name = "no_asistio", nullable = false)
  private long noAsistio;

  @Column(name = "no_show_rate", nullable = false)
  private double noShowRate;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt = LocalDateTime.now();
}
