-- Refund tracking fields and indexes
ALTER TABLE payments
  ADD COLUMN IF NOT EXISTS refund_method VARCHAR(20),
  ADD COLUMN IF NOT EXISTS refund_reference VARCHAR(120);

CREATE INDEX IF NOT EXISTS idx_payments_refunded_at ON payments(refunded_at);
