#!/usr/bin/env bash
# =============================================================================
# EthioStat — Web Parser Workflow Test Script
#
# Usage:  chmod +x scripts/test-web-workflow.sh && ./scripts/test-web-workflow.sh
#
# Prerequisites:
#   - Node.js 18+ in PATH
#   - Run from the repo root: ./scripts/test-web-workflow.sh
#
# What this script does:
#   1. Runs the TypeScript SMS parser unit tests (src/data/smsParser.test.ts)
#   2. Runs the TypeScript type-checker (tsc --noEmit)
#   3. Reports a combined pass/fail banner
#
# No Android device or build required — pure web/parser testing.
# For device testing see: ./scripts/test-workflow.sh
# =============================================================================
#
# Android Emulator Quick-Start (test without physical device)
# ─────────────────────────────────────────────────────────────
# Tier 1 — Parser only (instant, no build):
#   ./scripts/test-web-workflow.sh
#
# Tier 2 — Emulator (full native SMS pipeline):
#   1. Android Studio → Device Manager → Create Pixel 7 API 34 → Start
#   2. npx cap build android && cd android && ./gradlew assembleDebug
#   3. adb -e install app/build/outputs/apk/debug/app-debug.apk
#   4. adb -e shell pm grant com.ethiobalance.app android.permission.READ_SMS
#   5. adb -e shell pm grant com.ethiobalance.app android.permission.RECEIVE_SMS
#   6. ./scripts/test-workflow.sh          # works identically on emulator
#
# Inject live SMS via emulator console (triggers SmsReceiver):
#   cat ~/.emulator_console_auth_token
#   telnet localhost 5554
#   auth <token>
#   sms send 127 "Your telebirr account balance is 500.00 ETB."
#
# Watch logs:
#   adb -e logcat -s SmsForegroundService:D SmsParser:D SmsReceiver:D ReconciliationEngine:D
#
# Tier 3 — Browser UI (no native features):
#   npm run dev        # http://localhost:5173
# =============================================================================

set -euo pipefail

GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m'

PASS=0
FAIL=0

pass() { echo -e "${GREEN}✅ PASS${NC}: $1"; PASS=$((PASS+1)); }
fail() { echo -e "${RED}❌ FAIL${NC}: $1"; FAIL=$((FAIL+1)); }
info() { echo -e "${YELLOW}ℹ️  ${NC}$1"; }
step() { echo -e "\n${CYAN}── $1 ${NC}──────────────────────────────────────────"; }

echo ""
echo "════════════════════════════════════════════"
echo "  EthioStat Web Parser Workflow Test"
echo "════════════════════════════════════════════"
echo ""

# ─── Preconditions ───────────────────────────────────────────────────────────

if ! command -v node &>/dev/null; then
  echo "❌ node not found in PATH. Install Node.js 18+."
  exit 1
fi

if ! command -v npm &>/dev/null; then
  echo "❌ npm not found in PATH."
  exit 1
fi

if [[ ! -f "package.json" ]]; then
  echo "❌ Run this script from the repo root (EthioStat-V-4/)."
  exit 1
fi

info "Node $(node --version) / npm $(npm --version)"

# ─── Step 1: SMS Parser Unit Tests ──────────────────────────────────────────

step "Step 1: SMS Parser Unit Tests"

PARSER_OUTPUT=$(npm test 2>&1) || true
PARSER_PASSED=$(echo "$PARSER_OUTPUT" | grep -c "✅ PASS" || true)
PARSER_FAILED=$(echo "$PARSER_OUTPUT" | grep -c "❌ FAIL" || true)

# Print individual test lines
echo "$PARSER_OUTPUT" | grep -E "(✅|❌)" || true

if [[ "$PARSER_FAILED" -eq 0 && "$PARSER_PASSED" -gt 0 ]]; then
  pass "SMS parser: $PARSER_PASSED tests passed, 0 failed"
