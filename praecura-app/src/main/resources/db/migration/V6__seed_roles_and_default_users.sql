-- V6: Seeds mínimos para poder iniciar sesión en una BD nueva
-- Crea roles ADMIN/USER y dos usuarios de ejemplo si la tabla está vacía.
-- Credenciales por defecto:
--   admin / Admin123*
--   user  / User123*

DO $$
DECLARE
  admin_role_id BIGINT;
  user_role_id BIGINT;
BEGIN
  -- Roles
  INSERT INTO roles(name)
  SELECT 'ADMIN'
  WHERE NOT EXISTS (SELECT 1 FROM roles WHERE name='ADMIN');

  INSERT INTO roles(name)
  SELECT 'USER'
  WHERE NOT EXISTS (SELECT 1 FROM roles WHERE name='USER');

  SELECT id INTO admin_role_id FROM roles WHERE name='ADMIN';
  SELECT id INTO user_role_id FROM roles WHERE name='USER';

  -- Usuarios (solo si no hay ninguno)
  IF NOT EXISTS (SELECT 1 FROM users) THEN
    INSERT INTO users(username, password_hash, enabled, role_id)
    VALUES
      ('admin', '$2b$10$P5ArOKSjflF/kCJ1BngOR.Yz4/lSxJlMzcO5804Y64TjLRahFG8GG', TRUE, admin_role_id),
      ('user',  '$2b$10$7XxrlkEff6wuuEeHUl4Jxu9DaOZxgwPnaqM4.eRxRHeNlsoS0JfLa', TRUE, user_role_id);
  END IF;
END $$;
