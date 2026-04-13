package com.baldwin.praecura.entity;

public enum ConsentType {
  SMS("SMS"),
  EMAIL("Email"),
  WHATSAPP("WhatsApp"),
  TREATMENT("Tratamiento"),
  PRIVACY("Privacidad");

  private final String label;

  ConsentType(String label) {
    this.label = label;
  }

  public String label() {
    return label;
  }
}
