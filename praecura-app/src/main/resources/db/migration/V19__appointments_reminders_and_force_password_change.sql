-- Recordatorios simples por cita + flag de cambio de contraseña

ALTER TABLE appointments
  ADD COLUMN IF NOT EXISTS reminded_at TIMESTAMP NULL;

ALTER TABLE users
  ADD COLUMN IF NOT EXISTS force_password_change BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX IF NOT EXISTS idx_appointments_reminded_at ON appointments(reminded_at);
