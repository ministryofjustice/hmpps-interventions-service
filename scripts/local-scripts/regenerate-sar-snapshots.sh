#!/usr/bin/env bash
# regenerate-sar-snapshots.sh
#
# Regenerates the SAR test snapshot files by running the SAR contract integration
# tests with SAR_GENERATE_ACTUAL=true, then copying the resulting .log files into
# the snapshot locations used by the snapshot tests.
#
# Usage:
#   ./scripts/local-scripts/regenerate-sar-snapshots.sh
#
# Prerequisites:
#   - A running PostgreSQL database (see docker-compose.yml or README for local setup)
#   - All environment variables configured for the "test" and "local" Spring profiles

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"

cd "${PROJECT_ROOT}"

echo "▶ Running SAR contract tests with SAR_GENERATE_ACTUAL=true ..."
SAR_GENERATE_ACTUAL=true ./gradlew test --tests "*SarContractIntegrationTest*" --rerun-tasks

echo ""
echo "▶ Copying generated snapshots ..."

SAR_SNAPSHOT_DIR="src/test/resources/sar"
SAR_GENERATED_DIR="src/test/resources"

copy_if_exists() {
  local src="$1"
  local dst="$2"
  if [[ -f "${src}" ]]; then
    cp "${src}" "${dst}"
    echo "  ✔ ${dst}"
  else
    echo "  ✘ Source file not found: ${src} — skipping"
  fi
}

copy_if_exists "${SAR_GENERATED_DIR}/sar-api-response.json.log"         "${SAR_SNAPSHOT_DIR}/sar-api-response.json"
copy_if_exists "${SAR_GENERATED_DIR}/sar-generated-report.html.log"     "${SAR_SNAPSHOT_DIR}/sar-expected-render-result.html"
copy_if_exists "${SAR_GENERATED_DIR}/entity-schema.json.log"            "${SAR_SNAPSHOT_DIR}/entity-schema-snapshot.json"

echo ""
echo "▶ Running SAR contract tests again to verify snapshots ..."
./gradlew test --tests "*SarContractIntegrationTest*"

echo ""
echo "✅ SAR snapshots regenerated and verified successfully."
echo ""
echo "Review the generated PDF report at: build/test-generated/sar-generated-report.pdf"

