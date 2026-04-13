-- Soft delete para citas (archivar) - agrega columna active
-- Nota: se deja DEFAULT true para mantener compatibilidad con datos existentes.

ALTER TABLE appointments
  ADD COLUMN IF NOT EXISTS active BOOLEAN NOT NULL DEFAULT TRUE;

CREATE INDEX IF NOT EXISTS idx_appointments_active ON appointments(active);

-- Acelera agenda / dashboard filtrando por médico y fecha.
-- Índice parcial solo para citas activas.
CREATE INDEX IF NOT EXISTS idx_appointments_doctor_sched_active
  ON appointments(doctor_id, scheduled_at)
  WHERE active = TRUE;
