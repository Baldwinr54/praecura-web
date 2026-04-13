-- V5: Corrige el CHECK antiguo de estados de citas (V2: ck_appointments_status)
DO $$
BEGIN
  IF EXISTS (
    SELECT 1 FROM information_schema.table_constraints
    WHERE table_name='appointments'
      AND constraint_type='CHECK'
      AND constraint_name='ck_appointments_status'
  ) THEN
    ALTER TABLE appointments DROP CONSTRAINT ck_appointments_status;
  END IF;
END $$;
