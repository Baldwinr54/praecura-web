package com.baldwin.praecura.security;

public final class PasswordPolicy {

  private static final int MIN_LENGTH = 10;
  private static final String SPECIALS = "!@#$%&*?+._-";

  private PasswordPolicy() {}

  public static String validate(String password) {
    if (password == null || password.isBlank()) {
      return "La contraseña es obligatoria.";
    }
    if (password.length() < MIN_LENGTH) {
      return "La contraseña debe tener al menos " + MIN_LENGTH + " caracteres.";
    }
    if (password.chars().anyMatch(Character::isWhitespace)) {
      return "La contraseña no debe contener espacios.";
    }
    if (password.chars().noneMatch(Character::isUpperCase)) {
      return "La contraseña debe incluir al menos una letra mayúscula.";
    }
    if (password.chars().noneMatch(Character::isLowerCase)) {
      return "La contraseña debe incluir al menos una letra minúscula.";
    }
    if (password.chars().noneMatch(Character::isDigit)) {
      return "La contraseña debe incluir al menos un número.";
    }
    if (password.chars().noneMatch(ch -> SPECIALS.indexOf((char) ch) >= 0)) {
      return "La contraseña debe incluir al menos un símbolo (" + SPECIALS + ").";
    }
    return null;
  }

  public static String policyHint() {
    return "Mínimo " + MIN_LENGTH + " caracteres, con mayúscula, minúscula, número y símbolo.";
  }
}
