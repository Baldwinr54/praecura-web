package com.baldwin.praecura.entity;

public enum EcfStatus {
  PENDING("Pendiente"),
  SIGNED("Firmado"),
  SENT("Enviado"),
  ACCEPTED("Aceptado"),
  ACCEPTED_WITH_OBSERVATION("Aceptado con observación"),
  REJECTED("Rechazado"),
  VOIDED("Anulado"),
  ERROR("Error");

  private final String label;

  EcfStatus(String label) {
    this.label = label;
  }

  public String label() {
    return label;
  }
}
