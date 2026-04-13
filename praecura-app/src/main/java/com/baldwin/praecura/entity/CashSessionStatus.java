package com.baldwin.praecura.entity;

public enum CashSessionStatus {
  OPEN("Abierta"),
  CLOSED("Cerrada");

  private final String label;

  CashSessionStatus(String label) {
    this.label = label;
  }

  public String label() {
    return label;
  }
}
