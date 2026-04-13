CREATE TABLE IF NOT EXISTS clinical_allergies (
  id BIGSERIAL PRIMARY KEY,
  patient_id BIGINT NOT NULL REFERENCES patients(id) ON DELETE CASCADE,
  allergen VARCHAR(200) NOT NULL,
  reaction VARCHAR(200),
  severity VARCHAR(20) NOT NULL DEFAULT 'UNKNOWN',
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  notes TEXT,
  created_by VARCHAR(120),
  updated_by VARCHAR(120),
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS clinical_conditions (
  id BIGSERIAL PRIMARY KEY,
  patient_id BIGINT NOT NULL REFERENCES patients(id) ON DELETE CASCADE,
  name VARCHAR(200) NOT NULL,
  icd10_code VARCHAR(20),
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  onset_date DATE,
  notes TEXT,
  created_by VARCHAR(120),
  updated_by VARCHAR(120),
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS clinical_medications (
  id BIGSERIAL PRIMARY KEY,
  patient_id BIGINT NOT NULL REFERENCES patients(id) ON DELETE CASCADE,
  name VARCHAR(200) NOT NULL,
  dosage VARCHAR(60),
  frequency VARCHAR(60),
  start_date DATE,
  end_date DATE,
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  notes TEXT,
  created_by VARCHAR(120),
  updated_by VARCHAR(120),
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS clinical_vitals (
  id BIGSERIAL PRIMARY KEY,
  patient_id BIGINT NOT NULL REFERENCES patients(id) ON DELETE CASCADE,
  recorded_at TIMESTAMP NOT NULL DEFAULT NOW(),
  weight_kg NUMERIC(6,2),
  height_cm NUMERIC(6,2),
  temperature_c NUMERIC(4,1),
  heart_rate INT,
  resp_rate INT,
  blood_pressure VARCHAR(20),
  oxygen_saturation INT,
  notes TEXT,
  created_by VARCHAR(120),
  created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS clinical_notes (
  id BIGSERIAL PRIMARY KEY,
  patient_id BIGINT NOT NULL REFERENCES patients(id) ON DELETE CASCADE,
  title VARCHAR(200) NOT NULL,
  note TEXT NOT NULL,
  recorded_at TIMESTAMP NOT NULL DEFAULT NOW(),
  created_by VARCHAR(120),
  updated_by VARCHAR(120),
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_clinical_allergies_patient ON clinical_allergies(patient_id);
CREATE INDEX IF NOT EXISTS idx_clinical_allergies_status ON clinical_allergies(status);
CREATE INDEX IF NOT EXISTS idx_clinical_conditions_patient ON clinical_conditions(patient_id);
CREATE INDEX IF NOT EXISTS idx_clinical_conditions_status ON clinical_conditions(status);
CREATE INDEX IF NOT EXISTS idx_clinical_medications_patient ON clinical_medications(patient_id);
CREATE INDEX IF NOT EXISTS idx_clinical_medications_status ON clinical_medications(status);
CREATE INDEX IF NOT EXISTS idx_clinical_vitals_patient ON clinical_vitals(patient_id);
CREATE INDEX IF NOT EXISTS idx_clinical_notes_patient ON clinical_notes(patient_id);
