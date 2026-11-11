#!/usr/bin/env bash
# Run as zarflow, from ~/zarflow (the git clone of this repo), on a timer (see
# scripts/install-autodeploy.sh). Pull-based: nothing SSHes in from GitHub.
#
#   1) git fetch; fast-forward if origin/main moved (compose file, scripts, Caddyfile, ...).
#   2) docker compose pull; compare running vs pulled image IDs for backend and web.
#      Nothing changed (git included) -> exit quietly.
#   3) Otherwise run scripts/deploy.sh.
#   4) Healthy -> record these image IDs as "last-good", notify the admin.
#      Unhealthy -> roll back to last-good, redeploy, notify with recent backend logs.
#
# flock'd so an overlapping run (e.g. a slow deploy still running when the next
# timer tick fires) exits immediately instead of racing the first one.
set -euo pipefail

cd "$(dirname "$0")/.."
REPO_DIR="$(pwd)"
COMPOSE="docker compose --env-file .env.prod -f docker-compose.prod.yml"
STATE_DIR="$HOME/.zarflow"
LAST_GOOD_FILE="$STATE_DIR/last-good"
LOCK_FILE="$STATE_DIR/auto-deploy.lock"

mkdir -p "$STATE_DIR"
exec 200>"$LOCK_FILE"
if ! flock -n 200; then
  echo "auto-deploy: another run is already in progress, exiting"
  exit 0
fi

if [ ! -f .env.prod ]; then
  echo "auto-deploy: .env.prod not found in $REPO_DIR, aborting" >&2
  exit 1
fi

notify() {
  local text="$1"
  local token chat_id
  token="$(grep -E '^TELEGRAM_BOT_TOKEN=' .env.prod | cut -d= -f2-)"
  chat_id="$(grep -E '^ZARFLOW_ADMIN_CHAT_ID=' .env.prod | cut -d= -f2-)"
  if [ -z "$chat_id" ] || [ -z "$token" ]; then
    return 0
  fi
  curl -fsS -X POST "https://api.telegram.org/bot${token}/sendMessage" \
    --data-urlencode "chat_id=${chat_id}" \
    --data-urlencode "text=${text}" \
    >/dev/null 2>&1 || echo "auto-deploy: admin notify failed (non-fatal)" >&2
}

image_ref() {
  # $1: a substring unique to the service's image name (e.g. "zarflow-backend").
  $COMPOSE config --images 2>/dev/null | grep "$1"
}

running_image_id() {
  # $1: compose service name. Empty if the container doesn't exist yet.
  local cid
  cid="$($COMPOSE ps -q "$1" 2>/dev/null || true)"
  if [ -z "$cid" ]; then
    return 0
  fi
  docker inspect --format '{{.Image}}' "$cid" 2>/dev/null || true
}

# ---- 1) git fetch / fast-forward ----

before_sha="$(git rev-parse HEAD)"
git fetch origin main --quiet
remote_sha="$(git rev-parse origin/main)"

git_changed=false
if [ "$before_sha" != "$remote_sha" ]; then
  git merge --ff-only origin/main --quiet
  git_changed=true
fi

# ---- 2) image comparison ----

backend_ref="$(image_ref zarflow-backend)"
web_ref="$(image_ref zarflow-web)"

running_backend_id="$(running_image_id backend)"
running_web_id="$(running_image_id web)"

$COMPOSE pull -q

pulled_backend_id="$(docker image inspect --format '{{.Id}}' "$backend_ref" 2>/dev/null || true)"
pulled_web_id="$(docker image inspect --format '{{.Id}}' "$web_ref" 2>/dev/null || true)"

images_changed=false
if [ "$pulled_backend_id" != "$running_backend_id" ] || [ "$pulled_web_id" != "$running_web_id" ]; then
  images_changed=true
fi

if [ "$git_changed" = false ] && [ "$images_changed" = false ]; then
  echo "auto-deploy: nothing changed, exiting quietly"
  exit 0
fi

echo "auto-deploy: change detected (git_changed=$git_changed images_changed=$images_changed), deploying"

# ---- 3) deploy ----

if scripts/deploy.sh; then
  # ---- 4a) success: record last-good, notify ----
  deployed_backend_id="$(docker image inspect --format '{{.Id}}' "$backend_ref")"
  deployed_web_id="$(docker image inspect --format '{{.Id}}' "$web_ref")"
  cat > "$LAST_GOOD_FILE" <<EOF
LAST_GOOD_BACKEND_ID=$deployed_backend_id
LAST_GOOD_WEB_ID=$deployed_web_id
LAST_GOOD_SHA=$(git rev-parse HEAD)
EOF

  short_sha="$(git rev-parse --short HEAD)"
  title="$(git log -1 --format=%s)"
  notify "✅ Deploy: ${short_sha} · ${title}"
  exit 0
fi

# ---- 4b) failure: roll back to last-good, redeploy, notify ----

echo "auto-deploy: deploy.sh failed, attempting rollback" >&2

if [ ! -f "$LAST_GOOD_FILE" ]; then
  echo "auto-deploy: no last-good recorded yet, cannot roll back" >&2
  notify "⚠️ Deploy failed, no previous good deploy to roll back to -- manual intervention needed"
  exit 1
fi

# shellcheck disable=SC1090
. "$LAST_GOOD_FILE"

docker tag "$LAST_GOOD_BACKEND_ID" "$backend_ref"
docker tag "$LAST_GOOD_WEB_ID" "$web_ref"
$COMPOSE up -d

rollback_sha="${LAST_GOOD_SHA:0:7}"
recent_logs="$($COMPOSE logs --tail=20 backend 2>/dev/null | \
  sed -E 's/(TELEGRAM_BOT_TOKEN|TELEGRAM_WEBHOOK_SECRET|ZARFLOW_JWT_SECRET|POSTGRES_PASSWORD)=[^ ]+/\1=***/g; s/(Bearer|bot)[0-9A-Za-z_:.-]{10,}/\1***/g' \
  || true)"
notify "⚠️ Deploy failed, rolled back to ${rollback_sha}
${recent_logs}"
exit 1
