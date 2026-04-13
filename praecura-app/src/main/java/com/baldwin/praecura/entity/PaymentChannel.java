package com.baldwin.praecura.entity;

public enum PaymentChannel {
  IN_PERSON("Presencial"),
  ONLINE_LINK("Link de pago"),
  BANK_TRANSFER("Transferencia"),
  INSURANCE("Seguro"),
  OTHER("Otro");

  private final String label;

  PaymentChannel(String label) {
    this.label = label;
  }

  public String label() {
    return label;
  }
}
