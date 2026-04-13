package com.baldwin.praecura.entity;

public enum ClinicalOrderStatus {
  ORDERED("Ordenada"),
  IN_PROGRESS("En proceso"),
  COMPLETED("Completada"),
  CANCELED("Cancelada");

  private final String label;

  ClinicalOrderStatus(String label) {
    this.label = label;
  }

  public String label() {
    return label;
  }
}
