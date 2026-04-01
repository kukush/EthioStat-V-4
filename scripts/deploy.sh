#!/bin/bash

# Move to project root (one level up from scripts/)
cd "$(dirname "$0")/.." || exit

echo "🚀 Starting EthioStat Deployment..."

# --- Environment Setup (NVM & Node.js) ---
echo "🔍 Checking environment..."

# 1. Detect and initialize NVM if npx is missing
if ! command -v npx &> /dev/null; then
  echo "⚠️ npx not found in PATH. Checking for NVM..."
  
  # Potential NVM installation paths
  NVM_PATHS=(
    "$HOME/.nvm/nvm.sh"
    "/usr/local/opt/nvm/nvm.sh"
    "/opt/homebrew/opt/nvm/nvm.sh"
  )

  for nvm_path in "${NVM_PATHS[@]}"; do
    if [ -s "$nvm_path" ]; then
      export NVM_DIR="$(dirname "$nvm_path")"
      \. "$nvm_path"
      echo "✅ Sourced NVM from $nvm_path"
      
      # Try to use a version (latest installed or from .nvmrc)
      if [ -f ".nvmrc" ]; then
        nvm use || nvm install
      else
        nvm use node &> /dev/null || nvm use default &> /dev/null || echo "⚠️ Could not automatically select a Node version with NVM."
      fi
      break
    fi
  done
fi

# 2. Final tool validation
for tool in node npm npx; do
  if ! command -v $tool &> /dev/null; then
    echo "❌ Error: Required tool '$tool' is not installed or not in PATH."
    echo "💡 Please install Node.js (which includes npm/npx) to proceed."
    exit 1
  fi
done

# 3. Check for node_modules (ensure dependencies are installed)
if [ ! -d "node_modules" ]; then
  echo "📦 node_modules not found. Installing dependencies..."
  npm install || { echo "❌ npm install failed. Please check your internet connection and package.json."; exit 1; }
else
  echo "✅ node_modules found."
fi

echo "✅ Node environment ready (Node $(node -v))"

# 4. Check Java version (Capacitor 6 requires Java 17)
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

# 2. Build the React web app
echo "📦 Building web assets..."
npm run build

# 3. Comprehensive Gradle Cache Cleanup (REDUCED)
echo "🧹 Cleaning Gradle build artifacts (lite)..."

# Force kill any stuck Gradle/Java processes
echo "🛑 Terminating lingering Gradle processes..."
pkill -f gradle || true
pkill -f java.*gradle || true
sleep 1

# Stop Gradle daemon explicitly
echo "🛑 Stopping Gradle daemon..."
./android/gradlew --stop || true
sleep 1

# [REDUCED] Nuclear cleanup of all Gradle caches is disabled by default to avoid re-downloads
# echo "💥 Nuclear Gradle cache cleanup..."
# rm -rf ~/.gradle/caches/
# rm -rf ~/.gradle/daemon/
# rm -rf ~/.gradle/wrapper/

# Clean Android project build artifacts
echo "🧹 Cleaning Android build artifacts..."
# rm -rf android/.gradle
rm -rf android/build
rm -rf android/app/build
# rm -rf android/capacitor-cordova-android-plugins/build
# rm -rf android/capacitor-cordova-android-plugins/.gradle


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

# First attempt: Clean build (Normal)
echo "🧹 Cleaning Android build..."
if ! ./gradlew clean --no-daemon; then
    echo "❌ Clean failed, trying with --refresh-dependencies..."
    ./gradlew clean --no-daemon --refresh-dependencies || {
        echo "❌ Clean still failed. Manual intervention may be required."
        # cd ..
        # rm -rf ~/.gradle/caches/
        # cd android
        # ./gradlew clean --no-daemon --refresh-dependencies
    }
fi


# Second attempt: Build debug APK
echo "🔨 Building debug APK..."
if ! ./gradlew assembleDebug --no-daemon; then
    echo "❌ Build failed, trying with --refresh-dependencies..."
    ./gradlew assembleDebug --no-daemon --refresh-dependencies || {
        echo "❌ Build failed again. Manual intervention may be required."
        echo "💡 Try: rm -rf ~/.gradle && ./gradlew assembleDebug --no-daemon"
        cd ..
        exit 1
    }
fi


cd ..

# 6. Deploy to connected device
echo "📲 Running on connected Android device..."
npx cap run android

echo "✅ Deployment process triggered!"
