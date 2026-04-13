# Reset de datos (sin borrar catálogos)

Estos scripts **NO borran la base de datos** ni el esquema.

Lo que hacen es **eliminar datos operativos** (citas, agenda, pacientes, médicos, etc.) para empezar “en limpio”, pero **manteniendo**:

- El usuario **admin** (con la contraseña inicial del proyecto)
- Los **roles**
- El catálogo de **especialidades** (30 especialidades)

> Nota: El catálogo de **servicios** se limpia (TRUNCATE) porque suele considerarse parte de la configuración funcional; si deseas conservarlo también, se puede quitar esa línea del SQL.

## Opción 1: SQL directo (psql)

Archivo: `reset-db.sql`

Ejemplo (valores por defecto del `docker-compose.yml`):

```bash
psql -h localhost -p 5433 -U praecurauser -d praecura_db -f scripts/reset-db.sql
```

Si cambiaste variables en `.env`, ajusta el host/puerto/usuario/DB según corresponda.

## Opción 2: Script `.sh` (Docker Compose)

Archivo: `reset-db.sh`

Ejemplo:

```bash
chmod +x scripts/reset-db.sh
./scripts/reset-db.sh
```

Este script toma sus valores desde `.env` (si existe) y usa por defecto:

- Contenedor: `praecura_postgres`
- DB: `praecura_db`
- Usuario: `praecurauser`

Si tus nombres difieren, ajusta las variables en `.env`:

- `POSTGRES_CONTAINER_NAME`
- `POSTGRES_DB`
- `POSTGRES_USER`
