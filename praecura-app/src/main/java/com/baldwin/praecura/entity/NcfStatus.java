package com.baldwin.praecura.entity;

public enum NcfStatus {
  PENDING("Pendiente"),
  ISSUED("Emitido"),
  VOIDED("Anulado");

  private final String label;

  NcfStatus(String label) {
    this.label = label;
  }

  public String label() {
    return label;
  }
}
