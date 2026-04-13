CREATE TABLE IF NOT EXISTS receivable_commitments (
  id BIGSERIAL PRIMARY KEY,
  invoice_id BIGINT NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  promised_amount NUMERIC(12,2) NOT NULL DEFAULT 0,
  promised_date TIMESTAMP NOT NULL,
  notes VARCHAR(500),
  created_by VARCHAR(120),
  updated_by VARCHAR(120),
  created_at TIMESTAMP NOT NULL DEFAULT now(),
  updated_at TIMESTAMP NOT NULL DEFAULT now(),
  fulfilled_at TIMESTAMP,
  canceled_at TIMESTAMP,
  broken_at TIMESTAMP
);

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint WHERE conname = 'fk_receivable_commitments_invoice'
  ) THEN
    ALTER TABLE receivable_commitments
      ADD CONSTRAINT fk_receivable_commitments_invoice
      FOREIGN KEY (invoice_id)
      REFERENCES invoices(id);
  END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_receivable_commitments_invoice ON receivable_commitments(invoice_id);
CREATE INDEX IF NOT EXISTS idx_receivable_commitments_status ON receivable_commitments(status);
CREATE INDEX IF NOT EXISTS idx_receivable_commitments_promised_date ON receivable_commitments(promised_date);
