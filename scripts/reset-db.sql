-- Reset database content to a "fresh" state.
--
-- What it does:
-- - Deletes all rows from the app tables.
-- - Resets sequences (IDs) back to 1.
-- - Keeps Flyway history intact.
--
-- After running this script, restart the app.
-- DataSeeder will ensure the minimum roles/user exist:
--   admin / CAMBIAR_PASSWORD_ADMIN

BEGIN;

-- Limpia datos de la aplicación (dominio) y reinicia IDs.
TRUNCATE TABLE audit_logs, appointments, medical_services, doctors, patients
  RESTART IDENTITY
  CASCADE;

-- Asegura roles y superadmin (username=admin). Esto permite reiniciar datos sin perder acceso.
DO $$
DECLARE
  admin_role_id BIGINT;
  user_role_id BIGINT;
BEGIN
  -- Roles mínimos
  IF NOT EXISTS (SELECT 1 FROM roles WHERE name = 'ADMIN') THEN
    INSERT INTO roles(name) VALUES ('ADMIN');
  END IF;
  IF NOT EXISTS (SELECT 1 FROM roles WHERE name = 'USER') THEN
    INSERT INTO roles(name) VALUES ('USER');
  END IF;

  SELECT id INTO admin_role_id FROM roles WHERE name = 'ADMIN';
  SELECT id INTO user_role_id  FROM roles WHERE name = 'USER';

  -- Superadmin (password: CAMBIAR_PASSWORD_ADMIN)
  IF NOT EXISTS (SELECT 1 FROM users WHERE lower(username) = 'admin') THEN
    INSERT INTO users(username, password_hash, enabled, role_id, failed_login_attempts, locked_until)
    VALUES ('admin', '$2b$10$P5ArOKSjflF/kCJ1BngOR.Yz4/lSxJlMzcO5804Y64TjLRahFG8GG', TRUE, admin_role_id, 0, NULL);
  ELSE
    UPDATE users
      SET enabled = TRUE,
          role_id = admin_role_id,
          failed_login_attempts = 0,
          locked_until = NULL
    WHERE lower(username) = 'admin';
  END IF;

  -- Mantén solo admin; elimina el resto de usuarios (si existieran)
  DELETE FROM users WHERE lower(username) <> 'admin';
END $$;

COMMIT;
