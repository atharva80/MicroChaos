#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
FRONTEND_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
PORT="${PORT:-5173}"

cd "${FRONTEND_DIR}"
python3 -m http.server "${PORT}"
