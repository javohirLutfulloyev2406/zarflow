#!/usr/bin/env bash
# Run as zarflow, from ~/zarflow (the git clone of this repo).
# Usage: scripts/restore.sh <path-to-zarflow-*.sql.gz>
#
# DESTRUCTIVE: drops and recreates all data in the zarflow database before
# loading the dump. Make sure the backend is stopped or expect it to error
# out mid-restore.
set -euo pipefail

if [ $# -ne 1 ]; then
  echo "Usage: $0 <path-to-zarflow-*.sql.gz>" >&2
  exit 1
fi

DUMP_FILE="$1"
if [ ! -f "$DUMP_FILE" ]; then
  echo "File not found: $DUMP_FILE" >&2
  exit 1
fi

cd "$(dirname "$0")/.."

echo "This will REPLACE all data in the 'zarflow' database with the contents of:"
echo "  $DUMP_FILE"
read -r -p "Type 'yes' to continue: " confirm
if [ "$confirm" != "yes" ]; then
  echo "Aborted."
  exit 1
fi

docker compose --env-file .env.prod -f docker-compose.prod.yml stop backend

docker compose --env-file .env.prod -f docker-compose.prod.yml exec -T postgres \
  psql -U zarflow -d zarflow -v ON_ERROR_STOP=1 \
  -c "DROP SCHEMA public CASCADE; CREATE SCHEMA public;"

gunzip -c "$DUMP_FILE" | docker compose --env-file .env.prod -f docker-compose.prod.yml exec -T postgres \
  psql -U zarflow -d zarflow -v ON_ERROR_STOP=1

docker compose --env-file .env.prod -f docker-compose.prod.yml start backend

echo "Restore complete."
