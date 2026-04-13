-- Ensure one invoice per appointment (when appointment_id is present)
CREATE UNIQUE INDEX IF NOT EXISTS uq_invoices_appointment_id ON invoices(appointment_id);

-- Helpful index for payment status queries
CREATE INDEX IF NOT EXISTS idx_payments_status ON payments(status);
