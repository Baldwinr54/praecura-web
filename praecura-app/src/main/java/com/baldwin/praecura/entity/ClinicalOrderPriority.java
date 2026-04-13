package com.baldwin.praecura.entity;

public enum ClinicalOrderPriority {
  ROUTINE("Rutina"),
  URGENT("Urgente"),
  STAT("STAT");

  private final String label;

  ClinicalOrderPriority(String label) {
    this.label = label;
  }

  public String label() {
    return label;
  }
}
