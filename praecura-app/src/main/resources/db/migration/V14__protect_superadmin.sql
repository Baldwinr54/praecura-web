-- V14: Protege el superadmin (username=admin) a nivel de base de datos.
-- Reglas:
-- - No se permite DELETE del usuario admin.
-- - No se permite desactivar (enabled=false) admin.
-- - El role_id de admin solo puede apuntar a un rol cuyo nombre sea 'ADMIN'.
-- - No se permite bloquear admin (locked_until no puede ser no-null).
-- - No se permite incrementar intentos fallidos de admin (failed_login_attempts debe ser 0).
-- - No se permite renombrar admin (username debe permanecer 'admin').

CREATE OR REPLACE FUNCTION fn_protect_superadmin_update()
RETURNS trigger AS $$
BEGIN
  IF lower(OLD.username) = 'admin' THEN
    -- username inmutable
    IF lower(NEW.username) <> 'admin' THEN
      RAISE EXCEPTION 'No se permite renombrar el usuario admin del sistema';
    END IF;

    -- enabled no puede ser false
    IF NEW.enabled = FALSE THEN
      RAISE EXCEPTION 'No se permite desactivar el usuario admin del sistema';
    END IF;

    -- role_id: solo se permite cambiarlo si el nuevo rol sigue siendo ADMIN
    -- (esto permite normalizaciones controladas desde migraciones si existían roles legacy).
    IF NEW.role_id IS DISTINCT FROM OLD.role_id THEN
      IF NOT EXISTS (SELECT 1 FROM roles r WHERE r.id = NEW.role_id AND r.name = 'ADMIN') THEN
        RAISE EXCEPTION 'No se permite cambiar el rol del usuario admin del sistema';
      END IF;
    END IF;

    -- lockout deshabilitado
    IF NEW.locked_until IS NOT NULL THEN
      RAISE EXCEPTION 'No se permite bloquear el usuario admin del sistema';
    END IF;

    IF NEW.failed_login_attempts <> 0 THEN
      RAISE EXCEPTION 'No se permite incrementar intentos fallidos del usuario admin del sistema';
    END IF;
  END IF;

  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION fn_protect_superadmin_delete()
RETURNS trigger AS $$
BEGIN
  IF lower(OLD.username) = 'admin' THEN
    RAISE EXCEPTION 'No se permite eliminar el usuario admin del sistema';
  END IF;
  RETURN OLD;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_protect_superadmin_update ON users;
CREATE TRIGGER trg_protect_superadmin_update
BEFORE UPDATE ON users
FOR EACH ROW
EXECUTE FUNCTION fn_protect_superadmin_update();

DROP TRIGGER IF EXISTS trg_protect_superadmin_delete ON users;
CREATE TRIGGER trg_protect_superadmin_delete
BEFORE DELETE ON users
FOR EACH ROW
EXECUTE FUNCTION fn_protect_superadmin_delete();
