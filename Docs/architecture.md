# EthioStat Architecture

## Overview
EthioStat is a telecom-grade, dual-tracking hybrid mobile application designed to function as an offline-first billing engine. Built with React and Capacitor, its business logic operates primarily in Android native memory, ensuring deterministic state processing.

The app tracks a user's telecom balances (Assets: internet, voice, SMS, and bonuses) separately from its financial history (Transactions: income and expenses).

## System Architecture

### 1. Presentation Layer (UI)
React functional components are **strictly read-only** from a business state perspective.
- **Screens** (`src/screens/`):
  - `HomeScreen`: Displays dynamically computed net balances and current remaining telecom packages.
  - `TelecomScreen`: Reads active `BalancePackageEntity` stats from the database.
  - `TransactionScreen`: Reads `TransactionEntity` history.
  - `SettingsScreen`: Manages strictly UI configuration (theme, tab, filters).

### 2. State Management Layer (`useNativeData`)
React strictly prohibits telecom packages and transactions from residing in a Javascript `store`.
- React state relies on `useNativeData.ts`, which polls or observes Capacitor Plugins to pull directly from the native SQLite standard.

### 3. Native Integration Layer (Capacitor Bridge)
- **SmsMonitorPlugin (Kotlin)**: A bridge that executes `getBalances()` and `getTransactions()` against the Room Database in standard API format.

### 4. Native Android Layer (The Single Source of Truth)
All computing executes offline in Kotlin.
- **Room Database**: Consists of explicit entities: `BalancePackage` (STATE) and `Transaction` (EVENTS).
- **SmsForegroundService**: Background service capturing all incoming signals (SMS/USSD).
- **SmsParser**: Smart Regex engine identifying English, Amharic, and Afaan Oromo signals securely.
- **ReconciliationEngine**: The heart of the system. Observes signals and deterministically segregates an identical SMS message into a distinct Asset increase (Package Added) and a Financial Decrease (Package Paid For).

## Data Flow
1. **Event Reception**: SMS received natively by `SmsForegroundService`.
2. **Audit Tracking**: Raw string logged directly to `SmsLogEntity` for replayability.
3. **Parse & Confidence Check**: `SmsParser` maps strings to `SmsScenario` enums based on confidence rules (> 0.70 threshold).
4. **Reconciliation**: Engine segregates `SELF_PURCHASE` from `GIFT_SENT` to prevent incorrect Asset increases during gift purchases.
5. **UI Rendering**: Component relies on `SmsMonitorPlugin.getBalances()` and receives structured `packages` alongside the mathematically computed `NetBalance`.
