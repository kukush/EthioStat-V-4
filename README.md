# EthioStat

**EthioStat** is a privacy-focused, offline-first financial and telecom asset management mobile application designed for the Ethiopian market. It helps users track their mobile telecom balances, data packages, and multi-bank financial transactions by automatically parsing SMS messages from telecom providers and 25+ Ethiopian banks.

---

## Project Structure & Architecture

This repository contains two coordinated layers:

1. **Native Android Application (`/android`)** *(Primary Mobile Target)*:
   - Built with **100% Kotlin & Jetpack Compose** (Material3 design system).
   - Local offline persistence powered by **Room Database**, Coroutines, and Kotlin StateFlows.
   - **Telephony & SMS Services**: Background SMS listener broadcast receiver, foreground synchronization service, and USSD automation (*804#, *805#, *806#, *999#).
   - Dependency Injection with **Hilt/Dagger**.
   - Clean Architecture with MVVM pattern.

2. **Web Simulator & Preview Companion (`/src`)** *(Interactive Sandbox)*:
   - High-fidelity React & Tailwind simulator running in Google AI Studio container preview.
   - Enables in-browser testing of SMS regex parsing, balance reconciliation, dual-SIM toggles, and multi-lingual UI without requiring a physical Android device or emulator.
   - Powered by centralized constants and environment configurations.

---

## Centralized Configuration & Environment Variables

All app identifiers, branding names, USSD dial codes, and default sources are defined in central configuration files rather than hardcoded:

### Android Configuration (`android/gradle.properties` & `AppConstants.kt`)
- `ethiobalance.app_name` (e.g. `EthioStat`) -> Injected into `BuildConfig.APP_NAME` and referenced via `AppConstants.APP_NAME`.
- `ethiobalance.app_version` (e.g. `1.1`) -> Injected into `BuildConfig.APP_VERSION_NAME`.
- `ethiobalance.ussd.balance_check` (`*804#`)
- `ethiobalance.ussd.recharge_self` (`*805*`)
- `ethiobalance.ussd.transfer_airtime` (`*806*`)
- `ethiobalance.ussd.gift_package` (`*999#`)
- `ethiobalance.default_sources` (`CBE,TELEBIRR,APOLLO,CBEBIRR`)

### Web Simulator Configuration (`.env.example` & `src/constants/app.ts`)
- `VITE_APP_NAME` -> Configurable via environment (defaults to `EthioStat`).
- `VITE_APP_VERSION` -> Application version tag.
- `VITE_DEFAULT_LANGUAGE` -> Default UI locale (`en`, `am`, `om`).

---

## Features

- **Dual-Tracking Architecture**: Separates telecom assets (Airtime, Data, Voice, SMS) from financial transactions (CBE, Telebirr, Awash, etc.).
- **Automatic SMS Parsing**: Scans incoming SMS from Ethio Telecom, Telebirr, and major Ethiopian banks to update balances and transaction history.
- **Privacy First**: All data is processed and stored locally on the device. No data is sent to external servers.
- **Multi-SIM Support**: Track balances and transactions across multiple SIM cards.
- **Transaction Management**: Categorize and filter transactions by source, search through history, and export data to CSV.
- **Telecom Management**: Monitor active packages, expiry dates, and get recommendations for new bundles.
- **Multilingual Support**: Available in English, Amharic (አማርኛ), and Afaan Oromoo.

---

## Building the Android Application

### Prerequisites
- JDK 21
- Android SDK (API 24+)
- Kotlin 1.9.24

### Build Commands

```bash
# 1. Clean and build Debug APK
cd android
./gradlew clean assembleDebug

# Output APK Location:
# android/app/build/outputs/apk/debug/EthioStat-debug.apk

# 2. Build Release APK
./gradlew assembleRelease
```

### Running Tests

```bash
# Run Android Unit Tests
cd android
./gradlew test

# Run JaCoCo Coverage Report
./gradlew :app:testDebugUnitTest
```

### Web Simulator Tests

```bash
# Run simulator unit & integration test suite (49 tests)
npm test
```

---

## Deploying to Android Device via ADB

```bash
# Clear previous package state (optional)
adb uninstall com.ethiobalance.app

# Install fresh APK with runtime permissions
adb install -r -g android/app/build/outputs/apk/debug/EthioStat-debug.apk

# Launch EthioStat
adb shell am start -n com.ethiobalance.app/.MainActivity
```

---

## License

MIT