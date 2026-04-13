CREATE TABLE IF NOT EXISTS billing_charges (
  id BIGSERIAL PRIMARY KEY,
  patient_id BIGINT NOT NULL,
  appointment_id BIGINT,
  service_id BIGINT,
  invoice_id BIGINT,
  category VARCHAR(30) NOT NULL DEFAULT 'CONSULTATION',
  description VARCHAR(220) NOT NULL,
  quantity INT NOT NULL DEFAULT 1,
  unit_price NUMERIC(12,2) NOT NULL DEFAULT 0,
  subtotal NUMERIC(12,2) NOT NULL DEFAULT 0,
  tax NUMERIC(12,2) NOT NULL DEFAULT 0,
  discount NUMERIC(12,2) NOT NULL DEFAULT 0,
  total NUMERIC(12,2) NOT NULL DEFAULT 0,
  currency VARCHAR(3) NOT NULL DEFAULT 'DOP',
  status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
  source VARCHAR(30) NOT NULL DEFAULT 'MANUAL',
  notes VARCHAR(500),
  performed_at TIMESTAMP,
  created_by VARCHAR(120),
  updated_by VARCHAR(120),
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  CONSTRAINT chk_billing_charges_quantity_gt_zero CHECK (quantity > 0),
  CONSTRAINT chk_billing_charges_amounts_non_negative CHECK (
      unit_price >= 0 AND subtotal >= 0 AND tax >= 0 AND discount >= 0 AND total >= 0
  )
);

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint WHERE conname = 'fk_billing_charges_patient'
  ) THEN
    ALTER TABLE billing_charges
      ADD CONSTRAINT fk_billing_charges_patient
      FOREIGN KEY (patient_id) REFERENCES patients(id) ON DELETE RESTRICT;
  END IF;
END
$$;

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint WHERE conname = 'fk_billing_charges_appointment'
  ) THEN
    ALTER TABLE billing_charges
      ADD CONSTRAINT fk_billing_charges_appointment
      FOREIGN KEY (appointment_id) REFERENCES appointments(id) ON DELETE SET NULL;
  END IF;
END
$$;

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint WHERE conname = 'fk_billing_charges_service'
  ) THEN
    ALTER TABLE billing_charges
      ADD CONSTRAINT fk_billing_charges_service
      FOREIGN KEY (service_id) REFERENCES medical_services(id) ON DELETE SET NULL;
  END IF;
END
$$;

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint WHERE conname = 'fk_billing_charges_invoice'
  ) THEN
    ALTER TABLE billing_charges
      ADD CONSTRAINT fk_billing_charges_invoice
      FOREIGN KEY (invoice_id) REFERENCES invoices(id) ON DELETE SET NULL;
  END IF;
END
$$;

CREATE INDEX IF NOT EXISTS idx_billing_charges_status ON billing_charges(status);
CREATE INDEX IF NOT EXISTS idx_billing_charges_created_at ON billing_charges(created_at);
CREATE INDEX IF NOT EXISTS idx_billing_charges_patient_status ON billing_charges(patient_id, status);
CREATE INDEX IF NOT EXISTS idx_billing_charges_appointment_status ON billing_charges(appointment_id, status);
CREATE INDEX IF NOT EXISTS idx_billing_charges_invoice ON billing_charges(invoice_id);
