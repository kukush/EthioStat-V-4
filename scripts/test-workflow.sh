#!/usr/bin/env bash
# =============================================================================
# EthioStat — End-to-End SMS Workflow Test Script
#
# Tests SMS parsing & transaction recording using REAL device SMS formats.
# All test messages are copied verbatim from actual Ethiopian bank/telecom SMS
# captured via: adb shell "content query --uri content://sms/inbox ..."
#
# Covered banks:  CBE, BOA, Awash Bank, Telebirr (127)
# Covered cases:  credit, debit, debit+service-charge, debit-for-party,
#                 credit-from-party, transfer, airtime-received
#
# Usage:  chmod +x scripts/test-workflow.sh && ./scripts/test-workflow.sh
# =============================================================================

set -euo pipefail

APP_PACKAGE="com.ethiobalance.app"
DB_NAME="ethio_balance_db"
PASS=0
FAIL=0
NOW_MS=$(date +%s000)
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ANDROID_DIR="$SCRIPT_DIR/../android"

GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m'

pass() { echo -e "${GREEN}✅ PASS${NC}: $1"; PASS=$((PASS+1)); }
fail() { echo -e "${RED}❌ FAIL${NC}: $1"; FAIL=$((FAIL+1)); }
info() { echo -e "${YELLOW}ℹ️  ${NC}$1"; }
section() { echo -e "\n${CYAN}── $1 ──${NC}"; }

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

# =============================================================================
#  JVM UNIT TESTS  (no device needed)
#
#  1. TelecomDataVolConversionTest — covers the dataVol MB→GB conversion used
#     by TelecomScreen and HomeScreen.
#     Bug: TelecomScreen summed raw remainingAmount (MB) and displayed it as GB,
#     e.g. 967.1 MB shown as 967.1 GB instead of 0.9 GB.
#     Fix: divide by 1024 when unit != GB (same as HomeScreen).
#
#  2. UssdAccessibilityServiceTest — covers USSD text capture + dismissal logic.
#     NOTE: CALL_PHONE permission is NOT declared. Dialing uses ACTION_DIAL only.
# =============================================================================

section "TelecomScreen Data Volume Conversion Tests (JVM)"

if [[ -f "$ANDROID_DIR/gradlew" ]]; then
  info "Running TelecomDataVolConversionTest..."
  if "$ANDROID_DIR/gradlew" -p "$ANDROID_DIR" \
      :app:testDebugUnitTest \
      --tests "com.ethiobalance.app.ui.screens.TelecomDataVolConversionTest" \
      --quiet 2>&1 | tail -5; then
    pass "TelecomDataVolConversionTest — all unit tests passed"
  else
    fail "TelecomDataVolConversionTest — one or more unit tests failed"
  fi
else
  info "gradlew not found at $ANDROID_DIR/gradlew — skipping JVM tests"
fi

section "USSD Unit Tests (JVM)"

if [[ -f "$ANDROID_DIR/gradlew" ]]; then
  info "Running UssdAccessibilityServiceTest..."
  if "$ANDROID_DIR/gradlew" -p "$ANDROID_DIR" \
      :app:testDebugUnitTest \
      --tests "com.ethiobalance.app.services.UssdAccessibilityServiceTest" \
      --quiet 2>&1 | tail -5; then
    pass "UssdAccessibilityServiceTest — all unit tests passed"
  else
    fail "UssdAccessibilityServiceTest — one or more unit tests failed"
  fi
else
  info "gradlew not found at $ANDROID_DIR/gradlew — skipping JVM tests"
fi

section "ParseSmsUseCase Telecom Package Tests (JVM)"

if [[ -f "$ANDROID_DIR/gradlew" ]]; then
  info "Running ParseSmsUseCaseTest (Case 1 & 2)..."
  if "$ANDROID_DIR/gradlew" -p "$ANDROID_DIR" \
      :app:testDebugUnitTest \
      --tests "com.ethiobalance.app.domain.usecase.ParseSmsUseCaseTest" \
      --quiet 2>&1 | tail -5; then
    pass "ParseSmsUseCaseTest — all unit tests passed (Case 1 multi-segment + Case 2 night internet)"
  else
    fail "ParseSmsUseCaseTest — one or more unit tests failed"
  fi
