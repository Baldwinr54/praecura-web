ALTER TABLE electronic_fiscal_documents
  ADD COLUMN IF NOT EXISTS attempt_count INT NOT NULL DEFAULT 0,
  ADD COLUMN IF NOT EXISTS last_attempt_at TIMESTAMP,
  ADD COLUMN IF NOT EXISTS next_retry_at TIMESTAMP,
  ADD COLUMN IF NOT EXISTS last_error VARCHAR(1000);

CREATE INDEX IF NOT EXISTS idx_ecf_next_retry ON electronic_fiscal_documents(next_retry_at);
CREATE INDEX IF NOT EXISTS idx_ecf_updated_at ON electronic_fiscal_documents(updated_at);
