#!/usr/bin/env bash
set -euo pipefail

log() {
  printf '\n==> %s\n' "$1"
}

require_sudo() {
  if ! command -v sudo >/dev/null 2>&1; then
    echo "sudo is required for package installation." >&2
    exit 1
  fi
}

ensure_apt_package() {
  local pkg="$1"
  if dpkg -s "$pkg" >/dev/null 2>&1; then
    echo "$pkg is already installed."
    return
  fi

  echo "Installing $pkg..."
  sudo apt-get install -y "$pkg"
}

log "Validating WSL environment"
if grep -qi microsoft /proc/version; then
  echo "Running inside WSL."
else
  echo "This script is intended for Ubuntu/WSL." >&2
  exit 1
fi

require_sudo

log "Updating apt package index"
sudo apt-get update

log "Installing required packages"
ensure_apt_package openjdk-21-jdk
ensure_apt_package maven
ensure_apt_package postgresql-client
ensure_apt_package curl

log "Version check"
java -version
mvn -version
psql --version

log "Setup summary"
echo "WSL-side setup completed."
echo "Next steps:"
echo "1. From /mnt/d/MicroChaos run: docker-compose up -d postgres"
echo "2. Load schema/seed if needed"
echo "3. Start backend from backend/"
