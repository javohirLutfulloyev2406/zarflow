#!/usr/bin/env bash
# Run as zarflow, from ~/zarflow (the git clone of this repo).
# Dumps the postgres database, gzips it into ~/backups, and prunes dumps older than 14 days.
set -euo pipefail

cd "$(dirname "$0")/.."

BACKUP_DIR="$HOME/backups"
mkdir -p "$BACKUP_DIR"

STAMP="$(date +%Y%m%d-%H%M)"
OUT_FILE="$BACKUP_DIR/zarflow-${STAMP}.sql.gz"

docker compose --env-file .env.prod -f docker-compose.prod.yml exec -T postgres \
  pg_dump -U zarflow zarflow | gzip > "$OUT_FILE"

echo "Wrote $OUT_FILE"

find "$BACKUP_DIR" -name 'zarflow-*.sql.gz' -mtime +14 -delete

DOMAIN="$(grep -E '^ZARFLOW_DOMAIN=' .env.prod 2>/dev/null | cut -d= -f2-)"
DOMAIN="${DOMAIN:-<server>}"

cat <<EOF

To run this daily at 03:00 Asia/Tashkent, add to 'crontab -e' (as zarflow):
CRON_TZ=Asia/Tashkent
0 3 * * * $HOME/zarflow/scripts/backup.sh >> $HOME/backups/backup.log 2>&1

To pull backups off-server to your laptop (run this on your laptop, e.g. in a daily cron/Task Scheduler job):
rsync -avz zarflow@${DOMAIN}:~/backups/ ./zarflow-backups/
EOF
