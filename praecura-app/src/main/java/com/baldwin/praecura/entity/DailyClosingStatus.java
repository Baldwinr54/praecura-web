package com.baldwin.praecura.entity;

public enum DailyClosingStatus {
  DRAFT("Borrador"),
  FINALIZED("Finalizado");

  private final String label;

  DailyClosingStatus(String label) {
    this.label = label;
  }

  public String label() {
    return label;
  }
}
