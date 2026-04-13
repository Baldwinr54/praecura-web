package com.baldwin.praecura.entity;

public enum BedStatus {
  AVAILABLE("Disponible"),
  OCCUPIED("Ocupada"),
  CLEANING("Limpieza"),
  MAINTENANCE("Mantenimiento"),
  RESERVED("Reservada");

  private final String label;

  BedStatus(String label) {
    this.label = label;
  }

  public String label() {
    return label;
  }
}
