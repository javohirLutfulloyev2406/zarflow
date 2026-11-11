#!/usr/bin/env bash
# Run once (re-run anytime to pick up changes to this script): sudo scripts/install-autodeploy.sh
# Installs systemd service+timer units for both the pull-based auto-deploy (every 2 min)
# and the server watchdog (every 5 min), running as the zarflow user from ~/zarflow.
# Logs: journalctl -u zarflow-autodeploy -f / journalctl -u zarflow-watchdog -f
set -euo pipefail

if [ "$(id -u)" -ne 0 ]; then
  echo "Run with sudo: sudo $0" >&2
  exit 1
fi

ZARFLOW_HOME="$(getent passwd zarflow | cut -d: -f6)"
if [ -z "$ZARFLOW_HOME" ]; then
  echo "zarflow user not found -- run scripts/server-setup.sh first" >&2
  exit 1
fi
REPO_DIR="$ZARFLOW_HOME/zarflow"
if [ ! -f "$REPO_DIR/scripts/auto-deploy.sh" ]; then
  echo "$REPO_DIR/scripts/auto-deploy.sh not found -- clone the repo there first" >&2
  exit 1
fi

chmod +x "$REPO_DIR/scripts/auto-deploy.sh" "$REPO_DIR/scripts/watchdog.sh"

cat > /etc/systemd/system/zarflow-autodeploy.service <<EOF
[Unit]
Description=ZarFlow pull-based auto-deploy
After=docker.service network-online.target
Wants=network-online.target

[Service]
Type=oneshot
User=zarflow
WorkingDirectory=$REPO_DIR
ExecStart=$REPO_DIR/scripts/auto-deploy.sh
EOF

cat > /etc/systemd/system/zarflow-autodeploy.timer <<'EOF'
[Unit]
Description=Run zarflow-autodeploy every 2 minutes

[Timer]
OnBootSec=1min
OnUnitActiveSec=2min
AccuracySec=10s

[Install]
WantedBy=timers.target
EOF

cat > /etc/systemd/system/zarflow-watchdog.service <<EOF
[Unit]
Description=ZarFlow server watchdog (health/disk/memory/backups)
After=docker.service network-online.target
Wants=network-online.target

[Service]
Type=oneshot
User=zarflow
WorkingDirectory=$REPO_DIR
ExecStart=$REPO_DIR/scripts/watchdog.sh
EOF

cat > /etc/systemd/system/zarflow-watchdog.timer <<'EOF'
[Unit]
Description=Run zarflow-watchdog every 5 minutes

[Timer]
OnBootSec=2min
OnUnitActiveSec=5min
AccuracySec=10s

[Install]
WantedBy=timers.target
EOF

systemctl daemon-reload
systemctl enable --now zarflow-autodeploy.timer zarflow-watchdog.timer

echo "Installed. Timer status:"
systemctl list-timers 'zarflow-*' --no-pager
echo
echo "Logs:"
echo "  journalctl -u zarflow-autodeploy -f"
echo "  journalctl -u zarflow-watchdog -f"
echo "Pause: sudo systemctl stop zarflow-autodeploy.timer   (or zarflow-watchdog.timer)"
