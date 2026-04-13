-- Asegura que ninguna cita existente quede sin estado visible
UPDATE appointments
SET status = 'CREADA'
WHERE status IS NULL OR status = '';

-- Establece default para futuras inserciones
ALTER TABLE appointments
  ALTER COLUMN status SET DEFAULT 'CREADA';

-- Obliga a que siempre exista un estado
ALTER TABLE appointments
  ALTER COLUMN status SET NOT NULL;
