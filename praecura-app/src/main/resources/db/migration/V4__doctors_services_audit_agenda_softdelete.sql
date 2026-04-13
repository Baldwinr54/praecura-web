-- V4: Médicos, Servicios, Auditoría, Soft delete y mejoras de Agenda/Citas

-- 1) Soft delete en pacientes
ALTER TABLE patients
  ADD COLUMN IF NOT EXISTS active BOOLEAN NOT NULL DEFAULT TRUE;

CREATE INDEX IF NOT EXISTS idx_patients_active ON patients(active);

-- 2) Tabla de médicos
CREATE TABLE IF NOT EXISTS doctors (
  id BIGSERIAL PRIMARY KEY,
  full_name VARCHAR(120) NOT NULL,
  specialty VARCHAR(120),
  license_no VARCHAR(60),
  phone VARCHAR(30),
  active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE INDEX IF NOT EXISTS idx_doctors_active ON doctors(active);
CREATE INDEX IF NOT EXISTS idx_doctors_full_name ON doctors(full_name);

-- 3) Tabla de servicios
CREATE TABLE IF NOT EXISTS medical_services (
  id BIGSERIAL PRIMARY KEY,
  name VARCHAR(120) NOT NULL,
  duration_minutes INT NOT NULL,
  price NUMERIC(12,2),
  active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE INDEX IF NOT EXISTS idx_services_active ON medical_services(active);
CREATE INDEX IF NOT EXISTS idx_services_name ON medical_services(name);

-- 4) Auditoría
CREATE TABLE IF NOT EXISTS audit_logs (
  id BIGSERIAL PRIMARY KEY,
  username VARCHAR(60),
  action VARCHAR(60) NOT NULL,
  entity VARCHAR(60) NOT NULL,
  entity_id BIGINT,
  detail VARCHAR(500),
  created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_audit_created_at ON audit_logs(created_at);
CREATE INDEX IF NOT EXISTS idx_audit_entity ON audit_logs(entity, entity_id);

-- 5) Evolución de appointments
ALTER TABLE appointments
  ADD COLUMN IF NOT EXISTS doctor_id BIGINT;

ALTER TABLE appointments
  ADD COLUMN IF NOT EXISTS service_id BIGINT;

ALTER TABLE appointments
  ADD COLUMN IF NOT EXISTS duration_minutes INT NOT NULL DEFAULT 30;

ALTER TABLE appointments
  ADD COLUMN IF NOT EXISTS notes VARCHAR(500);

-- 5.1) Seeds idempotentes: doctor y service por defecto
INSERT INTO doctors(full_name, specialty, license_no, phone, active)
SELECT 'Médico General', 'General', 'GEN-000', NULL, TRUE
WHERE NOT EXISTS (SELECT 1 FROM doctors);

INSERT INTO medical_services(name, duration_minutes, price, active)
SELECT 'Consulta', 30, NULL, TRUE
WHERE NOT EXISTS (SELECT 1 FROM medical_services);

-- 5.2) Backfill doctor/service en citas existentes
UPDATE appointments
SET doctor_id = (SELECT id FROM doctors ORDER BY id ASC LIMIT 1)
WHERE doctor_id IS NULL;

UPDATE appointments
SET service_id = (SELECT id FROM medical_services ORDER BY id ASC LIMIT 1)
WHERE service_id IS NULL;

-- 5.3) Enforce FK (si no existen)
DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.table_constraints
    WHERE constraint_name = 'fk_appointments_doctor'
      AND table_name = 'appointments'
  ) THEN
    ALTER TABLE appointments
      ADD CONSTRAINT fk_appointments_doctor
      FOREIGN KEY (doctor_id) REFERENCES doctors(id);
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.table_constraints
    WHERE constraint_name = 'fk_appointments_service'
      AND table_name = 'appointments'
  ) THEN
    ALTER TABLE appointments
      ADD CONSTRAINT fk_appointments_service
      FOREIGN KEY (service_id) REFERENCES medical_services(id);
  END IF;
END $$;

-- 5.4) doctor_id obligatorio
ALTER TABLE appointments
  ALTER COLUMN doctor_id SET NOT NULL;

-- 5.5) Migración de estados antiguos
UPDATE appointments SET status = 'PROGRAMADA' WHERE status = 'CREADA';
UPDATE appointments SET status = 'COMPLETADA' WHERE status = 'ATENDIDA';

-- Si por cualquier razón queda vacío
UPDATE appointments SET status = 'PROGRAMADA' WHERE status IS NULL OR status = '';

-- 5.6) Default y validación de estados
ALTER TABLE appointments
  ALTER COLUMN status SET DEFAULT 'PROGRAMADA';

-- Cambiar el tipo de status sigue siendo VARCHAR (por diseño), pero aseguramos CHECK
DO $$
BEGIN
  IF EXISTS (
    SELECT 1 FROM information_schema.table_constraints
    WHERE table_name='appointments' AND constraint_type='CHECK'
      AND constraint_name='chk_appointments_status'
  ) THEN
    ALTER TABLE appointments DROP CONSTRAINT chk_appointments_status;
  END IF;
END $$;

ALTER TABLE appointments
  ADD CONSTRAINT chk_appointments_status
  CHECK (status IN ('PROGRAMADA','CONFIRMADA','CANCELADA','COMPLETADA','NO_ASISTIO'));

CREATE INDEX IF NOT EXISTS idx_appointments_doctor ON appointments(doctor_id);
CREATE INDEX IF NOT EXISTS idx_appointments_service ON appointments(service_id);
CREATE INDEX IF NOT EXISTS idx_appointments_doctor_date ON appointments(doctor_id, scheduled_at);
