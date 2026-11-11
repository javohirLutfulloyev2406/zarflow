#!/usr/bin/env bash
# Run as zarflow, from ~/zarflow, on a timer (see scripts/install-autodeploy.sh).
# Checks health/disk/memory/backups and alerts the admin via a direct Bot API call
# (no app dependency -- this has to work even when the backend itself is down).
# Each condition alerts once when it trips, then stays quiet until it recovers
# (tracked in a small state file) -- no repeat spam every 5 minutes.
set -euo pipefail

cd "$(dirname "$0")/.."

if [ ! -f .env.prod ]; then
  echo "watchdog: .env.prod not found" >&2
  exit 1
fi

STATE_DIR="$HOME/.zarflow"
STATE_FILE="$STATE_DIR/watchdog-state"
BACKUP_DIR="$HOME/backups"
mkdir -p "$STATE_DIR"

DOMAIN="$(grep -E '^ZARFLOW_DOMAIN=' .env.prod | cut -d= -f2-)"

HEALTH_DOWN_STREAK=0
HEALTH_ALERTING=false
DISK_ALERTING=false
MEM_ALERTING=false
BACKUP_ALERTING=false
if [ -f "$STATE_FILE" ]; then
  # shellcheck disable=SC1090
  . "$STATE_FILE"
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
    >/dev/null 2>&1 || echo "watchdog: admin notify failed (non-fatal)" >&2
}

# ---- health: https://$ZARFLOW_DOMAIN/actuator/health not UP twice in a row ----

if curl -fsS -m 10 "https://${DOMAIN}/actuator/health" 2>/dev/null | grep -q '"status":"UP"'; then
  HEALTH_DOWN_STREAK=0
  if [ "$HEALTH_ALERTING" = true ]; then
    notify "✅ Recovered: ${DOMAIN} health is UP again"
    HEALTH_ALERTING=false
  fi
else
  HEALTH_DOWN_STREAK=$((HEALTH_DOWN_STREAK + 1))
  if [ "$HEALTH_DOWN_STREAK" -ge 2 ] && [ "$HEALTH_ALERTING" = false ]; then
    notify "🔴 ${DOMAIN} health check is DOWN (2 checks in a row, ~10 min)"
    HEALTH_ALERTING=true
  fi
fi

# ---- disk: root filesystem usage > 80% ----

disk_pct="$(df -P / | awk 'NR==2 { gsub("%","",$5); print $5 }')"
if [ "$disk_pct" -gt 80 ]; then
  if [ "$DISK_ALERTING" = false ]; then
    notify "🔴 Disk usage at ${disk_pct}% on $(hostname)"
    DISK_ALERTING=true
  fi
elif [ "$DISK_ALERTING" = true ]; then
  notify "✅ Recovered: disk usage back to ${disk_pct}%"
  DISK_ALERTING=false
fi

# ---- memory: "available" (free's column 7, reclaimable cache included) < 10% ----
# -- plain "free" is nearly always near-zero on Linux (the kernel uses idle RAM
# for page cache), so alerting on that would fire constantly and mean nothing.

mem_available_pct="$(free | awk '/^Mem:/ { printf "%d", ($7/$2)*100 }')"
if [ "$mem_available_pct" -lt 10 ]; then
  if [ "$MEM_ALERTING" = false ]; then
    notify "🔴 Available memory at ${mem_available_pct}% on $(hostname)"
    MEM_ALERTING=true
  fi
elif [ "$MEM_ALERTING" = true ]; then
  notify "✅ Recovered: available memory back to ${mem_available_pct}%"
  MEM_ALERTING=false
fi

# ---- backups: newest ~/backups/*.sql.gz older than 26h (daily job expected at 03:00) ----

latest_backup="$(find "$BACKUP_DIR" -maxdepth 1 -name 'zarflow-*.sql.gz' -printf '%T@ %p\n' 2>/dev/null \
  | sort -rn | head -1 | cut -d' ' -f2-)"
if [ -z "$latest_backup" ]; then
  if [ "$BACKUP_ALERTING" = false ]; then
    notify "🔴 No backups found in ${BACKUP_DIR}"
    BACKUP_ALERTING=true
  fi
else
  age_hours=$(( ( $(date +%s) - $(stat -c %Y "$latest_backup") ) / 3600 ))
  if [ "$age_hours" -gt 26 ]; then
    if [ "$BACKUP_ALERTING" = false ]; then
      notify "🔴 Latest backup is ${age_hours}h old -- backup cron may be broken"
      BACKUP_ALERTING=true
    fi
  elif [ "$BACKUP_ALERTING" = true ]; then
    notify "✅ Recovered: backups running again (latest ${age_hours}h old)"
    BACKUP_ALERTING=false
  fi
fi

cat > "$STATE_FILE" <<EOF
HEALTH_DOWN_STREAK=$HEALTH_DOWN_STREAK
HEALTH_ALERTING=$HEALTH_ALERTING
DISK_ALERTING=$DISK_ALERTING
MEM_ALERTING=$MEM_ALERTING
BACKUP_ALERTING=$BACKUP_ALERTING
EOF
