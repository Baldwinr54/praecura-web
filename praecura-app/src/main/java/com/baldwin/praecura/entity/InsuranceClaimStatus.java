package com.baldwin.praecura.entity;

public enum InsuranceClaimStatus {
  DRAFT("Borrador"),
  SUBMITTED("Enviado"),
  IN_REVIEW("En revisión"),
  APPROVED("Aprobado"),
  PARTIALLY_APPROVED("Parcial"),
  DENIED("Rechazado"),
  PAID("Pagado"),
  CLOSED("Cerrado");

  private final String label;

  InsuranceClaimStatus(String label) {
    this.label = label;
  }

  public String label() {
    return label;
  }
}
