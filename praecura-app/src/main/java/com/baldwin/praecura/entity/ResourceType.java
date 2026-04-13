package com.baldwin.praecura.entity;

public enum ResourceType {
  CONSULTORIO("Consultorio"),
  SALA_PROCEDIMIENTOS("Sala de procedimientos"),
  DIAGNOSTICO("Diagnóstico"),
  EQUIPO("Equipo");

  private final String label;

  ResourceType(String label) {
    this.label = label;
  }

  public String label() {
    return label;
  }
}
