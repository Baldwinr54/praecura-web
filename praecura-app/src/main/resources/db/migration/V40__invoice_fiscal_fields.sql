ALTER TABLE invoices
  ADD COLUMN IF NOT EXISTS invoice_type VARCHAR(20) NOT NULL DEFAULT 'INVOICE',
  ADD COLUMN IF NOT EXISTS ncf_type VARCHAR(10),
  ADD COLUMN IF NOT EXISTS ncf VARCHAR(20),
  ADD COLUMN IF NOT EXISTS ncf_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  ADD COLUMN IF NOT EXISTS ncf_issued_at TIMESTAMP,
  ADD COLUMN IF NOT EXISTS fiscal_name VARCHAR(160),
  ADD COLUMN IF NOT EXISTS fiscal_tax_id VARCHAR(20),
  ADD COLUMN IF NOT EXISTS fiscal_address VARCHAR(200),
  ADD COLUMN IF NOT EXISTS credit_note_of BIGINT;

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM pg_constraint
    WHERE conname = 'fk_invoices_credit_note'
  ) THEN
    ALTER TABLE invoices
      ADD CONSTRAINT fk_invoices_credit_note
      FOREIGN KEY (credit_note_of) REFERENCES invoices(id) ON DELETE SET NULL;
  END IF;
END
$$;

CREATE INDEX IF NOT EXISTS idx_invoices_ncf ON invoices(ncf);
CREATE INDEX IF NOT EXISTS idx_invoices_type ON invoices(invoice_type);

ALTER TABLE patients
  ADD COLUMN IF NOT EXISTS billing_name VARCHAR(160),
  ADD COLUMN IF NOT EXISTS billing_tax_id VARCHAR(20),
  ADD COLUMN IF NOT EXISTS billing_address VARCHAR(200);
