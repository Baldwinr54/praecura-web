#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT_DIR"

# Load .env if present (Docker Compose does this automatically; we load it for the health-check step)
if [[ -f ".env" ]]; then
  set -a
  # shellcheck disable=SC1091
  source ".env"
  set +a
fi

POSTGRES_CONTAINER_NAME="${POSTGRES_CONTAINER_NAME:-praecura_postgres}"

echo "==> Starting Postgres (Docker Compose)"
docker compose up -d

echo "==> Waiting for Postgres to become healthy..."
for i in {1..30}; do
  STATUS="$(docker inspect --format='{{json .State.Health.Status}}' "$POSTGRES_CONTAINER_NAME" 2>/dev/null || true)"
  if [[ "$STATUS" == "\"healthy\"" ]]; then
    echo "Postgres is healthy."
    break
  fi
  sleep 2
done

echo "==> Running the app"
cd "$ROOT_DIR/praecura-app"
chmod +x mvnw
./mvnw -DskipTests spring-boot:run
