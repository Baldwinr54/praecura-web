package com.baldwin.praecura.entity;

public enum PurchaseOrderStatus {
  DRAFT("Borrador"),
  SENT("Enviada"),
  PARTIALLY_RECEIVED("Parcial"),
  RECEIVED("Recibida"),
  CANCELED("Cancelada");

  private final String label;

  PurchaseOrderStatus(String label) {
    this.label = label;
  }

  public String label() {
    return label;
  }
}
