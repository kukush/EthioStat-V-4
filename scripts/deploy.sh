#!/bin/bash

# Move to project root (one level up from scripts/)
cd "$(dirname "$0")/.." || exit

# EthioStat Deployment Script
# Automates the build and sync process for Android development

echo "🚀 Starting EthioStat Deployment..."

# 0. Check Java version (Capacitor 6 requires Java 17)
JAVA_VER=$(java -version 2>&1 | head -n 1 | awk -F '"' '{print $2}' | cut -d'.' -f1)
if [ "$JAVA_VER" != "17" ]; then
  echo "⚠️ Warning: Detected Java version $JAVA_VER, but Capacitor 6 requires Java 17."
  # Trying to set Java 17 for this session if it's installed (on macOS)
  if [ "$(uname)" == "Darwin" ]; then
    export JAVA_HOME=$(/usr/libexec/java_home -v 17 2>/dev/null)
    if [ -n "$JAVA_HOME" ]; then
      echo "✅ Set JAVA_HOME to Java 17."
    else
      echo "❌ Error: Java 17 is not installed. Please install it and try again."
      exit 1
    fi
  fi
fi

# 1. Build the React web app
echo "📦 Building web assets..."
npm run build

# 2. Sync with Capacitor
echo "🔄 Syncing Capacitor platforms..."

# Ensure we are in the project root by checking for capacitor.config.json
if [ ! -f "capacitor.config.json" ]; then
    echo "❌ Error: capacitor.config.json not found."
    exit 1
fi

# If android folder exists but isn't recognized, we ensure it's added
if [ ! -d "android" ]; then
    echo "➕ Adding android platform..."
    npx cap add android
fi

# Clean potentially corrupted build artifacts
echo "🧹 Cleaning Capacitor Android build artifacts..."
rm -rf node_modules/@capacitor/android/capacitor/build
rm -rf android/app/build
rm -rf android/.gradle

echo "🔄 Syncing with Android..."
npx cap sync android

# 3. Run on connected device
echo "📲 Running on connected Android device..."
npx cap run android

echo "✅ Deployment process triggered!"
