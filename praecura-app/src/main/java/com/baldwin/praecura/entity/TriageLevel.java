package com.baldwin.praecura.entity;

public enum TriageLevel {
  ROJO("Rojo (Emergencia)", 1),
  NARANJA("Naranja (Muy urgente)", 2),
  AMARILLO("Amarillo (Urgente)", 3),
  VERDE("Verde (Menos urgente)", 4),
  AZUL("Azul (No urgente)", 5);

  private final String label;
  private final int priority;

  TriageLevel(String label, int priority) {
    this.label = label;
    this.priority = priority;
  }

  public String label() {
    return label;
  }

  public int priority() {
    return priority;
  }
}
