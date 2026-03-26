#!/bin/bash

# EthioStat Dual-Tracking Validation Script
# This script reads mock_sms.json and sequentially triggers Android background broadcasts
# so you can watch the React UI update magically!

# --- Configuration ---
SCRIPT_DIR="$(dirname "$0")"
JSON_FILE="$SCRIPT_DIR/mock_sms.json"

# --- Dependency Checks ---
if ! command -v jq &> /dev/null; then
    echo "❌ Error: 'jq' is not installed. Please install it (e.g., 'brew install jq')."
    exit 1
fi

if ! command -v adb &> /dev/null; then
    echo "❌ Error: 'adb' is not installed. Please install Android Platform Tools."
    exit 1
fi

if [ ! -f "$JSON_FILE" ]; then
    echo "❌ Error: $JSON_FILE not found."
    exit 1
fi

echo "================================================="
echo "  ETHIOSTAT MOCK SMS BROADCASTER "
echo "================================================="
echo "Loading scenarios from $JSON_FILE..."
echo ""

count=$(jq '. | length' "$JSON_FILE")

for ((i=0; i<$count; i++)); do
    category=$(jq -r ".[$i].category" "$JSON_FILE")
    expectation=$(jq -r ".[$i].expectation" "$JSON_FILE")
    sender=$(jq -r ".[$i].sender" "$JSON_FILE")
    body=$(jq -r ".[$i].body" "$JSON_FILE")

    echo "-------------------------------------------------"
    echo "▶ SCENARIO: $category"
    echo "👀 EXPECT: $expectation"
    echo "-------------------------------------------------"
    echo "Sender: $sender"
    echo "Message: $body"
    echo ""

    # Broadcast SMS via adb to the provider
    adb shell am start-foreground-service \
        -a android.intent.action.MAIN \
        -n "com.ethiobalance.app/.services.SmsForegroundService" \
        --es "sender" "$sender" \
        --es "body" "'$body'"
        
    echo "✓ Broadcast sent. Pausing for 4 seconds so you can see the UI update..."
    sleep 4
    echo ""
done

echo "🎉 All mock scenarios processed!"
