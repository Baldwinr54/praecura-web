-- Ensure audit_logs.entity is TEXT/VARCHAR and not BYTEA.
-- This handles cases where audit_logs was created earlier with a wrong column type.

DO $$
DECLARE
  col_udt text;
BEGIN
  -- Only act if the table/column exists and is BYTEA.
  SELECT c.udt_name
    INTO col_udt
  FROM information_schema.columns c
  WHERE c.table_schema = 'public'
    AND c.table_name = 'audit_logs'
    AND c.column_name = 'entity';

  IF col_udt = 'bytea' THEN
    -- Convert the stored bytes to UTF-8 text, then swap columns.
    -- (If some rows are not valid UTF-8, this will fail, which is preferable to silent corruption.)
    ALTER TABLE audit_logs ADD COLUMN entity_text text;
    UPDATE audit_logs SET entity_text = convert_from(entity, 'UTF8');

    ALTER TABLE audit_logs DROP COLUMN entity;
    ALTER TABLE audit_logs RENAME COLUMN entity_text TO entity;

    -- Keep the original semantic: entity is required.
    ALTER TABLE audit_logs ALTER COLUMN entity SET NOT NULL;
  END IF;
END $$;
