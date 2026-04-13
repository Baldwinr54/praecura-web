-- Data-quality + compliance hardening for billing, pharmacy and insurance modules.
-- Focus:
-- 1) Enforce non-negative/valid ranges at DB level.
-- 2) Tighten payment traces to PCI-friendly storage rules (no full PAN; only last4 format).
-- 3) Keep migration idempotent for existing environments.

UPDATE invoices
SET currency = upper(currency)
WHERE currency IS NOT NULL AND currency <> upper(currency);

UPDATE payments
SET currency = upper(currency)
WHERE currency IS NOT NULL AND currency <> upper(currency);

UPDATE payment_links
SET currency = upper(currency)
WHERE currency IS NOT NULL AND currency <> upper(currency);

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint WHERE conname = 'chk_invoice_items_quantity_positive'
  ) THEN
    ALTER TABLE invoice_items
      ADD CONSTRAINT chk_invoice_items_quantity_positive
      CHECK (quantity > 0);
  END IF;
END
$$;

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint WHERE conname = 'chk_invoice_items_amounts_non_negative'
  ) THEN
    ALTER TABLE invoice_items
      ADD CONSTRAINT chk_invoice_items_amounts_non_negative
      CHECK (unit_price >= 0 AND total >= 0);
  END IF;
END
$$;

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint WHERE conname = 'chk_payments_amount_positive'
  ) THEN
    ALTER TABLE payments
      ADD CONSTRAINT chk_payments_amount_positive
      CHECK (amount > 0);
  END IF;
END
$$;

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint WHERE conname = 'chk_payments_refund_range'
  ) THEN
    ALTER TABLE payments
      ADD CONSTRAINT chk_payments_refund_range
      CHECK (refunded_amount >= 0 AND refunded_amount <= amount);
  END IF;
END
$$;

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint WHERE conname = 'chk_payments_last4_format'
  ) THEN
    ALTER TABLE payments
      ADD CONSTRAINT chk_payments_last4_format
      CHECK (last4 IS NULL OR last4 ~ '^[0-9]{4}$');
  END IF;
END
$$;

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint WHERE conname = 'chk_payments_cash_values'
  ) THEN
    ALTER TABLE payments
      ADD CONSTRAINT chk_payments_cash_values
      CHECK (
        (cash_received IS NULL OR cash_received >= 0) AND
        (cash_change IS NULL OR cash_change >= 0)
      );
  END IF;
END
$$;

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint WHERE conname = 'chk_payments_cash_received_min'
  ) THEN
    ALTER TABLE payments
      ADD CONSTRAINT chk_payments_cash_received_min
      CHECK (method <> 'CASH' OR cash_received IS NULL OR cash_received >= amount);
  END IF;
END
$$;

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint WHERE conname = 'chk_payments_currency_iso3'
  ) THEN
    ALTER TABLE payments
      ADD CONSTRAINT chk_payments_currency_iso3
      CHECK (currency ~ '^[A-Z]{3}$');
  END IF;
END
$$;

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint WHERE conname = 'chk_payment_links_amount_non_negative'
  ) THEN
    ALTER TABLE payment_links
      ADD CONSTRAINT chk_payment_links_amount_non_negative
      CHECK (amount >= 0);
  END IF;
END
$$;

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint WHERE conname = 'chk_payment_links_currency_iso3'
  ) THEN
    ALTER TABLE payment_links
      ADD CONSTRAINT chk_payment_links_currency_iso3
      CHECK (currency ~ '^[A-Z]{3}$');
  END IF;
END
$$;

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint WHERE conname = 'chk_insurance_plans_percentages'
  ) THEN
    ALTER TABLE insurance_plans
      ADD CONSTRAINT chk_insurance_plans_percentages
      CHECK (
        coverage_percent >= 0 AND coverage_percent <= 100 AND
        copay_percent >= 0 AND copay_percent <= 100
      );
  END IF;
END
$$;

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint WHERE conname = 'chk_insurance_plans_deductible_non_negative'
  ) THEN
    ALTER TABLE insurance_plans
      ADD CONSTRAINT chk_insurance_plans_deductible_non_negative
      CHECK (deductible_amount >= 0);
  END IF;
