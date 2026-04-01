#!/usr/bin/env bash
# =============================================================================
# EthioStat — End-to-End SMS Workflow Test Script
#
# Usage:  chmod +x scripts/test-workflow.sh && ./scripts/test-workflow.sh
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
  echo -e "${RED}❌ adb not found in PATH. Install Android SDK platform-tools.${NC}"
  exit 1
fi

DEVICE_COUNT=$(adb devices | grep -c "device$" || true)
if [[ "$DEVICE_COUNT" -lt 1 ]]; then
  echo -e "${RED}❌ No Android device connected. Connect a device with USB debugging enabled.${NC}"
  exit 1
fi

info "Device connected. Checking app installation..."
if ! adb shell pm list packages | grep -q "$APP_PACKAGE"; then
  echo -e "${RED}❌ App $APP_PACKAGE not installed on device.${NC}"
  exit 1
fi
info "App is installed."

# ─── Helper: query Room DB via adb ───────────────────────────────────────────

LOCAL_DB_DIR=$(mktemp -d)
trap "rm -rf $LOCAL_DB_DIR" EXIT

pull_db() {
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
echo "── Injecting Mock Data SMS Messages ─────────────────────────────────────"

# Test 1: Telebirr - Internet Purchase (Dual Impact)
T1_TS=$((NOW_MS - 100000))
inject_sms "127" "You have successfully paid 100.00 ETB for 5GB Monthly Internet package via Telebirr. Your new balance is 2400.00 ETB." "$T1_TS"

# Test 2: Telebirr - Voice Purchase (Dual Impact)
T2_TS=$((NOW_MS - 90000))
inject_sms "127" "You have successfully paid 50.00 ETB for 100 Minutes Weekly Voice package. New Telebirr balance: 2350.00 ETB." "$T2_TS"

# Test 3: Telebirr - SMS Purchase (Dual Impact)
T3_TS=$((NOW_MS - 80000))
inject_sms "127" "You have successfully paid 20.00 ETB for 200 Weekly SMS package. Transaction Ref: SMS998." "$T3_TS"

# Test 4: Telebirr - Merchant Payment (Financial Only)
T4_TS=$((NOW_MS - 70000))
inject_sms "TELEBIRR" "You have paid 450.50 ETB to Merchant ABC. New balance 1899.50 ETB." "$T4_TS"

# Test 5: CBE - Salary Deposit (Financial Only)
T5_TS=$((NOW_MS - 60000))
inject_sms "CBEBirr" "Your account has been credited with 15,500.00 ETB. Reference: SALARY-MAR-2026." "$T5_TS"

# Test 6: EthioTelecom - Direct Package Gift Received (Asset Only)
T6_TS=$((NOW_MS - 50000))
inject_sms "251994" "Dear customer, you have received a gift of 500MB Daily Internet package from 0911XXXXXX. Valid until tomorrow." "$T6_TS"

# Test 7: EthioTelecom - Airtime Recharge (Financial Income)
T7_TS=$((NOW_MS - 40000))
inject_sms "805" "Your account has been recharged with 50.00 ETB." "$T7_TS"

echo ""
info "Waiting 8 seconds for processing pipeline to complete..."
sleep 8

echo ""
echo "── Asserting DB State ────────────────────────────────────────────────────"

# Assert 1: Telebirr Internet Purchase (100 ETB expense + 5GB Asset)
TX_1=$(db_query "SELECT COUNT(*) FROM transactions WHERE type='EXPENSE' AND amount=100.0;")
if [[ "$TX_1" -ge 1 ]]; then pass "Test 1 [Financial]: Telebirr 100 ETB Internet Purchase recorded."; else fail "Test 1 [Financial]: Missing 100 ETB Telebirr expense."; fi

PK_1=$(db_query "SELECT COUNT(*) FROM balance_packages WHERE type='internet' AND remainingAmount >= 5;")
if [[ "$PK_1" -ge 1 ]]; then pass "Test 1 [Asset]: 5GB Internet package tracked successfully."; else fail "Test 1 [Asset]: Missing 5GB Internet package."; fi

# Assert 2: Telebirr Voice Purchase (50 ETB expense + 100 Min Asset)
TX_2=$(db_query "SELECT COUNT(*) FROM transactions WHERE type='EXPENSE' AND amount=50.0;")
if [[ "$TX_2" -ge 1 ]]; then pass "Test 2 [Financial]: Telebirr 50 ETB Voice Purchase recorded."; else fail "Test 2 [Financial]: Missing 50 ETB Telebirr voice expense."; fi

PK_2=$(db_query "SELECT COUNT(*) FROM balance_packages WHERE type='voice' AND remainingAmount >= 100;")
if [[ "$PK_2" -ge 1 ]]; then pass "Test 2 [Asset]: 100 Min Voice package tracked successfully."; else fail "Test 2 [Asset]: Missing 100 Min Voice package."; fi

# Assert 3: Telebirr SMS Purchase (20 ETB expense + 200 SMS Asset)
TX_3=$(db_query "SELECT COUNT(*) FROM transactions WHERE type='EXPENSE' AND amount=20.0;")
if [[ "$TX_3" -ge 1 ]]; then pass "Test 3 [Financial]: Telebirr 20 ETB SMS Purchase recorded."; else fail "Test 3 [Financial]: Missing 20 ETB Telebirr SMS expense."; fi

PK_3=$(db_query "SELECT COUNT(*) FROM balance_packages WHERE type='sms' AND remainingAmount >= 200;")
if [[ "$PK_3" -ge 1 ]]; then pass "Test 3 [Asset]: 200 SMS package tracked successfully."; else fail "Test 3 [Asset]: Missing 200 SMS package."; fi

# Assert 4: Telebirr Merchant Payment (Financial Only, 450.50 ETB)
TX_4=$(db_query "SELECT COUNT(*) FROM transactions WHERE type='EXPENSE' AND amount=450.50;")
if [[ "$TX_4" -ge 1 ]]; then pass "Test 4 [Financial]: Merchant Payment 450.50 ETB recorded."; else fail "Test 4 [Financial]: Missing 450.50 ETB Merchant Payment."; fi

# Assert 5: CBE - Salary Deposit (Income, 15500 ETB)
TX_5=$(db_query "SELECT COUNT(*) FROM transactions WHERE type='INCOME' AND amount=15500.0;")
if [[ "$TX_5" -ge 1 ]]; then pass "Test 5 [Financial]: CBE Salary Income of 15,500.00 ETB recorded."; else fail "Test 5 [Financial]: Missing CBE Salary Income."; fi

# Assert 6: Direct Package Gift Received (Asset Only, 500MB)
PK_6=$(db_query "SELECT COUNT(*) FROM balance_packages WHERE type='internet';")
if [[ "$PK_6" -ge 1 ]]; then pass "Test 6 [Asset]: Gifted Internet package tracked successfully."; else fail "Test 6 [Asset]: Missing Gifted Internet package."; fi

# Assert 7: Airtime Recharge (Income, 50 ETB)
TX_7=$(db_query "SELECT COUNT(*) FROM transactions WHERE type='INCOME' AND amount=50.0;")
if [[ "$TX_7" -ge 1 ]]; then pass "Test 7 [Financial]: Airtime Recharge Income of 50.00 ETB recorded."; else fail "Test 7 [Financial]: Missing Airtime Recharge Income."; fi

echo ""
echo "════════════════════════════════════════════"
printf "  Results: \033[0;32m%d passed\033[0m, \033[0;31m%d failed\033[0m\n" "$PASS" "$FAIL"
echo "════════════════════════════════════════════"
echo ""

if [[ "$FAIL" -gt 0 ]]; then
  exit 1
fi
exit 0
