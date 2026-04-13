#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

# Load .env if present (docker-compose uses it too)
if [[ -f "$ROOT_DIR/.env" ]]; then
  set -a
  # shellcheck disable=SC1091
  source "$ROOT_DIR/.env"
  set +a
fi

CONTAINER="${POSTGRES_CONTAINER_NAME:-praecura_postgres}"
DB="${POSTGRES_DB:-praecura_db}"
USER="${POSTGRES_USER:-praecurauser}"

if ! docker ps --format '{{.Names}}' | grep -qx "$CONTAINER"; then
  echo "ERROR: El contenedor '$CONTAINER' no está en ejecución." >&2
  echo "Inícialo primero con: docker compose up -d" >&2
  exit 1
fi

echo "Reseteando BD '$DB' en el contenedor '$CONTAINER'..."
docker exec -i "$CONTAINER" psql -U "$USER" -d "$DB" < "$ROOT_DIR/scripts/reset-db.sql"
echo "Listo. Reinicia la app; DataSeeder garantizará que exista el usuario admin."
