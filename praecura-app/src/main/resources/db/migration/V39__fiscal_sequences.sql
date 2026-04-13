CREATE TABLE IF NOT EXISTS fiscal_sequences (
  id BIGSERIAL PRIMARY KEY,
  type_code VARCHAR(10) NOT NULL UNIQUE,
  description VARCHAR(160),
  start_number BIGINT NOT NULL,
  end_number BIGINT,
  next_number BIGINT NOT NULL,
  number_length INT NOT NULL DEFAULT 8,
  active BOOLEAN NOT NULL DEFAULT TRUE,
  expires_at DATE,
  created_by VARCHAR(120),
  updated_by VARCHAR(120),
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_fiscal_sequences_active ON fiscal_sequences(active);
