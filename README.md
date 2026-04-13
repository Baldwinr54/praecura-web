# PraeCura Web (Spring Boot + Postgres)

This repository contains a Spring Boot (Thymeleaf) web application for managing appointments, plus a local Postgres instance via Docker Compose.

## Prerequisites
- Docker Desktop (or Docker Engine) with `docker compose`
- Java 21+ (recommended)
- Internet access (Maven downloads dependencies on the first build)

## Quick start (macOS/Linux)
1. From the project root (the folder that contains `docker-compose.yml` and the `praecura-app/` directory), copy the environment template:
   - `cp .env.example .env`

2. Start everything:
   - `./setup_praecura_app.sh`

3. Open the app:
   - `http://localhost:8080`

### Recommended env vars
- `PRAECURA_BOOTSTRAP_ADMIN_PASSWORD` (crea el admin con contraseña segura)
- `PRAECURA_REMEMBER_ME_KEY` (clave larga para cookies remember‑me)

### Demo data (opcional)
Para borrar datos y crear 50 registros realistas por entidad (sin tocar usuarios ni especialidades):
1. Exporta las variables:
   - `PRAECURA_DEMO_SEED=true`
   - `PRAECURA_DEMO_COUNT=50`
2. Inicia la app.
3. Cuando termine de levantar, vuelve a poner `PRAECURA_DEMO_SEED=false`.

### Default credentials (bootstrap)
- Admin: `admin` / `Admin123*` (solo si **no** se define `PRAECURA_BOOTSTRAP_ADMIN_PASSWORD`)
- Recomendado: definir `PRAECURA_BOOTSTRAP_ADMIN_PASSWORD` y cambiar la contraseña tras el primer ingreso.

## Manual start (any OS)
1. Start Postgres:
   - `docker compose up -d`

2. Run the app:
   - `cd praecura-app`
   - `./mvnw spring-boot:run`

## Notes
- Database connection defaults are aligned between `docker-compose.yml` and `src/main/resources/application.yml`.
- Flyway migrations live in `praecura-app/src/main/resources/db/migration`.
- Initial roles/users are created by `DataSeeder` on startup (idempotent).

## Resetting the database (start from zero)
If you want to wipe all app data (patients, appointments and users) and start again from a clean state:

1. Ensure Postgres is running:
   - `docker compose up -d`

2. Run the reset script:
   - `./scripts/reset-db.sh`

After running it, restart the app. On startup, `DataSeeder` will recreate the admin account if it does not exist.
