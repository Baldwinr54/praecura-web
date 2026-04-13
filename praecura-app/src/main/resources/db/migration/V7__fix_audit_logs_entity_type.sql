-- Ensure audit_logs.entity is stored as TEXT (or at least a character type).
-- The application filters use LOWER(a.entity), which fails when the column is BYTEA.

DO $$
DECLARE
  col_udt text;
BEGIN
  SELECT c.udt_name
    INTO col_udt
  FROM information_schema.columns c
  WHERE c.table_schema = 'public'
    AND c.table_name = 'audit_logs'
    AND c.column_name = 'entity';

  IF col_udt IS NULL THEN
    RAISE NOTICE 'Column audit_logs.entity not found; skipping migration.';
    RETURN;
  END IF;

  IF col_udt = 'bytea' THEN
    RAISE NOTICE 'Altering audit_logs.entity from BYTEA to TEXT.';
    ALTER TABLE audit_logs
      ALTER COLUMN entity TYPE text USING convert_from(entity, 'UTF8');
  ELSE
    RAISE NOTICE 'audit_logs.entity is %; no change needed.', col_udt;
  END IF;
END $$;
