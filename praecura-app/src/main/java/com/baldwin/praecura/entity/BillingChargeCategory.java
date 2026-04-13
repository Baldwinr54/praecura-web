package com.baldwin.praecura.entity;

public enum BillingChargeCategory {
  CONSULTATION("Consulta"),
  PROCEDURE("Procedimiento"),
  LABORATORY("Laboratorio"),
  IMAGING("Imagenología"),
  MEDICATION("Medicamento"),
  SUPPLY("Insumo"),
  ROOM("Estancia/Habitación"),
  PROFESSIONAL_FEE("Honorario profesional"),
  OTHER("Otro");

  private final String label;

  BillingChargeCategory(String label) {
    this.label = label;
  }

  public String label() {
    return label;
  }
}