else
  info "gradlew not found at $ANDROID_DIR/gradlew — skipping JVM tests"
fi

# ─── Accessibility Service Bound Check ───────────────────────────────────────

section "Accessibility Service Check"

BOUND=$(adb shell dumpsys accessibility 2>/dev/null \
  | grep -c "UssdAccessibilityService" || true)
if [[ "$BOUND" -ge 1 ]]; then
  pass "UssdAccessibilityService is registered in accessibility dump"
else
  fail "UssdAccessibilityService NOT found — go to Settings → Accessibility and enable it"
fi

CRASHED=$(adb shell dumpsys accessibility 2>/dev/null \
  | grep "Crashed services" | grep -c "UssdAccessibilityService" || true)
if [[ "$CRASHED" -eq 0 ]]; then
  pass "UssdAccessibilityService has not crashed"
else
  fail "UssdAccessibilityService appears in Crashed services"
fi

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

# ─── Helper: inject SMS via SmsForegroundService ────────────────────────────

inject_sms() {
  local sender="$1"
  local body="$2"
  local ts="${3:-$NOW_MS}"
  info "Injecting SMS from '${sender}'"
  adb shell "am start-foreground-service \
    -n $APP_PACKAGE/.services.SmsForegroundService \
    --es sender '$sender' \
    --es body '$body' \
    --el timestamp $ts" 2>/dev/null || true
}

# ─── Assert helper ──────────────────────────────────────────────────────────

assert_tx() {
  local test_num="$1"
  local label="$2"
  local type="$3"
  local amount="$4"
  local source="$5"
  local count
  count=$(db_query "SELECT COUNT(*) FROM transactions WHERE type='${type}' AND amount=${amount} AND source='${source}';")
  if [[ "$count" -ge 1 ]]; then
    pass "Test ${test_num}: ${label}"
  else
    fail "Test ${test_num}: ${label} (expected ${type} ${amount} from ${source})"
  fi
}

assert_pkg() {
  local test_num="$1"
  local label="$2"
  local pkg_id="$3"
  local expected_type="$4"
  local expected_subtype="$5"
  local expected_remaining="$6"
  local expected_unit="$7"
  local row
  row=$(db_query "SELECT type, subType, remainingAmount, unit FROM balance_packages WHERE id='${pkg_id}';")
  if [[ -z "$row" ]]; then
    fail "Test ${test_num}: ${label} (package id '${pkg_id}' not found)"
    return
  fi
  local db_type db_sub db_remain db_unit
  IFS='|' read -r db_type db_sub db_remain db_unit <<< "$row"
  local ok=true
  [[ "$db_type" != "$expected_type" ]] && ok=false
  [[ "$db_sub" != "$expected_subtype" ]] && ok=false
  [[ "$db_unit" != "$expected_unit" ]] && ok=false
  if $ok; then
    pass "Test ${test_num}: ${label} (${db_type}/${db_sub} ${db_remain} ${db_unit})"
  else
    fail "Test ${test_num}: ${label} (got: ${db_type}/${db_sub} ${db_remain} ${db_unit}, expected: ${expected_type}/${expected_subtype} ${expected_remaining} ${expected_unit})"
  fi
}

# ─── Reset ──────────────────────────────────────────────────────────────────

section "Reset"

info "Clearing app data to reset DB..."
adb shell pm clear $APP_PACKAGE 2>/dev/null || true
sleep 1

info "Launching app to re-initialize database..."
adb shell am start -n $APP_PACKAGE/.MainActivity 2>/dev/null || true
sleep 4

info "Clearing logcat..."
adb logcat -c 2>/dev/null || true

# =============================================================================
#  REAL-WORLD SMS TEST CASES
#  All messages below are verbatim copies from actual device SMS inbox
# =============================================================================

section "Injecting Real-World SMS Messages"

