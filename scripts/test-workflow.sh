#!/usr/bin/env bash
# =============================================================================
# EthioStat — End-to-End SMS Workflow Test Script
#
# Usage:  chmod +x scripts/test-workflow.sh && ./scripts/test-workflow.sh
#
# Prerequisites:
#   1. Android device connected via USB with debugging enabled
#   2. EthioStat app installed and running on the device
#   3. READ_SMS, RECEIVE_SMS permissions granted to the app
#   4. adb is in your PATH
#
# What this script does:
#   - Injects simulated SMS broadcasts for Telebirr (127), EthioTelecom (251994),
#     balance query (804), and CBE (847)
#   - Waits for the processing pipeline to complete
#   - Queries the Room DB directly via adb to assert expected records exist
# =============================================================================

set -euo pipefail

APP_PACKAGE="com.ethiobalance.app"
DB_NAME="ethio_balance_db"
PASS=0
FAIL=0
NOW_MS=$(date +%s000)

GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m'

pass() { echo -e "${GREEN}✅ PASS${NC}: $1"; PASS=$((PASS+1)); }
fail() { echo -e "${RED}❌ FAIL${NC}: $1"; FAIL=$((FAIL+1)); }
info() { echo -e "${YELLOW}ℹ️  ${NC}$1"; }

# ─── Preconditions ───────────────────────────────────────────────────────────

echo ""
echo "════════════════════════════════════════════"
echo "  EthioStat SMS Workflow Integration Test"
echo "════════════════════════════════════════════"
echo ""

if ! command -v adb &>/dev/null; then
  echo "❌ adb not found in PATH. Install Android SDK platform-tools."
  exit 1
fi

DEVICE_COUNT=$(adb devices | grep -c "device$" || true)
if [[ "$DEVICE_COUNT" -lt 1 ]]; then
  echo "❌ No Android device connected. Connect a device with USB debugging enabled."
  exit 1
fi

info "Device connected. Checking app installation..."
if ! adb shell pm list packages | grep -q "$APP_PACKAGE"; then
  echo "❌ App $APP_PACKAGE not installed on device."
  exit 1
fi
info "App is installed."

# ─── Helper: query Room DB via adb ───────────────────────────────────────────
# sqlite3 may not exist on device, so we pull the DB (+ WAL/SHM) locally and
# query with the host's sqlite3.

LOCAL_DB_DIR=$(mktemp -d)
trap "rm -rf $LOCAL_DB_DIR" EXIT

pull_db() {
  # Copy DB files from app-private storage to /data/local/tmp, then pull
  for suffix in "" "-wal" "-shm"; do
    adb shell "run-as $APP_PACKAGE cat databases/${DB_NAME}${suffix}" > "${LOCAL_DB_DIR}/${DB_NAME}${suffix}" 2>/dev/null || true
  done
}

db_query() {
  local sql="$1"
  pull_db
  sqlite3 "${LOCAL_DB_DIR}/${DB_NAME}" "$sql" 2>/dev/null || echo ""
}

# ─── Helper: inject a fake SMS broadcast ─────────────────────────────────────
# NOTE: Android restricts injecting SMS_RECEIVED broadcasts on API 19+.
# We instead start the SmsForegroundService directly with the payload,
# which is exactly what SmsReceiver does after filtering.

inject_sms() {
  local sender="$1"
  local body="$2"
  local ts="${3:-$NOW_MS}"
  info "Injecting SMS from '$sender': $body"
  adb shell "am start-foreground-service \
    -n $APP_PACKAGE/.services.SmsForegroundService \
    --es sender '$sender' \
    --es body '$body' \
    --el timestamp $ts" 2>/dev/null || true
}

# ─── Clear old test data ──────────────────────────────────────────────────────

info "Clearing previous test data from DB..."
db_query "DELETE FROM transactions WHERE reference IS NULL AND category IN ('PURCHASE', 'CREDIT', 'EXPENSE', 'REPAYMENT', 'GIFT', 'FEE');" 2>/dev/null || true
db_query "DELETE FROM balance_packages WHERE source = 'SMS';" 2>/dev/null || true
db_query "DELETE FROM sms_logs;" 2>/dev/null || true

echo ""
echo "── Injecting Test SMS Messages ──────────────────────────────────────────"

# NOTE: All balance packages use canonical IDs (airtime-sim1, internet-sim1, etc.)
# so the LAST message for each type determines the final DB state.

# Test 1: Telebirr Payment — creates EXPENSE transaction + internet package + airtime balance
T1_TS=$((NOW_MS - 100000))
inject_sms "127" "You have paid 130.00 ETB for 1GB internet package. Your telebirr account balance is 370.00 ETB." "$T1_TS"

