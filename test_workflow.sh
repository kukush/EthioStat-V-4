#!/usr/bin/env bash
# ──────────────────────────────────────────────────────────────────────────────
# test_workflow.sh — Run all unit tests for EthioStat
#
# Usage:
#   ./test_workflow.sh               # Run all unit tests
#   ./test_workflow.sh --quick       # Run only sync-related tests
#   ./test_workflow.sh --permission  # Run only permission-handling tests
#
# Permission test cases verify:
#   - No crash when user installs without granting permissions
#   - All SMS-dependent features disabled when permission denied
#   - No default transaction sources seeded without permission
#   - Post-grant recovery: seeds defaults + scans history
#   - handleSync sets error instead of crashing
#   - TelecomViewModel works correctly with permission granted (mock)
# ──────────────────────────────────────────────────────────────────────────────
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
ANDROID_DIR="$SCRIPT_DIR/android"

echo "═══════════════════════════════════════════════════════════════════"
echo "  EthioStat — Unit Test Workflow"
echo "═══════════════════════════════════════════════════════════════════"
echo ""

cd "$ANDROID_DIR"

if [[ "${1:-}" == "--permission" ]]; then
    echo "▶ Running permission-handling tests only..."
    echo ""

    echo "── 1/2 PermissionGuardTest ──────────────────────────────────"
    echo "  Cases: permission-denied (no crash, 0 returns, no seed,"
    echo "         skip scan, error message) + permission-granted"
    echo "         (defaults seeded, banks resolve, telecom senders)"
    ./gradlew :app:testDebugUnitTest --tests "com.ethiobalance.app.ui.viewmodel.PermissionGuardTest" --no-build-cache 2>&1 | tail -5
    echo ""

    echo "── 2/2 TelecomViewModelTest ─────────────────────────────────"
    echo "  Cases: handleSync with permission (mock hasSmsPermission=true),"
    echo "         dialUssd called, isSyncing transitions, error handling"
    ./gradlew :app:testDebugUnitTest --tests "com.ethiobalance.app.ui.viewmodel.TelecomViewModelTest" --no-build-cache 2>&1 | tail -5
    echo ""

elif [[ "${1:-}" == "--quick" ]]; then
    echo "▶ Running sync-related tests only..."
    echo ""

    echo "── 1/4 SyncConstantsTest ──────────────────────────────────────"
    ./gradlew :app:testDebugUnitTest --tests "com.ethiobalance.app.SyncConstantsTest" --no-build-cache 2>&1 | tail -5
    echo ""

    echo "── 2/4 TelecomViewModelTest ───────────────────────────────────"
    ./gradlew :app:testDebugUnitTest --tests "com.ethiobalance.app.ui.viewmodel.TelecomViewModelTest" --no-build-cache 2>&1 | tail -5
    echo ""

    echo "── 3/4 TelecomSenderDetectionTest ─────────────────────────────"
    ./gradlew :app:testDebugUnitTest --tests "com.ethiobalance.app.services.TelecomSenderDetectionTest" --no-build-cache 2>&1 | tail -5
    echo ""

    echo "── 4/4 PermissionGuardTest ────────────────────────────────────"
    ./gradlew :app:testDebugUnitTest --tests "com.ethiobalance.app.ui.viewmodel.PermissionGuardTest" --no-build-cache 2>&1 | tail -5
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
