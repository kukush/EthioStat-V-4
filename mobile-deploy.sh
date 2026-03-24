#!/bin/bash

# 1. Build the web app
npm run build

# 2. Initialize Capacitor (only run once)
# npx cap init EthioBalance com.ethiobalance.app --web-dir dist

# 3. Add Android/iOS platforms
# npx cap add android
# npx cap add ios

# 4. Sync web code to native platforms
npx cap sync

# 5. Open in Android Studio or Xcode
# npx cap open android
# npx cap open ios
