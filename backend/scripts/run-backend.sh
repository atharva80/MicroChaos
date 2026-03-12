#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKEND_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"

mkdir -p "${BACKEND_DIR}/out"
find "${BACKEND_DIR}/src" -name "*.java" -print0 | xargs -0 javac -d "${BACKEND_DIR}/out"

PORT="${PORT:-8080}" java -cp "${BACKEND_DIR}/out" com.microchaos.backend.MicroChaosBackendApplication
