package com.baldwin.praecura.entity;

public enum InvoiceStatus {
  DRAFT("Borrador"),
  ISSUED("Emitida"),
  PARTIALLY_PAID("Pago parcial"),
  PAID("Pagada"),
  VOID("Anulada"),
  REFUNDED("Reembolsada");

  private final String label;

  InvoiceStatus(String label) {
    this.label = label;
  }

  public String label() {
    return label;
  }
}
