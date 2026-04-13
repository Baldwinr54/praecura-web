-- Unicidad (opcional pero recomendable)
ALTER TABLE patients
  ADD CONSTRAINT uq_patients_cedula UNIQUE (cedula);

-- Evitar estados inválidos
ALTER TABLE appointments
  ADD CONSTRAINT ck_appointments_status
  CHECK (status IN ('CREADA','CONFIRMADA','ATENDIDA','CANCELADA'));

-- Índice útil para queries por rol
CREATE INDEX IF NOT EXISTS idx_users_role_id ON users(role_id);
