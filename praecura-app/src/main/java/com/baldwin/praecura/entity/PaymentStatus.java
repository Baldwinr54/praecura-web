package com.baldwin.praecura.entity;

public enum PaymentStatus {
  PENDING("Pendiente"),
  AUTHORIZED("Autorizado"),
  CAPTURED("Capturado"),
  FAILED("Fallido"),
  REFUNDED("Reembolsado"),
  VOIDED("Anulado");

  private final String label;

  PaymentStatus(String label) {
    this.label = label;
  }

  public String label() {
    return label;
  }
}
