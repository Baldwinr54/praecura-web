package com.baldwin.praecura.entity;

public enum ApprovalStatus {
  PENDING("Pendiente"),
  APPROVED("Aprobado"),
  REJECTED("Rechazado"),
  USED("Utilizado"),
  EXPIRED("Vencido");

  private final String label;

  ApprovalStatus(String label) {
    this.label = label;
  }

  public String label() {
    return label;
  }
}
