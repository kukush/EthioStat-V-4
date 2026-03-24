#!/bin/bash

# EthioStat Deployment Script
# Automates the build and sync process for Android development

echo "🚀 Starting EthioStat Deployment..."

# 1. Build the React web app
echo "📦 Building web assets..."
npm run build

# 2. Sync with Capacitor
echo "🔄 Syncing Capacitor platforms..."
npx cap sync android

# 3. Open Android Studio (optional)
# npx cap open android

# 4. Run on connected device
echo "📲 Running on connected Android device..."
npx cap run android

echo "✅ Deployment process triggered!"
