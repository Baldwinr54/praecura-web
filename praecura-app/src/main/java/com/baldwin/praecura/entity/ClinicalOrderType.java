package com.baldwin.praecura.entity;

public enum ClinicalOrderType {
  LABORATORY("Laboratorio"),
  IMAGING("Imagenología"),
  PROCEDURE("Procedimiento"),
  MEDICATION("Medicamento"),
  REFERRAL("Referimiento"),
  HOSPITALIZATION("Hospitalización");

  private final String label;

  ClinicalOrderType(String label) {
    this.label = label;
  }

  public String label() {
    return label;
  }
}
