CREATE TABLE IF NOT EXISTS electronic_fiscal_documents (
  id BIGSERIAL PRIMARY KEY,
  invoice_id BIGINT NOT NULL,
  e_ncf VARCHAR(25),
  status VARCHAR(40) NOT NULL DEFAULT 'PENDING',
  security_code VARCHAR(120),
  verification_url VARCHAR(500),
  dgii_track_id VARCHAR(120),
  dgii_status_code VARCHAR(60),
  dgii_message VARCHAR(1000),
  signed_xml TEXT,
  sent_at TIMESTAMP,
  accepted_at TIMESTAMP,
  last_checked_at TIMESTAMP,
  created_by VARCHAR(120),
  updated_by VARCHAR(120),
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint WHERE conname = 'fk_ecf_invoice'
  ) THEN
    ALTER TABLE electronic_fiscal_documents
      ADD CONSTRAINT fk_ecf_invoice
      FOREIGN KEY (invoice_id) REFERENCES invoices(id) ON DELETE CASCADE;
  END IF;
END
$$;

CREATE UNIQUE INDEX IF NOT EXISTS uq_ecf_invoice_id ON electronic_fiscal_documents(invoice_id);
CREATE INDEX IF NOT EXISTS idx_ecf_status ON electronic_fiscal_documents(status);
CREATE INDEX IF NOT EXISTS idx_ecf_track_id ON electronic_fiscal_documents(dgii_track_id);
CREATE INDEX IF NOT EXISTS idx_ecf_enfc ON electronic_fiscal_documents(e_ncf);
