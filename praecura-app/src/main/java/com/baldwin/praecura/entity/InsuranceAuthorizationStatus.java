package com.baldwin.praecura.entity;

public enum InsuranceAuthorizationStatus {
  REQUESTED("Solicitada"),
  APPROVED("Aprobada"),
  PARTIALLY_APPROVED("Parcial"),
  REJECTED("Rechazada"),
  EXPIRED("Vencida");

  private final String label;

  InsuranceAuthorizationStatus(String label) {
    this.label = label;
  }

  public String label() {
    return label;
  }
}
