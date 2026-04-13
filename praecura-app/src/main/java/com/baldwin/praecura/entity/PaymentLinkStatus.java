package com.baldwin.praecura.entity;

public enum PaymentLinkStatus {
  CREATED("Creado"),
  SENT("Enviado"),
  PAID("Pagado"),
  CANCELLED("Cancelado"),
  EXPIRED("Expirado"),
  FAILED("Fallido");

  private final String label;

  PaymentLinkStatus(String label) {
    this.label = label;
  }

  public String label() {
    return label;
  }
}
