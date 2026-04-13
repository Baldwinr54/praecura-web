package com.baldwin.praecura.entity;

public enum StockMovementType {
  PURCHASE_IN("Entrada por compra"),
  ADJUSTMENT_IN("Ajuste positivo"),
  DISPENSE_OUT("Salida por dispensación"),
  ADJUSTMENT_OUT("Ajuste negativo"),
  RETURN_IN("Devolución entrada"),
  RETURN_OUT("Devolución salida"),
  TRANSFER_IN("Transferencia entrada"),
  TRANSFER_OUT("Transferencia salida");

  private final String label;

  StockMovementType(String label) {
    this.label = label;
  }

  public String label() {
    return label;
  }
}
