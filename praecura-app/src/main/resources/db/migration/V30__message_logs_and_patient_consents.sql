ALTER TABLE patients
  ADD COLUMN IF NOT EXISTS consent_sms boolean NOT NULL DEFAULT false,
  ADD COLUMN IF NOT EXISTS consent_email boolean NOT NULL DEFAULT false,
  ADD COLUMN IF NOT EXISTS consent_whatsapp boolean NOT NULL DEFAULT false,
  ADD COLUMN IF NOT EXISTS preferred_channel varchar(20);

CREATE TABLE IF NOT EXISTS message_logs (
  id bigserial PRIMARY KEY,
  created_at timestamp NOT NULL DEFAULT now(),
  channel varchar(20) NOT NULL,
  status varchar(20) NOT NULL,
  to_address varchar(160) NOT NULL,
  subject varchar(160),
  body text,
  error text,
  appointment_id bigint,
  patient_id bigint,
  username varchar(120),
  request_id varchar(120),
  ip_address varchar(80),
  user_agent varchar(255),
  CONSTRAINT fk_message_logs_appointment FOREIGN KEY (appointment_id) REFERENCES appointments(id) ON DELETE SET NULL,
  CONSTRAINT fk_message_logs_patient FOREIGN KEY (patient_id) REFERENCES patients(id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_message_logs_created_at ON message_logs(created_at);
CREATE INDEX IF NOT EXISTS idx_message_logs_channel ON message_logs(channel);
CREATE INDEX IF NOT EXISTS idx_message_logs_status ON message_logs(status);
