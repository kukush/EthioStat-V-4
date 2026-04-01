# EthioBalance

EthioBalance is a privacy-focused, offline-first financial and telecom asset management application designed for the Ethiopian market. It helps users track their mobile balances, data packages, and financial transactions by automatically parsing SMS messages from telecom providers and banks.

## Features

- **Dual-Tracking Architecture**: Separates telecom assets (Airtime, Data, Voice, SMS) from financial transactions (CBE, Telebirr, etc.).
- **Automatic SMS Parsing**: Scans incoming SMS from Ethio Telecom, Telebirr, and major Ethiopian banks to update balances and transaction history.
- **Privacy First**: All data is processed and stored locally on the device. No data is sent to external servers.
- **Multi-SIM Support**: Track balances and transactions across multiple SIM cards.
- **Transaction Management**: Categorize and filter transactions by source, search through history, and export data to CSV.
- **Telecom Management**: Monitor active packages, expiry dates, and get recommendations for new bundles.
- **Multilingual Support**: Available in English, Amharic, and Afaan Oromoo.
- **Native Performance**: Built with Kotlin and Jetpack Compose for a smooth, responsive UI.

## Architecture

The application follows **MVVM (Model-View-ViewModel)** with Clean Architecture principles:

- **Model**: Room entities and DAOs (`data/`) represent the database layer.
- **Repository**: Single-source-of-truth data access (`repository/`) abstracts data sources.
- **ViewModel**: UI state management (`ui/viewmodel/`) with Kotlin Flows.
- **View**: Jetpack Compose screens (`ui/screens/`) and reusable components (`ui/components/`).

### Key Components

- **`ReconciliationEngine`**: Core logic for parsing SMS and inserting transactions.
- **`SmsRepository`**: SMS scanning, USSD dialing, and response handling.
- **`EthioBalanceApp`**: Root composable with navigation and theme.
- **`Translations.kt`**: Centralized localization for English, Amharic, and Afaan Oromoo.

## Usage

1. **Initial Setup**: Grant SMS and Call permissions to allow the app to read messages and perform USSD calls.
2. **Sync Balances**: Tap the "Sync" button on the Telecom screen to trigger USSD *804#. The app will automatically capture the response SMS.
3. **Add Transaction Sources**: In Settings, add the sender IDs of your banks (e.g., `CBE`, `Telebirr`) to start tracking financial transactions.
4. **Manage SIMs**: Register your phone numbers in Settings to link data to specific SIM cards.

## Development

### Prerequisites

- JDK 21
- Android SDK (API 24+)
- Kotlin 1.9.24

### Building the App

```bash
cd android
./gradlew assembleDebug
```

### Running Tests

```bash
cd android
./gradlew test
```

### Deploying to Android Device

#### Prerequisites
- Enable Developer Options and USB Debugging on your Android device
- Connect device via USB and authorize debugging

#### Install APK via ADB

```bash
# Build the APK first
cd android
./gradlew assembleDebug

# Install to connected device
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

#### Alternative: Drag-and-Drop Install

1. Build the APK: `cd android && ./gradlew assembleDebug`
2. Copy the APK to your device: `android/app/build/outputs/apk/debug/app-debug.apk`
3. On your device, enable "Install from unknown sources"
4. Open the APK file and follow the installation prompts

#### Verify Installation

```bash
# Check if app is installed
adb shell pm list packages | grep com.ethiobalance.app

# Launch the app (optional)
adb shell am start -n com.ethiobalance.app/.MainActivity
```

### Code Style

- Kotlin: Follow Android Kotlin Style Guide.
- Compose: Use Material3 design tokens and Manrope font.
- Room: Use Kotlin Coroutines and Flow for reactive data.

## Exporting Data

You can export your transaction history to a CSV file from the Transactions screen. This allows you to perform further analysis in spreadsheet software like Excel or Google Sheets.

## License

MIT
