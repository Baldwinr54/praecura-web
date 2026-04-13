CREATE TABLE IF NOT EXISTS payment_links (
  id BIGSERIAL PRIMARY KEY,
  invoice_id BIGINT NOT NULL REFERENCES invoices(id) ON DELETE CASCADE,
  provider VARCHAR(30) NOT NULL,
  status VARCHAR(20) NOT NULL,
  amount NUMERIC(12,2) NOT NULL DEFAULT 0,
  currency CHAR(3) NOT NULL DEFAULT 'USD',
  url TEXT,
  external_id VARCHAR(120),
  session_id VARCHAR(120),
  session_key VARCHAR(120),
  notes VARCHAR(500),
  created_by VARCHAR(120),
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  expires_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_payment_links_invoice ON payment_links(invoice_id);
CREATE INDEX IF NOT EXISTS idx_payment_links_status ON payment_links(status);
CREATE INDEX IF NOT EXISTS idx_payment_links_provider ON payment_links(provider);
