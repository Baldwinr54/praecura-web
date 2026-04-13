DO $$
DECLARE
  r RECORD;
  col TEXT;
  len INT;
  q TEXT;
BEGIN
  --
  -- Safety migration.
  --
  -- In some environments, audit_logs text-like columns have been observed as BYTEA.
  -- That breaks filters that rely on LOWER()/ILIKE in PostgreSQL.
  --
  -- This migration detects any audit_logs table (any schema) and converts ONLY the
  -- affected columns when their underlying type is BYTEA.
  --
  FOR r IN
    SELECT table_schema
    FROM information_schema.tables
    WHERE table_name = 'audit_logs'
      AND table_type = 'BASE TABLE'
    ORDER BY table_schema
  LOOP
    FOREACH col IN ARRAY ARRAY['username', 'action', 'entity', 'detail']
    LOOP
      SELECT CASE col
               WHEN 'detail' THEN 500
               ELSE 60
             END
        INTO len;

      IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = r.table_schema
          AND table_name = 'audit_logs'
          AND column_name = col
          AND udt_name = 'bytea'
      ) THEN
        q := format(
          'ALTER TABLE %I.audit_logs ALTER COLUMN %I TYPE varchar(%s) USING convert_from(%I, ''UTF8'');',
          r.table_schema,
          col,
          len,
          col
        );
        BEGIN
          EXECUTE q;
        EXCEPTION WHEN OTHERS THEN
          -- Fallback: convert to a safe textual representation so the app can run.
          -- (Prefer UTF-8, but do not fail the whole migration if conversion fails.)
          q := format(
            'ALTER TABLE %I.audit_logs ALTER COLUMN %I TYPE varchar(%s) USING encode(%I, ''escape'');',
            r.table_schema,
            col,
            len,
            col
          );
          EXECUTE q;
        END;
      END IF;
    END LOOP;
  END LOOP;
END $$;