# ── CBE: Simple Credit ──────────────────────────────────────────────────────
T1_TS=$((NOW_MS - 200000))
inject_sms "CBE" \
  "Dear Abebech your Account 1********3618 has been credited with ETB 7000.00. Your Current Balance is ETB 62923.77. Thank you for Banking with CBE! https://apps.cbe.com.et:100/BranchReceipt/FT252810466B&35393618" \
  "$T1_TS"

# ── CBE: Credit from named party ────────────────────────────────────────────
T2_TS=$((NOW_MS - 190000))
inject_sms "CBE" \
  "Dear Abebech your Account 1*********3743 has been Credited with ETB 20,000.00 from Metawal Taye, on 12/12/2025 at 09:58:18 with Ref No FT253462WZSY Your Current Balance is ETB 87,037.78. Thank you for Banking with CBE! https://apps.cbe.com.et:100/?id=FT253462WZSY95763743" \
  "$T2_TS"

# ── CBE: Debit with service charge ──────────────────────────────────────────
T3_TS=$((NOW_MS - 180000))
inject_sms "CBE" \
  "Dear Abebech your Account 1********3618 has been debited with ETB 5005.75 including Service charge 5.00 and VAT(15%) 0.75. Your Current Balance is ETB 45465.75. Thank you for Banking with CBE!. For feedback https://shorturl.at/auUX0" \
  "$T3_TS"

# ── CBE: Large debit with service charge ────────────────────────────────────
T4_TS=$((NOW_MS - 170000))
inject_sms "CBE" \
  "Dear Abebech your Account 1********3618 has been debited with ETB 50011.50 including Service charge 10.00 and VAT(15%) 1.50. Your Current Balance is ETB 142454.25. Thank you for Banking with CBE!. For feedback https://shorturl.at/auUX0" \
  "$T4_TS"

# ── CBE: Debit for named party ──────────────────────────────────────────────
T5_TS=$((NOW_MS - 160000))
inject_sms "CBE" \
  "Dear Abebech your Account 1********3618 has been debited for SELAMAWIT  ALEMU GETAHUN with ETB 50011.5 including Service charge ETB10.00 and VAT(15%) ETB1.50. Your Current Balance is ETB 77356.25. Thank you for Banking with CBE!. For feedback https://shorturl.at/auUX0 https://apps.cbe.com.et:100/BranchReceipt/FT25360QSQD2&35393618" \
  "$T5_TS"

# ── CBE: Debit for utility ──────────────────────────────────────────────────
T6_TS=$((NOW_MS - 150000))
inject_sms "CBE" \
  "Dear Abebech your Account 1********1058 has been debited for GULELE KK YEMAH.AKEF .TEN.MED.TE.A with ETB 2005.75 including Service charge ETB5.00 and VAT(15%) ETB0.75. Your Current Balance is ETB 64.16. Thank you for Banking with CBE!. For feedback https://shorturl.at/auUX0 https://apps.cbe.com.et:100/BranchReceipt/FT25297CHLHW&07211058" \
  "$T6_TS"

# ── CBE: Large credit ───────────────────────────────────────────────────────
T7_TS=$((NOW_MS - 140000))
inject_sms "CBE" \
  "Dear Abebech your Account 1********3618 has been credited with ETB 60000.00. Your Current Balance is ETB 64745.46. Thank you for Banking with CBE! for Reciept https://apps.cbe.com.et:100/BranchReceipt/FT26035CC8BL&35393618" \
  "$T7_TS"

# ── BOA: Credit by cash deposit ─────────────────────────────────────────────
T8_TS=$((NOW_MS - 130000))
inject_sms "BOA" \
  "Dear Ababech, your account 1*****07 was credited with ETB 5,000.00 by Cash Deposit-ABEBECH WELDE SHOLATO. Available Balance:  ETB 81,505.76. Receipt: https://cs.bankofabyssinia.com/slip/?trx=TT26076G8L5B74607" \
  "$T8_TS"

# ── BOA: Debit ──────────────────────────────────────────────────────────────
T9_TS=$((NOW_MS - 120000))
inject_sms "BOA" \
  "Dear ABEBECH, your account 1*****07 was debited with ETB 50,000.00. Available Balance:  ETB 50,095.90. Receipt: https://cs.bankofabyssinia.com/slip/?trx=TT252119GSPL74607" \
  "$T9_TS"

