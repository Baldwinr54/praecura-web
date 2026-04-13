package com.baldwin.praecura.entity;

public enum AlertStatus {
  OPEN("Abierta"),
  ACKNOWLEDGED("En seguimiento"),
  RESOLVED("Resuelta");

  private final String label;

  AlertStatus(String label) {
    this.label = label;
  }

  public String label() {
    return label;
  }
}
