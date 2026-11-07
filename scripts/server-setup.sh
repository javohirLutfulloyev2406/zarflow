#!/usr/bin/env bash
# Run once on a fresh server: sudo scripts/server-setup.sh
# Safe to re-run -- every step checks current state before changing anything.
# Does NOT touch sshd; run scripts/harden-ssh.sh separately once zarflow key login works.
set -euo pipefail

if [ "$(id -u)" -ne 0 ]; then
  echo "Run with sudo: sudo $0" >&2
  exit 1
fi

echo "== Growing root filesystem (no-op if the EBS volume wasn't enlarged) =="
ROOT_SRC="$(findmnt -n -o SOURCE /)"
ROOT_DISK="$(lsblk -no PKNAME "$ROOT_SRC")"
ROOT_PARTNUM="$(lsblk -no PARTN "$ROOT_SRC")"
if [ -n "$ROOT_DISK" ] && [ -n "$ROOT_PARTNUM" ]; then
  apt-get install -y -qq cloud-guest-utils >/dev/null
  growpart "/dev/$ROOT_DISK" "$ROOT_PARTNUM" || echo "growpart: already at full size"
  FSTYPE="$(findmnt -n -o FSTYPE /)"
  case "$FSTYPE" in
    ext4) resize2fs "$ROOT_SRC" ;;
    xfs) xfs_growfs / ;;
    *) echo "Unknown root fstype '$FSTYPE', skipping filesystem resize" ;;
  esac
else
  echo "Could not determine root disk/partition, skipping resize"
fi

echo "== Swap =="
if [ ! -f /swapfile ]; then
  fallocate -l 2G /swapfile
  chmod 600 /swapfile
  mkswap /swapfile
  swapon /swapfile
else
  echo "swapfile already exists"
fi
grep -q '^/swapfile ' /etc/fstab || echo '/swapfile none swap sw 0 0' >> /etc/fstab
if ! grep -q '^vm.swappiness' /etc/sysctl.d/99-zarflow.conf 2>/dev/null; then
  echo 'vm.swappiness=10' >> /etc/sysctl.d/99-zarflow.conf
  sysctl --system >/dev/null
fi

echo "== Docker =="
if command -v docker >/dev/null 2>&1; then
  echo "docker already installed, skipping"
else
  CODENAME="$(. /etc/os-release && echo "$VERSION_CODENAME")"
  if curl -fsSL -o /dev/null "https://download.docker.com/linux/ubuntu/dists/${CODENAME}/Release"; then
    echo "Using Docker's official apt repo (suite: ${CODENAME})"
    apt-get update -qq
    apt-get install -y -qq ca-certificates curl gnupg
    install -m 0755 -d /etc/apt/keyrings
    curl -fsSL https://download.docker.com/linux/ubuntu/gpg -o /etc/apt/keyrings/docker.asc
    chmod a+r /etc/apt/keyrings/docker.asc
    echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] https://download.docker.com/linux/ubuntu ${CODENAME} stable" \
      > /etc/apt/sources.list.d/docker.list
    apt-get update -qq
    apt-get install -y -qq docker-ce docker-ce-cli containerd.io docker-compose-plugin
  else
    echo "Docker has no '${CODENAME}' suite yet -- falling back to Ubuntu's docker.io + docker-compose-v2"
    apt-get update -qq
    apt-get install -y -qq docker.io docker-compose-v2
  fi
  systemctl enable --now docker
fi

echo "== zarflow service user =="
if ! id zarflow >/dev/null 2>&1; then
  useradd -m -s /bin/bash -G docker zarflow
else
  usermod -aG docker zarflow
fi
install -d -m 700 -o zarflow -g zarflow /home/zarflow/.ssh
if [ -f /home/ubuntu/.ssh/authorized_keys ]; then
  cp /home/ubuntu/.ssh/authorized_keys /home/zarflow/.ssh/authorized_keys
  chown zarflow:zarflow /home/zarflow/.ssh/authorized_keys
  chmod 600 /home/zarflow/.ssh/authorized_keys
fi

echo "== GitHub deploy key (read-only) =="
DEPLOY_KEY=/home/zarflow/.ssh/id_ed25519
if [ ! -f "$DEPLOY_KEY" ]; then
  sudo -u zarflow ssh-keygen -t ed25519 -f "$DEPLOY_KEY" -N "" -C "zarflow-deploy"
fi
echo "---------------------------------------------------------------"
echo "Add this PUBLIC key to GitHub -> repo -> Settings -> Deploy keys"
echo "as a READ-ONLY deploy key (do NOT allow write access):"
echo
cat "${DEPLOY_KEY}.pub"
echo "---------------------------------------------------------------"

echo "== ufw =="
apt-get install -y -qq ufw
ufw allow 22/tcp
ufw allow 80/tcp
ufw allow 443/tcp
ufw --force enable

echo "== unattended-upgrades =="
apt-get install -y -qq unattended-upgrades
dpkg-reconfigure -f noninteractive unattended-upgrades
systemctl enable --now unattended-upgrades

echo "== journald log cap =="
mkdir -p /etc/systemd/journald.conf.d
cat > /etc/systemd/journald.conf.d/zarflow.conf <<'EOF'
[Journal]
SystemMaxUse=200M
EOF
systemctl restart systemd-journald

echo "== Docker log rotation =="
mkdir -p /etc/docker
cat > /etc/docker/daemon.json <<'EOF'
{
  "log-driver": "json-file",
  "log-opts": {
    "max-size": "10m",
    "max-file": "3"
  }
}
EOF
systemctl restart docker

echo "== Done =="
echo "Next: add the deploy key above to GitHub, then 'su - zarflow' and clone the repo."
