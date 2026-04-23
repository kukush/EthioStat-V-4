# EthioStat Deployment Guide

This guide provides instructions for building, deploying, and testing the EthioStat native Android application.

---

## Prerequisites

- **Physical Android device** with USB debugging enabled (Settings > Developer Options)
- **Android SDK** (API 36) and platform tools installed
- **JDK 21** (Azul Zulu or OpenJDK recommended)
- **Gradle 8.13** (bundled via `gradlew` wrapper — no separate install needed)
- **ADB** accessible from the command line (`adb devices` should list your device)

---

## Build Process

### 1. Clone and Open

```bash
git clone https://github.com/<org>/EthioStat-V-4.git
cd EthioStat-V-4/android
```

Or open the `android/` directory directly in Android Studio.

### 2. Verify Configuration

Build-time constants are defined in `gradle.properties`:

```properties
ethiobalance.ussd.balance_check=*804#
ethiobalance.ussd.recharge_self=*805*
ethiobalance.ussd.recharge_other=*805*
ethiobalance.ussd.transfer_airtime=*806*
ethiobalance.ussd.gift_package=*999#
ethiobalance.phone_app_package=com.android.phone
ethiobalance.default_sources=CBE,TELEBIRR
```

These are injected into `BuildConfig` and accessed via `AppConstants`.

### 3. Build the APK

```bash
# Debug build
./gradlew assembleDebug

# Release build (requires signing config)
./gradlew assembleRelease
```

The APK is output to `app/build/outputs/apk/debug/app-debug.apk`.

### 4. Deploy to Device

```bash
# Build and install in one step
./gradlew installDebug

# Or install a pre-built APK manually
adb install app/build/outputs/apk/debug/app-debug.apk
```

### 5. Open in Android Studio (optional)

```bash
# From the project root
open -a "Android Studio" android/
```

---

## Device Configuration

### Required Permissions

The app requests these permissions at runtime. Grant them when prompted:

| Permission | Purpose |
|---|---|
| `READ_SMS` | Scan SMS inbox for financial transactions |
| `RECEIVE_SMS` | Process incoming SMS in real time |
| `POST_NOTIFICATIONS` | Show foreground service and sync notifications |

### ADB Permission Grant (development shortcut)

```bash
adb shell pm grant com.ethiobalance.app android.permission.READ_SMS
adb shell pm grant com.ethiobalance.app android.permission.RECEIVE_SMS
adb shell pm grant com.ethiobalance.app android.permission.POST_NOTIFICATIONS
```

> **Note**: The app does **not** require `CALL_PHONE`, `READ_PHONE_STATE`, or any Accessibility Service. USSD balance checks use `ACTION_DIAL` which opens the system dialer without special permissions.

---

## Testing

### 1. Unit Tests (JVM)

```bash
./gradlew testDebugUnitTest
```

Covers SMS parsing, confidence scoring, party name extraction, and transaction categorisation.

### 2. Integration Tests (device required)

```bash
chmod +x scripts/test-workflow.sh
./scripts/test-workflow.sh
```

This script:
- Injects mock SMS via `adb shell am broadcast`
- Verifies transactions in the Room database via `adb shell content query`
- Checks party name extraction, amount parsing, and source resolution

### 3. Background SMS Monitoring

1. Deploy the app and grant SMS permissions
2. Send a test SMS to the device from another phone (e.g., from sender "127")
3. Verify the "EthioStat" foreground service notification appears
4. Check that the transaction appears in the app's transaction list

### 4. Historical SMS Scanning (90-day)

1. Open the app and navigate to **Settings**
2. Add a new transaction source (e.g., "CBE")
3. The app initiates a 90-day historical SMS scan for that source
4. Verify parsed transactions appear on the Dashboard with a dedicated card

### 5. USSD Balance Sync

1. Navigate to the **Telecom** tab
2. Tap **Sync Balance**
3. The system dialer opens with `*804#` pre-filled — user presses Call
4. After the USSD popup, Ethio Telecom sends a 994 SMS with balance data
5. The app processes the 994 SMS and updates telecom assets automatically
6. A heads-up notification appears: "Telecom data updated — tap to return"

### 6. Room Database Persistence

1. Process several SMS transactions
2. Force-close the app (`adb shell am force-stop com.ethiobalance.app`)
3. Reopen the app
4. Verify all data is preserved and loaded correctly

---

## Multilingual Support Testing

The app supports three languages with smart parsing:

| Language | Detection | Calendar |
|---|---|---|
| **English** | Standard ASCII | Gregorian |
| **Amharic** | Ethiopic Unicode (`\u1200-\u137F`) | Ethiopian (EthiopicCalendar) |
| **Afaan Oromo** | Latin script keywords | Ethiopian (EthiopicCalendar) |

### Testing Language Parsing
1. Send SMS messages in different languages
2. Verify correct language detection in parsed results
3. Confirm accurate transaction parsing for each language
4. Switch app language in Settings and verify UI and date formatting

---

## Troubleshooting

### Build Issues

**Issue**: `BUILD FAILED` with SDK version errors
**Solution**: Ensure `compileSdk = 36` and `minSdk = 24` in `app/build.gradle.kts`. Run `./gradlew --refresh-dependencies`.

**Issue**: `No connected devices!` on `installDebug`
**Solution**: Verify `adb devices` lists your device. Enable USB debugging and accept the RSA fingerprint prompt.

**Issue**: Gradle daemon out of memory
**Solution**: Add `org.gradle.jvmargs=-Xmx4096m` to `gradle.properties`.

### Runtime Issues

**Issue**: SMS monitoring not working
**Solution**: Verify SMS permissions are granted (`adb shell dumpsys package com.ethiobalance.app | grep permission`). Check that the foreground service notification is visible.

**Issue**: Historical scan finds no messages
**Solution**: Confirm the sender ID matches what's in the SMS inbox. Use `adb shell content query --uri content://sms/inbox --projection address,body --where "address='127'"` to verify.

**Issue**: Transactions not appearing after SMS
**Solution**: Check `adb logcat -s ReconciliationEngine SmsForegroundService SmsReceiver` for processing logs and error messages.

### Performance

- **Background Processing**: SMS monitoring runs as a `dataSync` foreground service
- **Database**: All data stored in Room (`ethio_balance_db`) — fully offline
- **Dedup**: Hash-based deduplication in `SmsLogDao` prevents double-counting on rescan

---

## Security Considerations

- **100% offline** — all SMS processing happens on-device, no data sent externally
- **No internet required** for core functionality (SMS parsing, transaction tracking)
- **Minimal permissions** — only `READ_SMS`, `RECEIVE_SMS`, `POST_NOTIFICATIONS`
- **No `CALL_PHONE`** — USSD uses `ACTION_DIAL` (user confirms the call manually)
- **No Accessibility Service** — no system-level interception of any kind
- **Room database** provides structured local persistence

---

## Next Steps

After successful deployment:

1. **Smoke Test**: Navigate all tabs — Home, Transactions, Telecom, Settings
2. **Multi-Source Test**: Add CBE, Awash, Telebirr sources and verify each parses correctly
3. **Battery Test**: Monitor battery usage over 24 hours with background SMS monitoring active
4. **Feature Validation**: Run `./scripts/test-workflow.sh` for automated verification
