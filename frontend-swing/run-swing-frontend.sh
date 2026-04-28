#!/bin/bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "${SCRIPT_DIR}"

API_BASE="${API_BASE:-http://localhost:8080/api}"

echo "Starting MicroChaos Swing Frontend..."
echo "API Base: $API_BASE"

mvn clean compile exec:java \
  -Dexec.mainClass="com.microchaos.swing.MicroChaosSwingApp" \
  -Dapi.base="$API_BASE" \
  -q