END
$$;

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint WHERE conname = 'chk_insurance_authorizations_amounts'
  ) THEN
    ALTER TABLE insurance_authorizations
      ADD CONSTRAINT chk_insurance_authorizations_amounts
      CHECK (requested_amount >= 0 AND approved_amount >= 0);
  END IF;
END
$$;

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint WHERE conname = 'chk_insurance_claims_amounts'
  ) THEN
    ALTER TABLE insurance_claims
      ADD CONSTRAINT chk_insurance_claims_amounts
      CHECK (
        claimed_amount >= 0 AND
        approved_amount >= 0 AND
        denied_amount >= 0
      );
  END IF;
END
$$;

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint WHERE conname = 'chk_clinical_orders_cost_non_negative'
  ) THEN
    ALTER TABLE clinical_orders
      ADD CONSTRAINT chk_clinical_orders_cost_non_negative
      CHECK (cost_estimate >= 0);
  END IF;
END
$$;

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint WHERE conname = 'chk_surgery_estimated_minutes_positive'
  ) THEN
    ALTER TABLE surgery_schedules
      ADD CONSTRAINT chk_surgery_estimated_minutes_positive
      CHECK (estimated_minutes > 0);
  END IF;
END
$$;

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint WHERE conname = 'chk_inventory_pricing_non_negative'
  ) THEN
    ALTER TABLE inventory_items
      ADD CONSTRAINT chk_inventory_pricing_non_negative
      CHECK (
        cost_price >= 0 AND
        sale_price >= 0 AND
        min_stock >= 0
      );
  END IF;
END
$$;

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint WHERE conname = 'chk_inventory_tax_rate_range'
  ) THEN
    ALTER TABLE inventory_items
      ADD CONSTRAINT chk_inventory_tax_rate_range
      CHECK (tax_rate >= 0 AND tax_rate <= 100);
  END IF;
END
$$;

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint WHERE conname = 'chk_stock_movement_quantity_positive'
  ) THEN
    ALTER TABLE stock_movements
      ADD CONSTRAINT chk_stock_movement_quantity_positive
      CHECK (quantity > 0);
  END IF;
END
$$;

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint WHERE conname = 'chk_stock_movement_unit_cost_non_negative'
  ) THEN
    ALTER TABLE stock_movements
      ADD CONSTRAINT chk_stock_movement_unit_cost_non_negative
      CHECK (unit_cost >= 0);
  END IF;
END
$$;

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint WHERE conname = 'chk_purchase_order_items_quantity_positive'
  ) THEN
    ALTER TABLE purchase_order_items
      ADD CONSTRAINT chk_purchase_order_items_quantity_positive
      CHECK (quantity > 0 AND received_quantity >= 0);
  END IF;
END
$$;

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint WHERE conname = 'chk_purchase_order_items_amounts_non_negative'
  ) THEN
    ALTER TABLE purchase_order_items
      ADD CONSTRAINT chk_purchase_order_items_amounts_non_negative
      CHECK (
        unit_cost >= 0 AND
        tax_rate >= 0 AND tax_rate <= 100 AND
        total >= 0
      );
  END IF;
END
$$;

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint WHERE conname = 'chk_dispensation_items_quantity_positive'
  ) THEN
    ALTER TABLE pharmacy_dispensation_items
      ADD CONSTRAINT chk_dispensation_items_quantity_positive
      CHECK (quantity > 0);
  END IF;
END
$$;

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint WHERE conname = 'chk_dispensation_items_amounts_non_negative'
  ) THEN
    ALTER TABLE pharmacy_dispensation_items
      ADD CONSTRAINT chk_dispensation_items_amounts_non_negative
      CHECK (unit_price >= 0 AND total >= 0);
  END IF;
END
$$;

CREATE INDEX IF NOT EXISTS idx_payments_external_id_nonnull
  ON payments(external_id)
  WHERE external_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_payments_invoice_status_created
  ON payments(invoice_id, status, created_at DESC);
