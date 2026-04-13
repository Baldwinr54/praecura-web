package com.baldwin.praecura.entity;

public enum ReceivableCommitmentStatus {
  PENDING("Pendiente"),
  FULFILLED("Cumplido"),
  BROKEN("Incumplido"),
  CANCELED("Cancelado");

  private final String label;

  ReceivableCommitmentStatus(String label) {
    this.label = label;
  }

  public String label() {
    return label;
  }
}
