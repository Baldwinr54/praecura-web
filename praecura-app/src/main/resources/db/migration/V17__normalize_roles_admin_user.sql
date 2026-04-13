-- V17: Normaliza el catálogo de roles para que use únicamente 'ADMIN' y 'USER'.
--
-- Contexto:
-- - En etapas previas se llegaron a sembrar 'ROLE_ADMIN'/'ROLE_USER'.
-- - El UserDetailsService usa `.roles(<name>)`, lo que agrega el prefijo ROLE_ automáticamente.
--   Por tanto, el nombre guardado en BD debe ser 'ADMIN'/'USER' y no 'ROLE_ADMIN'/'ROLE_USER'.
--
-- Esta migración:
-- 1) Garantiza 'ADMIN' y 'USER'.
-- 2) Si existen 'ROLE_ADMIN'/'ROLE_USER', migra usuarios y luego intenta eliminarlos si quedan sin uso.

DO $$
DECLARE
  id_admin BIGINT;
  id_user BIGINT;
  id_role_admin BIGINT;
  id_role_user BIGINT;
BEGIN
  INSERT INTO roles(name)
  SELECT 'ADMIN'
  WHERE NOT EXISTS (SELECT 1 FROM roles WHERE name = 'ADMIN');

  INSERT INTO roles(name)
  SELECT 'USER'
  WHERE NOT EXISTS (SELECT 1 FROM roles WHERE name = 'USER');

  SELECT id INTO id_admin FROM roles WHERE name = 'ADMIN';
  SELECT id INTO id_user  FROM roles WHERE name = 'USER';

  SELECT id INTO id_role_admin FROM roles WHERE name = 'ROLE_ADMIN';
  SELECT id INTO id_role_user  FROM roles WHERE name = 'ROLE_USER';

  IF id_role_admin IS NOT NULL THEN
    UPDATE users SET role_id = id_admin WHERE role_id = id_role_admin;
  END IF;
  IF id_role_user IS NOT NULL THEN
    UPDATE users SET role_id = id_user WHERE role_id = id_role_user;
  END IF;

  -- Intentar limpiar roles legacy si ya no están referenciados
  IF id_role_admin IS NOT NULL AND NOT EXISTS (SELECT 1 FROM users WHERE role_id = id_role_admin) THEN
    DELETE FROM roles WHERE id = id_role_admin;
  END IF;

  IF id_role_user IS NOT NULL AND NOT EXISTS (SELECT 1 FROM users WHERE role_id = id_role_user) THEN
    DELETE FROM roles WHERE id = id_role_user;
  END IF;
END $$;
