#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DEMO_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"

mkdir -p "${DEMO_DIR}/out"
find "${DEMO_DIR}/src" -name "*.java" -print0 | xargs -0 javac -d "${DEMO_DIR}/out"
java -cp "${DEMO_DIR}/out" com.microchaos.demo.DemoStackLauncher
