package com.baldwin.praecura.entity;

public enum SurgeryStatus {
  SCHEDULED("Programada"),
  IN_PROGRESS("En proceso"),
  COMPLETED("Completada"),
  CANCELED("Cancelada");

  private final String label;

  SurgeryStatus(String label) {
    this.label = label;
  }

  public String label() {
    return label;
  }
}
