package com.baldwin.praecura.entity;

public enum DispensationStatus {
  PENDING("Pendiente"),
  PARTIALLY_DISPENSED("Parcial"),
  DISPENSED("Dispensado"),
  CANCELED("Cancelado");

  private final String label;

  DispensationStatus(String label) {
    this.label = label;
  }

  public String label() {
    return label;
  }
}
