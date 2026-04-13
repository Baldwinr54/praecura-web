package com.baldwin.praecura.entity;

public enum ClinicalEncounterStatus {
  OPEN("Abierto"),
  CLOSED("Cerrado");

  private final String label;

  ClinicalEncounterStatus(String label) {
    this.label = label;
  }

  public String label() {
    return label;
  }
}
