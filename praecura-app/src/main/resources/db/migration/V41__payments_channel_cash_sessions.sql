CREATE TABLE IF NOT EXISTS cash_sessions (
  id BIGSERIAL PRIMARY KEY,
  status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
  opening_amount NUMERIC(12,2) NOT NULL DEFAULT 0,
  closing_amount NUMERIC(12,2),
  opened_at TIMESTAMP NOT NULL DEFAULT NOW(),
  closed_at TIMESTAMP,
  opened_by VARCHAR(120),
  closed_by VARCHAR(120),
  notes VARCHAR(500)
);

ALTER TABLE payments
  ADD COLUMN IF NOT EXISTS channel VARCHAR(20) NOT NULL DEFAULT 'IN_PERSON',
  ADD COLUMN IF NOT EXISTS card_brand VARCHAR(40),
  ADD COLUMN IF NOT EXISTS terminal_id VARCHAR(60),
  ADD COLUMN IF NOT EXISTS batch_id VARCHAR(60),
  ADD COLUMN IF NOT EXISTS rrn VARCHAR(60),
  ADD COLUMN IF NOT EXISTS cash_session_id BIGINT,
  ADD COLUMN IF NOT EXISTS refund_session_id BIGINT;

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM pg_constraint
    WHERE conname = 'fk_payments_cash_session'
  ) THEN
    ALTER TABLE payments
      ADD CONSTRAINT fk_payments_cash_session
      FOREIGN KEY (cash_session_id) REFERENCES cash_sessions(id) ON DELETE SET NULL;
  END IF;
END
$$;

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM pg_constraint
    WHERE conname = 'fk_payments_refund_session'
  ) THEN
    ALTER TABLE payments
      ADD CONSTRAINT fk_payments_refund_session
      FOREIGN KEY (refund_session_id) REFERENCES cash_sessions(id) ON DELETE SET NULL;
  END IF;
END
$$;

CREATE INDEX IF NOT EXISTS idx_payments_cash_session ON payments(cash_session_id);
CREATE INDEX IF NOT EXISTS idx_payments_refund_session ON payments(refund_session_id);
