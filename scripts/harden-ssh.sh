#!/usr/bin/env bash
# Run once key login for "zarflow" is confirmed working from your laptop.
# Disables password auth and root login over SSH. Run with sudo.
set -euo pipefail

if [ "$(id -u)" -ne 0 ]; then
  echo "Run with sudo: sudo $0" >&2
  exit 1
fi

read -r -p "Have you confirmed 'ssh zarflow@<server>' works with your key? [y/N] " confirm
if [ "$confirm" != "y" ] && [ "$confirm" != "Y" ]; then
  echo "Aborting -- confirm key login works before hardening ssh." >&2
  exit 1
fi

CONF_DIR=/etc/ssh/sshd_config.d
mkdir -p "$CONF_DIR"
cat > "$CONF_DIR/99-zarflow-hardening.conf" <<'EOF'
PasswordAuthentication no
KbdInteractiveAuthentication no
PermitRootLogin no
EOF

sshd -t
systemctl reload ssh

echo "SSH hardened: password auth and root login are now disabled."
