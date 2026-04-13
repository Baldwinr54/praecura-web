package com.baldwin.praecura.entity;

public enum AlertSeverity {
  INFO("Info"),
  WARNING("Advertencia"),
  CRITICAL("Crítica");

  private final String label;

  AlertSeverity(String label) {
    this.label = label;
  }

  public String label() {
    return label;
  }
}