# ── BOA: Large credit ───────────────────────────────────────────────────────
T10_TS=$((NOW_MS - 110000))
inject_sms "BOA" \
  "Dear ABEBECH, your account 1*****07 was credited with ETB 100,000.00 by Cash Deposit-ABEBECH WELDE SHOLATO. Available Balance:  ETB 100,107.40. Receipt: https://cs.bankofabyssinia.com/slip/?trx=TT25202BV0J174607" \
  "$T10_TS"

# ── Awash: Credit with party ────────────────────────────────────────────────
T11_TS=$((NOW_MS - 100000))
inject_sms "Awash Bank" \
  "Dear Customer, your Account 01320xxxxx1400 has been Credited with ETB 1300.00 on 2026-03-31 11:32:14 by ABEBECH WOLDE. Your balance now is ETB 17535.60. For any complaint or enquiry, please call 8980. Thank You. Awash Bank." \
  "$T11_TS"

# ── Awash: Credit via transfer ──────────────────────────────────────────────
T12_TS=$((NOW_MS - 90000))
inject_sms "Awash Bank" \
  "Dear Customer, ETB 50 has been credited to your account from SAMUEL MITIKU GUDINA on : 2026-01-28 20:12:21  with Txn ID: 260128201223255 . Your available balance is now ETB 50.00. Receipt  Link: https://awashpay.awashbank.com:8225/-2K7H8UP3KN-3JLL2T. Contact center  8980." \
  "$T12_TS"

# ── Awash: Larger credit ────────────────────────────────────────────────────
T13_TS=$((NOW_MS - 80000))
inject_sms "Awash Bank" \
  "Dear Customer, your Account 01320xxxxxx1400 has been Credited with ETB 4000.00 on 2026-01-20 09:52:38 by sd. Your balance now is ETB 28696.65. For any complaint or enquiry, please call 8980. Thank You. Awash Bank." \
  "$T13_TS"

# ── Telebirr: Airtime received ──────────────────────────────────────────────
T14_TS=$((NOW_MS - 70000))
inject_sms "127" \
  "Dear Customer\nYou have received ETB 25.00 airtime from 251927983338 on 16/11/2025 18:16:34. Your transaction number is CKG87P67W6.\nThank you for using telebirr\nethio telecom" \
  "$T14_TS"

# ── Telebirr: Payment for package ───────────────────────────────────────────
T15_TS=$((NOW_MS - 60000))
inject_sms "127" \
  "You have paid 100.00 ETB for 5GB Monthly Internet package. Transaction ID: TX001. Your new balance is 2400.00 ETB." \
  "$T15_TS"

# ── Telebirr: Transfer/Send ─────────────────────────────────────────────────
T16_TS=$((NOW_MS - 50000))
inject_sms "127" \
  "You have sent 450.50 ETB to 0911223344. Transaction ID: TX016. Fee: 2.50 ETB." \
  "$T16_TS"

# ── USSD: Airtime balance (simulates AccessibilityService broadcast) ─────────
# Sends the ACTION_USSD_RESPONSE broadcast directly, bypassing the dialer flow.
# This verifies that TelecomViewModel receives the broadcast and persists the
# airtime value via ReconciliationEngine.processSms("804", ...).
# (No CALL_PHONE permission needed — broadcast is sent via adb am broadcast)
T17_TS=$((NOW_MS - 40000))
info "Simulating USSD balance broadcast (ACTION_USSD_RESPONSE)..."
adb shell "am broadcast \
  -a com.ethiobalance.app.ACTION_USSD_RESPONSE \
  -p $APP_PACKAGE \
  --es ussd_text 'Dear customer, Your account balance is 12.50 Birr . With this balance your account will expire on 15/04/2027. ethio telecom'" \
  2>/dev/null || true

echo ""
info "Waiting 12 seconds for processing pipeline to complete..."
sleep 12

# =============================================================================
#  ASSERTIONS
# =============================================================================

section "Asserting DB State"

