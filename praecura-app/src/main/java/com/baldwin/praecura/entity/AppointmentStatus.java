package com.baldwin.praecura.entity;

public enum AppointmentStatus {
  PROGRAMADA,
  CONFIRMADA,
  CANCELADA,
  COMPLETADA,
  NO_ASISTIO;

  /**
   * Reglas de transición de estado.
   * - CANCELADA/COMPLETADA/NO_ASISTIO son estados finales.
   * - PROGRAMADA puede pasar a CONFIRMADA, CANCELADA, NO_ASISTIO o COMPLETADA.
   * - CONFIRMADA puede pasar a PROGRAMADA (reprogramación), CANCELADA, NO_ASISTIO o COMPLETADA.
   */
  public boolean canTransitionTo(AppointmentStatus target) {
    if (target == null) return false;
    if (this == target) return true;
    return switch (this) {
      case PROGRAMADA -> target == CONFIRMADA || target == CANCELADA || target == NO_ASISTIO || target == COMPLETADA;
      case CONFIRMADA -> target == PROGRAMADA || target == CANCELADA || target == NO_ASISTIO || target == COMPLETADA;
      case CANCELADA, COMPLETADA, NO_ASISTIO -> false;
    };
  }

  public String label() {
    return switch (this) {
      case PROGRAMADA -> "Programada";
      case CONFIRMADA -> "Confirmada";
      case CANCELADA -> "Cancelada";
      case COMPLETADA -> "Atendida";
      case NO_ASISTIO -> "No asistió";
    };
  }
}
