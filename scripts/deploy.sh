#!/usr/bin/env bash
# Run as zarflow, from ~/zarflow (the git clone of this repo).
# Usage: scripts/deploy.sh [image-tag]
#   image-tag defaults to IMAGE_TAG in .env.prod (normally "latest").
#   Pass a git sha to roll back/forward to a specific build.
set -euo pipefail

cd "$(dirname "$0")/.."

if [ ! -f .env.prod ]; then
  echo ".env.prod not found in $(pwd) -- copy .env.prod.example and fill it in first" >&2
  exit 1
fi

if [ $# -ge 1 ]; then
  export IMAGE_TAG="$1"
fi

git pull --ff-only
export ZARFLOW_BUILD_SHA="$(git rev-parse --short HEAD)"

docker compose --env-file .env.prod -f docker-compose.prod.yml pull
docker compose --env-file .env.prod -f docker-compose.prod.yml up -d

DOMAIN="$(grep -E '^ZARFLOW_DOMAIN=' .env.prod | cut -d= -f2-)"
HEALTH_URL="https://${DOMAIN}/actuator/health"

echo "Waiting for ${HEALTH_URL} (up to 180s)..."
elapsed=0
until curl -fsS "$HEALTH_URL" >/dev/null 2>&1; do
  if [ "$elapsed" -ge 180 ]; then
    echo "Health check failed after 180s" >&2
    echo "---- backend logs (last 50 lines) ----"
    docker compose -f docker-compose.prod.yml logs --tail=50 backend
    echo "---- web logs (last 50 lines) ----"
    docker compose -f docker-compose.prod.yml logs --tail=50 web
    exit 1
  fi
  sleep 5
  elapsed=$((elapsed + 5))
done

echo "Deploy OK: ${HEALTH_URL} is healthy."
