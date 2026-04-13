-- V22: Catálogo de especialidades (FK opcional) en doctors y relación doctor-servicios

-- 1) Agregar columna specialty_id (FK opcional)
ALTER TABLE doctors
  ADD COLUMN IF NOT EXISTS specialty_id BIGINT;

-- 2) Backfill specialty_id a partir del texto legacy (si coincide por nombre)
UPDATE doctors d
SET specialty_id = s.id
FROM specialties s
WHERE d.specialty_id IS NULL
  AND d.specialty IS NOT NULL
  AND LOWER(TRIM(d.specialty)) = LOWER(TRIM(s.name));

-- 3) Crear FK (idempotente)
DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.table_constraints
    WHERE constraint_name = 'fk_doctors_specialty'
      AND table_name = 'doctors'
  ) THEN
    ALTER TABLE doctors
      ADD CONSTRAINT fk_doctors_specialty
      FOREIGN KEY (specialty_id) REFERENCES specialties(id);
  END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_doctors_specialty_id ON doctors(specialty_id);

-- 4) Join table doctor_services
CREATE TABLE IF NOT EXISTS doctor_services (
  doctor_id BIGINT NOT NULL,
  service_id BIGINT NOT NULL,
  PRIMARY KEY (doctor_id, service_id)
);

-- 5) FKs para join table (idempotentes)
DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.table_constraints
    WHERE constraint_name = 'fk_doctor_services_doctor'
      AND table_name = 'doctor_services'
  ) THEN
    ALTER TABLE doctor_services
      ADD CONSTRAINT fk_doctor_services_doctor
      FOREIGN KEY (doctor_id) REFERENCES doctors(id) ON DELETE CASCADE;
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.table_constraints
    WHERE constraint_name = 'fk_doctor_services_service'
      AND table_name = 'doctor_services'
  ) THEN
    ALTER TABLE doctor_services
      ADD CONSTRAINT fk_doctor_services_service
      FOREIGN KEY (service_id) REFERENCES medical_services(id) ON DELETE CASCADE;
  END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_doctor_services_service_id ON doctor_services(service_id);

-- 6) Seed: si existe 1 médico y 1 servicio y no hay relación aún, crea relación por defecto
INSERT INTO doctor_services(doctor_id, service_id)
SELECT d.id, s.id
FROM doctors d
CROSS JOIN medical_services s
WHERE d.active = true AND s.active = true
  AND NOT EXISTS (
    SELECT 1 FROM doctor_services ds WHERE ds.doctor_id = d.id AND ds.service_id = s.id
  )
  AND d.id = (SELECT id FROM doctors ORDER BY id ASC LIMIT 1)
  AND s.id = (SELECT id FROM medical_services ORDER BY id ASC LIMIT 1);