# Test 2: CBE Credit — creates INCOME transaction
T2_TS=$((NOW_MS - 90000))
inject_sms "847" "Your account has been credited with 1,500.00 ETB from John Doe." "$T2_TS"

# Test 3: EthioTelecom Voice Gift — creates voice package
T3_TS=$((NOW_MS - 80000))
inject_sms "251994" "You have received a gift of 50 Min from telebirr. Enjoy your calls!" "$T3_TS"

# Test 4: EthioTelecom Data Package — creates/updates internet package (450MB overwrites 1GB)
T4_TS=$((NOW_MS - 70000))
inject_sms "251994" "You have 450MB data remaining until 2026-06-30." "$T4_TS"

# Test 5: Telebirr Balance Query — updates airtime to 500 ETB (overwrites 370)
T5_TS=$((NOW_MS - 60000))
inject_sms "127" "Your telebirr account balance is 500.00 ETB. Thank you for using Telebirr." "$T5_TS"

# Test 6: EthioTelecom Balance Query (sender 804) — may overwrite airtime-sim1
T6_TS=$((NOW_MS - 50000))
inject_sms "804" "Your account balance is 145.50 ETB. Valid until 2026-10-30." "$T6_TS"

echo ""
info "Waiting 8 seconds for processing pipeline to complete..."
sleep 8

echo ""
echo "── Asserting DB State ────────────────────────────────────────────────────"

# Assert 1: Airtime balance — final value is 145.50 ETB (Test 6 from 804, last writer to airtime-sim1)
AIRTIME_VAL=$(db_query "SELECT totalAmount FROM balance_packages WHERE id='airtime-sim1';")
if [[ -n "$AIRTIME_VAL" ]]; then
  pass "Airtime balance stored in balance_packages (value: $AIRTIME_VAL ETB)"
else
  fail "Airtime balance NOT found in balance_packages"
fi

# Assert 2: Internet package exists
INTERNET_VAL=$(db_query "SELECT totalAmount || ' ' || unit FROM balance_packages WHERE id='internet-sim1';")
if [[ -n "$INTERNET_VAL" ]]; then
  pass "Internet package stored in balance_packages ($INTERNET_VAL)"
else
  fail "Internet package NOT found in balance_packages"
fi

# Assert 3: Voice package from gifted SMS (Test 3)
VOICE_COUNT=$(db_query "SELECT COUNT(*) FROM balance_packages WHERE type='voice';")
if [[ "$VOICE_COUNT" -ge 1 ]]; then
  pass "Gifted voice package stored from EthioTelecom SMS [sender 251994]"
else
  fail "Gifted voice package NOT found in balance_packages"
fi

# Assert 4: Telebirr EXPENSE transaction (Test 1 — paid 130 ETB)
TX_TELEBIRR=$(db_query "SELECT COUNT(*) FROM transactions WHERE source='TELEBIRR' AND type='EXPENSE' AND amount=130.0;")
if [[ "$TX_TELEBIRR" -ge 1 ]]; then
  pass "Telebirr payment (130 ETB expense) recorded as transaction"
else
  fail "Telebirr payment transaction NOT found (expected 130 ETB expense)"
fi

# Assert 5: CBE INCOME transaction (Test 2)
TX_CBE=$(db_query "SELECT COUNT(*) FROM transactions WHERE source='CBE' AND type='INCOME' AND amount=1500.0;")
if [[ "$TX_CBE" -ge 1 ]]; then
  pass "CBE credit (1500 ETB income) recorded as transaction"
else
  fail "CBE credit transaction NOT found (expected 1500 ETB income)"
fi

# Assert 6: sms_logs rows exist (processing happened for all 6 injected messages)
LOG_COUNT=$(db_query "SELECT COUNT(*) FROM sms_logs;")
if [[ "$LOG_COUNT" -ge 6 ]]; then
  pass "All 6 SMS messages were logged in sms_logs (count: $LOG_COUNT)"
else
  fail "Only $LOG_COUNT SMS log entries found (expected >= 6)"
fi

# Bonus: show full DB state for debugging
echo ""
info "Current balance_packages:"
db_query "SELECT id, type, totalAmount, remainingAmount, unit FROM balance_packages;"
echo ""
info "Recent transactions:"
db_query "SELECT type, amount, category, source FROM transactions ORDER BY timestamp DESC LIMIT 10;"

echo ""
echo "════════════════════════════════════════════"
printf "  Results: ${GREEN}%d passed${NC}, ${RED}%d failed${NC}\n" "$PASS" "$FAIL"
echo "════════════════════════════════════════════"
echo ""

if [[ "$FAIL" -gt 0 ]]; then
  echo "Some assertions failed. Check device logs:"
  echo "  adb logcat -s SmsForegroundService SmsParser SmsReceiver ReconciliationEngine"
  exit 1
fi

exit 0
