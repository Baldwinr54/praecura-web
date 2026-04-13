package com.baldwin.praecura.entity;

public enum PaymentLinkProvider {
  AZUL_LINK("AZUL"),
  CARDNET_BOTON("CardNET"),
  MANUAL("Manual");

  private final String label;

  PaymentLinkProvider(String label) {
    this.label = label;
  }

  public String label() {
    return label;
  }
}
