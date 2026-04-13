package com.baldwin.praecura.entity;

public enum AdmissionStatus {
  ADMITTED("Ingresado"),
  IN_SURGERY("En cirugía"),
  DISCHARGED("Alta"),
  CANCELED("Cancelado");

  private final String label;

  AdmissionStatus(String label) {
    this.label = label;
  }

  public String label() {
    return label;
  }
}
