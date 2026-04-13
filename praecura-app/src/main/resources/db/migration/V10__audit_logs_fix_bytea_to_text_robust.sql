-- PraeCura: Robust fix for legacy schema drift where audit_logs columns were created as BYTEA.
--
-- Symptom (PostgreSQL):
--   ERROR: function lower(bytea) does not exist
-- Cause:
--   Columns like audit_logs.entity / audit_logs.username may be BYTEA in an existing volume.
--
-- This migration is idempotent: it only acts when the target column is BYTEA.
-- It also handles non-UTF8 byte sequences by falling back to an escaped text representation.

DO $$
DECLARE
  r record;
BEGIN
  -- Iterate across any schema that contains audit_logs (public in normal setups).
  FOR r IN
    SELECT n.nspname AS schema_name
    FROM pg_class c
    JOIN pg_namespace n ON n.oid = c.relnamespace
    WHERE c.relkind = 'r'
      AND c.relname = 'audit_logs'
  LOOP

    -- entity
    IF EXISTS (
      SELECT 1
      FROM information_schema.columns
      WHERE table_schema = r.schema_name
        AND table_name = 'audit_logs'
        AND column_name = 'entity'
        AND udt_name = 'bytea'
    ) THEN
      BEGIN
        EXECUTE format(
          'ALTER TABLE %I.audit_logs ALTER COLUMN entity TYPE varchar(60) USING convert_from(entity, ''UTF8'')',
          r.schema_name
        );
      EXCEPTION WHEN others THEN
        EXECUTE format(
          'ALTER TABLE %I.audit_logs ALTER COLUMN entity TYPE varchar(60) USING left(encode(entity, ''escape''), 60)',
          r.schema_name
        );
      END;
    END IF;

    -- username
    IF EXISTS (
      SELECT 1
      FROM information_schema.columns
      WHERE table_schema = r.schema_name
        AND table_name = 'audit_logs'
        AND column_name = 'username'
        AND udt_name = 'bytea'
    ) THEN
      BEGIN
        EXECUTE format(
          'ALTER TABLE %I.audit_logs ALTER COLUMN username TYPE varchar(120) USING convert_from(username, ''UTF8'')',
          r.schema_name
        );
      EXCEPTION WHEN others THEN
        EXECUTE format(
          'ALTER TABLE %I.audit_logs ALTER COLUMN username TYPE varchar(120) USING left(encode(username, ''escape''), 120)',
          r.schema_name
        );
      END;
    END IF;

    -- action
    IF EXISTS (
      SELECT 1
      FROM information_schema.columns
      WHERE table_schema = r.schema_name
        AND table_name = 'audit_logs'
        AND column_name = 'action'
        AND udt_name = 'bytea'
    ) THEN
      BEGIN
        EXECUTE format(
          'ALTER TABLE %I.audit_logs ALTER COLUMN action TYPE varchar(60) USING convert_from(action, ''UTF8'')',
          r.schema_name
        );
      EXCEPTION WHEN others THEN
        EXECUTE format(
          'ALTER TABLE %I.audit_logs ALTER COLUMN action TYPE varchar(60) USING left(encode(action, ''escape''), 60)',
          r.schema_name
        );
      END;
    END IF;

    -- detail
    IF EXISTS (
      SELECT 1
      FROM information_schema.columns
      WHERE table_schema = r.schema_name
        AND table_name = 'audit_logs'
        AND column_name = 'detail'
        AND udt_name = 'bytea'
    ) THEN
      BEGIN
        EXECUTE format(
          'ALTER TABLE %I.audit_logs ALTER COLUMN detail TYPE text USING convert_from(detail, ''UTF8'')',
          r.schema_name
        );
      EXCEPTION WHEN others THEN
        EXECUTE format(
          'ALTER TABLE %I.audit_logs ALTER COLUMN detail TYPE text USING encode(detail, ''escape'')',
          r.schema_name
        );
      END;
    END IF;

  END LOOP;
END $$;
