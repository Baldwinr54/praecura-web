DO $$
BEGIN
  --
  -- Some early database builds created audit_logs.entity / audit_logs.username as BYTEA.
  -- JPQL uses LOWER() for case-insensitive filtering, which fails on BYTEA in PostgreSQL.
  -- This migration force-converts those columns to VARCHAR using UTF-8 decoding.
  --

  -- entity
  IF EXISTS (
      SELECT 1
      FROM information_schema.columns
      WHERE table_schema IN (current_schema(), 'public')
        AND table_name = 'audit_logs'
        AND column_name = 'entity'
        AND udt_name = 'bytea'
  ) THEN
    BEGIN
      ALTER TABLE audit_logs
        ALTER COLUMN entity TYPE varchar(60)
        USING convert_from(entity, 'UTF8');
    EXCEPTION WHEN OTHERS THEN
      -- Fallback: do not fail the entire migration if the stored bytes are not valid UTF-8.
      ALTER TABLE audit_logs
        ALTER COLUMN entity TYPE varchar(60)
        USING encode(entity, 'escape');
    END;
  END IF;

  -- username
  IF EXISTS (
      SELECT 1
      FROM information_schema.columns
      WHERE table_schema IN (current_schema(), 'public')
        AND table_name = 'audit_logs'
        AND column_name = 'username'
        AND udt_name = 'bytea'
  ) THEN
    BEGIN
      ALTER TABLE audit_logs
        ALTER COLUMN username TYPE varchar(100)
        USING convert_from(username, 'UTF8');
    EXCEPTION WHEN OTHERS THEN
      ALTER TABLE audit_logs
        ALTER COLUMN username TYPE varchar(100)
        USING encode(username, 'escape');
    END;
  END IF;
END $$;
