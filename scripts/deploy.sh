#!/bin/bash

# Move to project root (one level up from scripts/)
cd "$(dirname "$0")/.." || exit

echo "🚀 Starting EthioStat Deployment..."

# 0. Check Java version (Capacitor 6 requires Java 17)
JAVA_VER=$(java -version 2>&1 | head -n 1 | awk -F '"' '{print $2}' | cut -d'.' -f1)
if [ "$JAVA_VER" != "17" ]; then
  echo "⚠️ Warning: Detected Java version $JAVA_VER, but Capacitor 6 requires Java 17."
  if [ "$(uname)" == "Darwin" ]; then
    export JAVA_HOME=$(/usr/libexec/java_home -v 17 2>/dev/null)
    if [ -n "$JAVA_HOME" ]; then
      echo "✅ Set JAVA_HOME to Java 17."
    else
      echo "❌ Error: Java 17 is not installed."
      exit 1
    fi
  fi
fi

# 1. Download bank logos (with SVG fallbacks)
echo "🖼️ Downloading bank logos..."
npx tsx scripts/download-logos.ts

# 2. Build the React web app
echo "📦 Building web assets..."
npm run build

# 3. Comprehensive Gradle Cache Cleanup
echo "🧹 Cleaning Gradle caches and build artifacts..."

# Force kill any stuck Gradle/Java processes
echo "🛑 Terminating lingering Gradle processes..."
pkill -f gradle || true
pkill -f java.*gradle || true
sleep 3

# Stop Gradle daemon explicitly
echo "🛑 Stopping Gradle daemon..."
./android/gradlew --stop || true
sleep 2

# Nuclear cleanup of all Gradle caches
echo "💥 Nuclear Gradle cache cleanup..."
rm -rf ~/.gradle/caches/
rm -rf ~/.gradle/daemon/
rm -rf ~/.gradle/wrapper/

# Clean Android project build artifacts
echo "🧹 Cleaning Android build artifacts..."
rm -rf android/.gradle
rm -rf android/build
rm -rf android/app/build
rm -rf android/capacitor-cordova-android-plugins/build
rm -rf android/capacitor-cordova-android-plugins/.gradle

# Clean Capacitor generated files
echo "🧹 Cleaning Capacitor artifacts..."
rm -rf android/app/src/main/assets/public
rm -rf android/app/src/main/assets/capacitor.config.json

# 4. Fresh Capacitor sync
echo "🔄 Syncing Capacitor platforms..."
npx cap sync android

# 5. Gradle build with comprehensive error handling
echo "🔨 Building Android app with fresh Gradle setup..."
cd android

# First attempt: Clean build with no daemon
echo "🧹 Cleaning Android build..."
if ! ./gradlew clean --no-daemon --refresh-dependencies; then
    echo "❌ Clean failed, trying with --rerun-tasks..."
    ./gradlew clean --no-daemon --rerun-tasks --refresh-dependencies || {
        echo "❌ Clean still failed. Trying one more cache cleanup..."
        cd ..
        rm -rf ~/.gradle/caches/
        cd android
        ./gradlew clean --no-daemon --refresh-dependencies
    }
fi

# Second attempt: Build debug APK
echo "🔨 Building debug APK..."
if ! ./gradlew assembleDebug --no-daemon --refresh-dependencies; then
    echo "❌ Build failed, trying with --rerun-tasks..."
    ./gradlew assembleDebug --no-daemon --rerun-tasks --refresh-dependencies || {
        echo "❌ Build failed again. Manual intervention may be required."
        echo "💡 Try: rm -rf ~/.gradle && ./gradlew assembleDebug --no-daemon"
        cd ..
        exit 1
    }
fi

cd ..

# 6. Deploy to connected device
echo "📲 Running on connected Android device..."
npx cap run android --no-build

echo "✅ Deployment process triggered!"
