ALTER TABLE invoices ALTER COLUMN currency TYPE VARCHAR(3) USING currency::varchar;
ALTER TABLE payments ALTER COLUMN currency TYPE VARCHAR(3) USING currency::varchar;
ALTER TABLE payment_links ALTER COLUMN currency TYPE VARCHAR(3) USING currency::varchar;
