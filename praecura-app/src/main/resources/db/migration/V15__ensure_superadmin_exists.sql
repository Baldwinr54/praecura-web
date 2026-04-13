-- V15: Asegura que el superadmin (username=admin) y el usuario demo (user) existan siempre.
--
-- Problema que resuelve:
-- - Si se limpian datos (TRUNCATE/DELETE) y se borra la tabla users, Flyway NO re-ejecuta V6.
-- - El sistema puede quedar sin credenciales para entrar.
--
-- Reglas:
-- - No crea roles adicionales: solo ADMIN y USER.
-- - Si faltan roles, los crea.
-- - Si falta admin, lo inserta.
-- - Si admin existe, lo re-habilita, le resetea lockout y le asegura rol ADMIN.
-- - Si falta user, lo inserta (para demos).
--
-- Credenciales por defecto:
--   admin / Admin123*
--   user  / User123*

DO $$
DECLARE
  admin_role_id BIGINT;
  user_role_id BIGINT;
BEGIN
  -- Roles mínimos
  INSERT INTO roles(name)
  SELECT 'ADMIN'
  WHERE NOT EXISTS (SELECT 1 FROM roles WHERE name='ADMIN');

  INSERT INTO roles(name)
  SELECT 'USER'
  WHERE NOT EXISTS (SELECT 1 FROM roles WHERE name='USER');

  SELECT id INTO admin_role_id FROM roles WHERE name='ADMIN';
  SELECT id INTO user_role_id  FROM roles WHERE name='USER';

  -- Superadmin (password: Admin123*)
  IF NOT EXISTS (SELECT 1 FROM users WHERE lower(username) = 'admin') THEN
    INSERT INTO users(username, password_hash, enabled, role_id, failed_login_attempts, locked_until)
    VALUES (
      'admin',
      '$2b$10$P5ArOKSjflF/kCJ1BngOR.Yz4/lSxJlMzcO5804Y64TjLRahFG8GG',
      TRUE,
      admin_role_id,
      0,
      NULL
    );
  ELSE
    UPDATE users
       SET enabled = TRUE,
           role_id = admin_role_id,
           failed_login_attempts = 0,
           locked_until = NULL
     WHERE lower(username) = 'admin';
  END IF;

  -- Usuario demo (password: User123*)
  IF NOT EXISTS (SELECT 1 FROM users WHERE lower(username) = 'user') THEN
    INSERT INTO users(username, password_hash, enabled, role_id, failed_login_attempts, locked_until)
    VALUES (
      'user',
      '$2b$10$7XxrlkEff6wuuEeHUl4Jxu9DaOZxgwPnaqM4.eRxRHeNlsoS0JfLa',
      TRUE,
      user_role_id,
      0,
      NULL
    );
  END IF;
END $$;
