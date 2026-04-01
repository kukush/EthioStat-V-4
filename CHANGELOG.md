# Changelog

All notable changes to this project will be documented in this file.

## [2.0.0] - 2026-04-01

### 🚀 Major Release: Native Kotlin/Jetpack Compose Rewrite

- **Full Native Rewrite**: Migrated from React/Capacitor to native Android with Kotlin and Jetpack Compose.
- **Performance Gains**: Eliminated web bridge overhead for smoother UI and faster SMS processing.
- **Pixel-Perfect UI**: Replicated the exact React UI using Material3 and custom components.
- **MVVM Architecture**: Introduced clean architecture with Repository, ViewModel, and Compose layers.
- **Offline-First**: All data remains on-device; no external dependencies or servers.

### ✨ New Features
- **USSD Sync**: Native USSD *804# dialing and response SMS capture for balance sync.
- **CSV Export**: Direct file sharing from Android without web dependencies.
- **Dynamic Themes**: Four themes (Light, Dark, Midnight, Forest) with Material3.
- **Google Fonts**: Integrated Manrope font via Google Fonts provider.
- **SmsLog Deduplication**: Added `bodyHash` to prevent duplicate transaction processing.
- **Sender Normalization**: Unified SMS sender formats (+251, 0, bare) to avoid duplicates.
- **BuildConfig Injection**: Moved hardcoded USSD codes to `gradle.properties` via BuildConfig.

### 🛠️ Technical Changes
- **Removed Capacitor**: Deleted all web assets, Cordova plugins, and Capacitor config.
- **Room Database**: Bumped to version 4 for new `SmsLogEntity.bodyHash` column.
- **Repositories**: Created `SmsRepository`, `SimRepository`, `TransactionRepository`, `BalanceRepository`, `SettingsRepository`.
- **ViewModels**: Implemented reactive UI state with Kotlin Flows.
- **Compose Components**: Built reusable components (BottomNavBar, PackageCard, TransactionItem, SummaryCard).
- **Navigation**: Simple tab-based navigation without external routing.
- **Permissions**: Updated AndroidManifest for native permissions only.

### 🧹 Cleanup
- **Deleted Web Files**: Removed `src/`, `public/`, `scripts/`, and all React configs.
- **Removed Logo Download**: Eliminated `.env` logo URLs and download scripts.
- **Updated CI/CD**: Replaced Node.js steps with Android-only pipeline using JDK 21.
- **Simplified .gitignore**: Removed web and logo download ignores; kept Android build artifacts.

### 📦 Dependencies
- **Compose BOM**: `2024.12.01`
- **Kotlin**: `1.9.24`
- **Room**: `2.6.1`
- **DataStore**: `1.1.1`
- **Navigation Compose**: `2.8.4`

### 🐛 Fixes
- **Duplicate Transactions**: Fixed via sender normalization and SMS deduplication.
- **USSD Sync Flow**: Native implementation replaces web-based approach.
- **Build Errors**: Fixed Kotlin version mismatch and missing color resources.

### 📱 UI/UX
- **Material3**: Adopted Material3 design tokens and components.
- **Responsive Layouts**: All screens adapt to device size with proper scrolling.
- **Bottom Navigation**: Persistent tab navigation with active indicators.
- **Modals & Sheets**: Bottom sheet modals for recharge, transfer, and settings.

### 🌐 Localization
- **Translations.kt**: Centralized translation system for English, Amharic, Afaan Oromoo.
- **Dynamic Language**: Runtime language switching without app restart.
- **Localized Themes**: Theme names and UI strings adapt to selected language.

---

## [1.0.0] - 2025-03-01

### 🎉 Initial Release (React/Capacitor)

- Dual-tracking architecture for telecom and financial assets.
- SMS parsing for Ethio Telecom and major banks.
- Offline-first local storage.
- Multi-SIM support.
- CSV export.
- Multilingual support.

---

*Note: All versions prior to 2.0.0 were web-based and are now deprecated.*