else
  fail "SMS parser: $PARSER_PASSED passed, $PARSER_FAILED failed"
  echo ""
  echo "── Failing test details ──"
  echo "$PARSER_OUTPUT" | grep -A3 "❌ FAIL" || true
fi

# ─── Step 2: TypeScript Type Check ──────────────────────────────────────────

step "Step 2: TypeScript Type Check (tsc --noEmit)"

if npm run lint 2>&1 | grep -q "error TS"; then
  LINT_ERRORS=$(npm run lint 2>&1 | grep "error TS" | head -5)
  fail "TypeScript errors found:\n$LINT_ERRORS"
else
  pass "TypeScript: 0 type errors"
fi

# ─── Step 3: Sanity-check key parse scenarios inline ─────────────────────────

step "Step 3: Key Scenario Sanity Checks"

# 3a. Verify Telebirr complex multi-segment message returns 3 packages
COMPLEX_MSG='Dear Customer, your remaining amount  from Monthly Internet Package 12GB from telebirr to be expired after 30 days is 11927.084 MB with expiry date on 2026-04-30 at 02:41:08;  from Monthly Recurring 125 Min and 63Min night package bonus is 63 minute and 0 second with expiry date on 2026-04-30 at 16:02:16;   from Monthly Recurring 125 Min and 63Min night package bonus is 125 minute and 0 second with expiry date on 2026-04-30 at 16:02:16;    from Monthly Internet Package 12GB from telebirr to be expired after 30 days is 12288.000 MB with expiry date on 2026-04-30 at 10:16:09;  from Monthly voice 150 Min from telebirr to be expired after 30 days and 76 Min night package bonus valid for 30 days is 65 minute and 14 second with expiry date on 2026-04-10 at 11:08:07;     from Create Your Own Package Monthly is 149 SMS with expiry date on 2026-04-19 at 00:22:19;  Enjoy 10% additional rewards by downloading telebirr SuperApp.Ethio telecom'

COMPLEX_RESULT=$(npx tsx -e "
import { parseEthioSMS } from './src/data/smsParser.js';
const r = parseEthioSMS(process.argv[1], '127');
process.stdout.write(String(r.packages.length));
" -- "$COMPLEX_MSG" 2>/dev/null || echo "0")

PKG_COUNT="${COMPLEX_RESULT:-0}"

if [[ "$PKG_COUNT" -ge 3 ]]; then
  pass "Complex Telebirr SMS parses $PKG_COUNT packages (≥ 3 expected)"
else
  fail "Complex Telebirr SMS returned only $PKG_COUNT packages (expected ≥ 3)"
fi

# 3b. Verify Amharic balance is parsed
AMHARIC_RESULT=$(npx tsx -e "
import { parseEthioSMS } from './src/data/smsParser.js';
const r = parseEthioSMS('\u1240\u122a \u1212\u1233\u1261 450.00 \u1265\u122d', '804');
process.stdout.write(String(r.balance ?? 0));
" 2>/dev/null || echo "0")

if [[ "$AMHARIC_RESULT" == "450" ]]; then
  pass "Amharic balance (ቀሪ ሒሳቡ) correctly parsed as 450 ETB"
else
  fail "Amharic balance parse failed (got: $AMHARIC_RESULT, expected: 450)"
fi

# ─── Summary ─────────────────────────────────────────────────────────────────

echo ""
echo "════════════════════════════════════════════"
printf "  Results: ${GREEN}%d passed${NC}, ${RED}%d failed${NC}\n" "$PASS" "$FAIL"
echo "════════════════════════════════════════════"
echo ""

if [[ "$FAIL" -gt 0 ]]; then
  echo "Some checks failed. To debug:"
  echo "  npm test                    # full parser test output"
  echo "  npm run lint                # TypeScript errors"
  exit 1
fi

echo -e "${GREEN}All web parser checks passed.${NC}"
echo ""
echo "Next: test on Android emulator or device:"
echo "  ./scripts/test-workflow.sh  # requires adb + device/emulator"
exit 0
