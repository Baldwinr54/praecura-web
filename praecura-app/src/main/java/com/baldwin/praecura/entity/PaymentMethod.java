package com.baldwin.praecura.entity;

public enum PaymentMethod {
  CASH("Efectivo"),
  CARD("Tarjeta"),
  TRANSFER("Transferencia"),
  CHECK("Cheque"),
  INSURANCE("Seguro"),
  OTHER("Otro");

  private final String label;

  PaymentMethod(String label) {
    this.label = label;
  }

  public String label() {
    return label;
  }
}
