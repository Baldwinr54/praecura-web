ALTER TABLE invoices ALTER COLUMN currency SET DEFAULT 'DOP';
ALTER TABLE payments ALTER COLUMN currency SET DEFAULT 'DOP';
ALTER TABLE payment_links ALTER COLUMN currency SET DEFAULT 'DOP';

UPDATE invoices SET currency = 'DOP' WHERE currency IS NULL OR currency = 'USD';
UPDATE payments SET currency = 'DOP' WHERE currency IS NULL OR currency = 'USD';
UPDATE payment_links SET currency = 'DOP' WHERE currency IS NULL OR currency = 'USD';
