-- Agrega holgura/buffer (en minutos) a nivel de médico para separar citas
ALTER TABLE doctors
  ADD COLUMN IF NOT EXISTS buffer_minutes INTEGER NOT NULL DEFAULT 5;

COMMENT ON COLUMN doctors.buffer_minutes IS 'Holgura/buffer (minutos) entre citas para el mismo medico.';
