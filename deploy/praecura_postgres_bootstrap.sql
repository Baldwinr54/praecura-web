\set ON_ERROR_STOP on
\echo [1/4] Creando rol de aplicación si no existe...
SELECT format(
  $$CREATE ROLE praecura_app LOGIN PASSWORD %L NOSUPERUSER NOCREATEDB NOCREATEROLE INHERIT$$,
  'CambiaEstaClaveSegura'
)
WHERE NOT EXISTS (
  SELECT 1 FROM pg_roles WHERE rolname = 'praecura_app'
)\gexec

\echo [2/4] Creando base de datos si no existe...
SELECT 'CREATE DATABASE praecura_prod OWNER praecura_app ENCODING ''UTF8'' TEMPLATE template0'
WHERE NOT EXISTS (
  SELECT 1 FROM pg_database WHERE datname = 'praecura_prod'
)\gexec

\echo [3/4] Ajustando privilegios de la base...
REVOKE ALL ON DATABASE praecura_prod FROM PUBLIC;
GRANT ALL PRIVILEGES ON DATABASE praecura_prod TO praecura_app;
\connect praecura_prod

\echo [4/4] Ajustando esquema public...
REVOKE CREATE ON SCHEMA public FROM PUBLIC;
GRANT USAGE, CREATE ON SCHEMA public TO praecura_app;
ALTER ROLE praecura_app SET search_path TO public;

\echo Bootstrap PostgreSQL completado. Ahora inicia la aplicación para que Flyway cree el esquema.
