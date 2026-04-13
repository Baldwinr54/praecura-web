package com.baldwin.praecura.entity;

public enum InvoiceType {
  INVOICE("Factura"),
  CREDIT_NOTE("Nota de crédito");

  private final String label;

  InvoiceType(String label) {
    this.label = label;
  }

  public String label() {
    return label;
  }
}
