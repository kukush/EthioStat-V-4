#!/bin/bash

# Test script to verify Gradle cache cleanup fixes the JAR corruption issue
cd "$(dirname "$0")/.." || exit

echo "🧪 Testing Gradle cache cleanup fix..."

# Step 1: Clean everything
echo "🧹 Cleaning all Gradle caches..."
pkill -f gradle || true
pkill -f java.*gradle || true
sleep 2

./android/gradlew --stop || true
sleep 1

rm -rf ~/.gradle/caches/
rm -rf ~/.gradle/daemon/
rm -rf android/.gradle
rm -rf android/build
rm -rf android/app/build
rm -rf android/capacitor-cordova-android-plugins/build

echo "✅ Cache cleanup complete"

# Step 2: Test basic Gradle functionality
echo "🔨 Testing Gradle build..."
cd android

if ./gradlew clean --no-daemon --refresh-dependencies; then
    echo "✅ Gradle clean successful"
    
    if ./gradlew assembleDebug --no-daemon --refresh-dependencies; then
        echo "✅ Android build successful - JAR corruption issue fixed!"
        cd ..
        exit 0
    else
        echo "❌ Android build failed"
        cd ..
        exit 1
    fi
else
    echo "❌ Gradle clean failed"
    cd ..
    exit 1
fi
