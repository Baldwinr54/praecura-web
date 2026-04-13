-- Add patient extra fields for contact + quick flags/notes
ALTER TABLE patients ADD COLUMN IF NOT EXISTS email VARCHAR(160);
ALTER TABLE patients ADD COLUMN IF NOT EXISTS flags VARCHAR(220);
ALTER TABLE patients ADD COLUMN IF NOT EXISTS notes TEXT;

-- Helper indexes for search/filter (avoid unique constraints to prevent migration failures on existing data)
CREATE INDEX IF NOT EXISTS idx_patients_full_name_ci ON patients (lower(full_name));
CREATE INDEX IF NOT EXISTS idx_patients_cedula_ci ON patients (lower(cedula));
CREATE INDEX IF NOT EXISTS idx_patients_email_ci ON patients (lower(email));
CREATE INDEX IF NOT EXISTS idx_patients_phone_ci ON patients (lower(phone));
