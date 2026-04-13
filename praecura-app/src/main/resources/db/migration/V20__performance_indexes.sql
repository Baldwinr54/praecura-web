-- Índices recomendados para búsquedas frecuentes (versión inicial)
-- (No todos los LIKE '%q%' aprovechan índices; esto cubre filtros y búsquedas exactas frecuentes)

DO $$
BEGIN
  -- Patients
  IF NOT EXISTS (SELECT 1 FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace WHERE c.relname = 'idx_patients_cedula' AND n.nspname = 'public') THEN
    CREATE INDEX idx_patients_cedula ON patients(cedula);
  END IF;

  IF NOT EXISTS (SELECT 1 FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace WHERE c.relname = 'idx_patients_phone' AND n.nspname = 'public') THEN
    CREATE INDEX idx_patients_phone ON patients(phone);
  END IF;

  IF NOT EXISTS (SELECT 1 FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace WHERE c.relname = 'idx_patients_full_name' AND n.nspname = 'public') THEN
    CREATE INDEX idx_patients_full_name ON patients(full_name);
  END IF;

  -- Appointments
  IF NOT EXISTS (SELECT 1 FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace WHERE c.relname = 'idx_appointments_doctor_scheduled' AND n.nspname = 'public') THEN
    CREATE INDEX idx_appointments_doctor_scheduled ON appointments(doctor_id, scheduled_at);
  END IF;

  IF NOT EXISTS (SELECT 1 FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace WHERE c.relname = 'idx_appointments_doctor_status' AND n.nspname = 'public') THEN
    CREATE INDEX idx_appointments_doctor_status ON appointments(doctor_id, status);
  END IF;

  -- Doctors
  IF NOT EXISTS (SELECT 1 FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace WHERE c.relname = 'idx_doctors_license_no' AND n.nspname = 'public') THEN
    CREATE INDEX idx_doctors_license_no ON doctors(license_no);
  END IF;

  IF NOT EXISTS (SELECT 1 FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace WHERE c.relname = 'idx_doctors_phone' AND n.nspname = 'public') THEN
    CREATE INDEX idx_doctors_phone ON doctors(phone);
  END IF;
END$$;
