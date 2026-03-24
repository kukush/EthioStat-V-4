#!/bin/bash

# EthioStat Device Configuration Script
# Sets up permissions and checks device status via ADB

echo "🛠 Configuring connected Android device..."

# Check if adb is installed
if ! command -v adb &> /dev/null
then
    echo "❌ Error: adb is not installed. Please install Android Platform Tools."
    exit 1
fi

DEVICE_ID=$(adb devices | grep -v "List" | awk '{print $1}' | head -n 1)

if [ -z "$DEVICE_ID" ]; then
    echo "❌ Error: No device connected. Please connect a device with USB debugging enabled."
    exit 1
fi

echo "✅ Found device: $DEVICE_ID"

PACKAGE_NAME="com.ethiobalance.app"

echo "🔐 Granting necessary permissions..."
adb -s "$DEVICE_ID" shell pm grant "$PACKAGE_NAME" android.permission.READ_SMS
adb -s "$DEVICE_ID" shell pm grant "$PACKAGE_NAME" android.permission.RECEIVE_SMS
adb -s "$DEVICE_ID" shell pm grant "$PACKAGE_NAME" android.permission.CALL_PHONE
adb -s "$DEVICE_ID" shell pm grant "$PACKAGE_NAME" android.permission.READ_PHONE_STATE

echo "🚀 Done! Permissions granted for $PACKAGE_NAME"
