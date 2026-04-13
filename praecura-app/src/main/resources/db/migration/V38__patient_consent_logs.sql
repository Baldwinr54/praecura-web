CREATE TABLE IF NOT EXISTS patient_consent_logs (
  id BIGSERIAL PRIMARY KEY,
  patient_id BIGINT NOT NULL REFERENCES patients(id) ON DELETE CASCADE,
  consent_type VARCHAR(30) NOT NULL,
  granted BOOLEAN NOT NULL,
  source VARCHAR(40),
  captured_by VARCHAR(120),
  captured_at TIMESTAMP NOT NULL DEFAULT now(),
  ip_address VARCHAR(80),
  user_agent VARCHAR(255),
  notes TEXT
);

CREATE INDEX IF NOT EXISTS idx_patient_consent_logs_patient ON patient_consent_logs(patient_id);
CREATE INDEX IF NOT EXISTS idx_patient_consent_logs_captured_at ON patient_consent_logs(captured_at);