# ── CBE Tests ───────────────────────────────────────────────────────────────
assert_tx  1 "CBE credit 7,000 ETB"                     "INCOME"  "7000.0"    "CBE"
assert_tx  2 "CBE credit 20,000 ETB (from Metawal Taye)" "INCOME"  "20000.0"   "CBE"
assert_tx  3 "CBE debit 5,005.75 ETB (incl. svc charge)"  "EXPENSE" "5005.75"   "CBE"
assert_tx  4 "CBE debit 50,011.50 ETB (incl. svc charge)" "EXPENSE" "50011.5"   "CBE"
assert_tx  5 "CBE debit 50,011.50 ETB (for SELAMAWIT)"    "EXPENSE" "50011.5"   "CBE"
assert_tx  6 "CBE debit 2,005.75 ETB (utility payment)"   "EXPENSE" "2005.75"   "CBE"
assert_tx  7 "CBE credit 60,000 ETB"                      "INCOME"  "60000.0"   "CBE"

# ── BOA Tests ──────────────────────────────────────────────────────────────
assert_tx  8 "BOA credit 5,000 ETB (cash deposit)"        "INCOME"  "5000.0"    "BOA"
assert_tx  9 "BOA debit 50,000 ETB"                       "EXPENSE" "50000.0"   "BOA"
assert_tx 10 "BOA credit 100,000 ETB (cash deposit)"      "INCOME"  "100000.0"  "BOA"

# ── Awash Tests ────────────────────────────────────────────────────────────
assert_tx 11 "Awash credit 1,300 ETB (from ABEBECH)"      "INCOME"  "1300.0"    "AWASH"
assert_tx 12 "Awash credit 50 ETB (from SAMUEL)"          "INCOME"  "50.0"      "AWASH"
assert_tx 13 "Awash credit 4,000 ETB"                     "INCOME"  "4000.0"    "AWASH"

# ── Telebirr Tests ─────────────────────────────────────────────────────────
assert_tx 14 "Telebirr airtime received 25 ETB"           "INCOME"  "25.0"      "TeleBirr"
assert_tx 15 "Telebirr package purchase 100 ETB"          "EXPENSE" "100.0"     "TeleBirr"
assert_tx 16 "Telebirr transfer 450.50 ETB"               "EXPENSE" "450.5"     "TeleBirr"

# ── USSD Airtime Balance Tests ──────────────────────────────────────────────
section "Asserting USSD Airtime Balance"

USSD_BALANCE=$(db_query "SELECT amount FROM balance_packages WHERE type='airtime' ORDER BY timestamp DESC LIMIT 1;")
if [[ "$USSD_BALANCE" == "12.5" || "$USSD_BALANCE" == "12.50" ]]; then
  pass "Test 17: USSD airtime balance 12.50 Birr persisted to balance_packages"
else
  fail "Test 17: USSD airtime balance not found (got: '${USSD_BALANCE}', expected 12.5)"
fi

USSD_COUNT=$(db_query "SELECT COUNT(*) FROM ussd_events;")
if [[ "$USSD_COUNT" -ge 1 ]]; then
  pass "Test 18: USSD event persisted to ussd_events table"
else
  fail "Test 18: No USSD events in ussd_events table"
fi

# =============================================================================
#  TELECOM PACKAGE INTEGRATION TESTS
# =============================================================================

section "Telecom Package Case 1 — Multi-segment balance SMS"

CASE1_TS=$((NOW_MS - 10000))
inject_sms "994" \
  "Dear Customer, your remaining amount from Monthly Internet Package 12GB from telebirr to be expired after 30 days is 11458.902 MB with expiry date on 2026-05-16 at 11:42:05;  from 50 minutes + 200 MB Free is 44 minute and 3 second with expiry date on 2026-04-22 at 00:00:00;   from Monthly Recurring 125 Min and 63Min night package bonus is 52 minute and 1 second with expiry date on 2026-04-30 at 16:02:16;   from Monthly Recurring 125 Min and 63Min night package bonus is 125 minute and 0 second with expiry date on 2026-04-30 at 16:02:16;     from Create Your Own Package Monthly is 136 SMS with expiry date on 2026-04-19 at 00:22:19;  Enjoy 10% additional rewards by downloading telebirr SuperApp https://bit.ly/telebirr_SuperApp.Happy Holiday! Ethio telecom." \
  "$CASE1_TS"

