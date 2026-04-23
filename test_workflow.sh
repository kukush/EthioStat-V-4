#!/usr/bin/env bash
# ──────────────────────────────────────────────────────────────────────────────
# test_workflow.sh — Run all unit tests for EthioStat sync changes
#
# Usage:
#   ./test_workflow.sh          # Run all unit tests
#   ./test_workflow.sh --quick  # Run only sync-related tests
# ──────────────────────────────────────────────────────────────────────────────
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
ANDROID_DIR="$SCRIPT_DIR/android"

echo "═══════════════════════════════════════════════════════════════════"
echo "  EthioStat — Unit Test Workflow"
echo "═══════════════════════════════════════════════════════════════════"
echo ""

cd "$ANDROID_DIR"

if [[ "${1:-}" == "--quick" ]]; then
    echo "▶ Running sync-related tests only..."
    echo ""

    echo "── 1/3 SyncConstantsTest ──────────────────────────────────────"
    ./gradlew :app:testDebugUnitTest --tests "com.ethiobalance.app.SyncConstantsTest" --no-build-cache 2>&1 | tail -5
    echo ""

    echo "── 2/3 TelecomViewModelTest ───────────────────────────────────"
    ./gradlew :app:testDebugUnitTest --tests "com.ethiobalance.app.ui.viewmodel.TelecomViewModelTest" --no-build-cache 2>&1 | tail -5
    echo ""

    echo "── 3/3 TelecomSenderDetectionTest ─────────────────────────────"
    ./gradlew :app:testDebugUnitTest --tests "com.ethiobalance.app.services.TelecomSenderDetectionTest" --no-build-cache 2>&1 | tail -5
    echo ""
else
    echo "▶ Running ALL unit tests..."
    echo ""
    ./gradlew :app:testDebugUnitTest --no-build-cache 2>&1
fi

echo ""
echo "═══════════════════════════════════════════════════════════════════"
echo "  ✓ Test workflow complete"
echo "═══════════════════════════════════════════════════════════════════"
