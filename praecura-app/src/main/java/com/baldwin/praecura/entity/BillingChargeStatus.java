package com.baldwin.praecura.entity;

public enum BillingChargeStatus {
  OPEN("Pendiente"),
  BILLED("Facturado"),
  CANCELED("Cancelado");

  private final String label;

  BillingChargeStatus(String label) {
    this.label = label;
  }

  public String label() {
    return label;
  }
}