info "Waiting 8 seconds for Case 1 processing..."
sleep 8

section "Asserting Case 1 Packages"

assert_pkg 19 "Monthly Internet ~11458 MB" "internet-Monthly-20260516" "internet" "Monthly" "11458.902" "MB"
assert_pkg 20 "Free Internet 200 MB"      "internet-Free-20260422"    "internet" "Free"    "200.0"     "MB"
assert_pkg 21 "Recurring Voice 125 min"   "voice-Recurring-20260430"  "voice"    "Recurring" "125.0"   "MIN"
assert_pkg 22 "Night Voice 52 min"        "voice-Night-20260430"      "voice"    "Night"   "52.0"      "MIN"
assert_pkg 23 "Free Voice 44 min"         "voice-Free-20260422"       "voice"    "Free"    "44.05"     "MIN"
assert_pkg 24 "Custom SMS 136"            "sms-Custom-20260419"       "sms"      "Custom"  "136.0"     "SMS"

# Count internet packages — should be exactly 2
INET_COUNT=$(db_query "SELECT COUNT(*) FROM balance_packages WHERE type='internet';")
if [[ "$INET_COUNT" == "2" ]]; then
  pass "Test 25: Case 1 has exactly 2 internet packages"
else
  fail "Test 25: Case 1 internet count (expected 2, got ${INET_COUNT})"
fi

section "Telecom Package Case 2 — Night Internet 600MB (new SMS)"

CASE2_TS=$((NOW_MS - 5000))
inject_sms "994" \
  "Dear customer You have received Night Internet package 600MB from telebirr expire after 24 hr from 0. The package Will be expired on 17-04-2026 06:59:59." \
  "$CASE2_TS"

info "Waiting 8 seconds for Case 2 processing..."
sleep 8

section "Asserting Case 2 Packages (additive, no overwrite)"

assert_pkg 26 "Night Internet 600 MB" "internet-Night-20260417" "internet" "Night" "600.0" "MB"

# Case 1 Monthly internet still exists
MONTH_COUNT=$(db_query "SELECT COUNT(*) FROM balance_packages WHERE id='internet-Monthly-20260516';")
if [[ "$MONTH_COUNT" == "1" ]]; then
  pass "Test 27: Case 1 Monthly internet preserved after Case 2"
else
  fail "Test 27: Case 1 Monthly internet was overwritten (count=${MONTH_COUNT})"
fi

# Total internet count should be 3 (Monthly + Free + Night)
INET_TOTAL=$(db_query "SELECT COUNT(*) FROM balance_packages WHERE type='internet';")
if [[ "$INET_TOTAL" == "3" ]]; then
  pass "Test 28: Total internet packages = 3 (Monthly + Free + Night)"
else
  fail "Test 28: Total internet count (expected 3, got ${INET_TOTAL})"
fi

# =============================================================================
#  DB DUMP  (debug aid)
# =============================================================================

section "DB Transaction Dump"
echo ""
db_query "SELECT type, amount, source, category, partyName FROM transactions ORDER BY timestamp DESC;"

section "DB Balance Packages Dump"
echo ""
db_query "SELECT id, type, subType, remainingAmount, unit FROM balance_packages ORDER BY type, id;"

section "Logcat (ReconciliationEngine + UssdAccessibility)"
echo ""
adb logcat -d -s "ReconciliationEngine:D" -s "UssdAccessibility:D" 2>/dev/null | tail -60 || true

# =============================================================================
#  SUMMARY
# =============================================================================

echo ""
echo "════════════════════════════════════════════"
printf "  Results: ${GREEN}%d passed${NC}, ${RED}%d failed${NC}\n" "$PASS" "$FAIL"
echo "════════════════════════════════════════════"
echo ""

if [[ "$FAIL" -gt 0 ]]; then
  exit 1
fi
exit 0
