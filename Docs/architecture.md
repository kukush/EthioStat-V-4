# EthioStat Architecture

## Overview
EthioStat is a telecom-grade, dual-tracking native Android application designed to function as an offline-first billing engine. The active application operates as a **100% Pure Native Kotlin Application**, using a **MVVM / MVI** architecture compiled via a strictly typed **Kotlin DSL (`.kts`)** build system.

EthioStat tracks a user's telecom balances (Assets: Airtime, Internet, Voice, SMS, Bonus) separately from their financial history (Transactions: Income and Expenses). Every piece of state flows from a single offline-first Room database, mediated by a centralized Domain/UseCase layer, and dependency injection is powered by Dagger Hilt.

---

## System Architecture

### 1. Presentation Layer (UI — Jetpack Compose)

All screens are built with 100% native Jetpack Compose. There is no WebView / Capacitor layer in the active codebase.

#### Screens (`android/app/src/main/java/.../ui/screens/`)

| Screen | Description |
|---|---|
| `HomeScreen.kt` | Dashboard with dual-tracking Financial Summary card (net cash flow, income, expense) and a dark Telecom Assets hero card (canvas `drawBehind` radial glow). Displays Source Summaries and Recent Activity. |
| `TransactionScreen.kt` | Scrollable lazy list with a **sticky compact header** that appears on scroll (`derivedStateOf { firstVisibleItemIndex >= 1 }`). Time-period pill filters (All / Today / Week / Month), circular source chips (self-normalizing for Telebirr/127), expandable Summary Card, and CSV export. ↻ button shows a live `CircularProgressIndicator` while the 90-day SMS scan runs. |
| `TelecomScreen.kt` | Telecom asset detail screen. Packages are sorted **Airtime → Internet → Voice → SMS → Bonus**. Bottom-sheet dialogs for USSD recharge and airtime transfer. Hero balance card with mini package indicator bars. |
| `SettingsScreen.kt` | Profile editor with avatar grid, theme/language selector, SIM card management, transaction source management, and a **live Accessibility Status Card** (`UssdAccessibilityCard`) that detects whether the USSD service is enabled. |

#### Components (`android/app/src/main/java/.../ui/components/`)

| Component | Description |
|---|---|
| `TransactionItem.kt` | Expandable card with `AnimatedVisibility`. Emerald (income) / Rose (expense) color coding. Rotate-animated chevron. Expanded view shows full date, category, and transaction ID. |
| `PackageCard.kt` | Rich card with circular arc progress (canvas arc drawing), linear validity bar, and expiry date. |
| `Chip.kt` | Pill badge with `internet / voice / sms / bonus / default` variants. |
| `SummaryCard.kt` | Embedded in `TransactionScreen`; shows net balance, income/expense split, transaction count, and last-activity timestamp. Supports amount masking (eye icon toggle). |

---

### 2. State Management Layer (ViewModels)

All ViewModels are annotated with `@HiltViewModel` and inject dependencies via constructor. UI components consume state via `collectAsStateWithLifecycle()` for exact alignment with the Android lifecycle.

| ViewModel | Key Flows |
|---|---|
| `HomeViewModel` | `userName`, `userPhone`, `totalIncome`, `totalExpense`, `telecomBalance`, `packages`, `transactions` |
| `TelecomViewModel` | `packages`, `telecomBalance`, `isSyncing`, `syncError` |
| `TransactionViewModel` | `filteredTransactions`, `uniqueSources`, `totalIncome`, `totalExpense`, `timeFilter`, `sourceFilter`, `searchQuery`, **`isScanningHistory`** |
| `SettingsViewModel` | `userName`, `userPhone`, `userAvatar`, `simCards`, `transactionSources`, `theme`, `language` |

**Source normalization in `TransactionViewModel`:**
All transaction source values are mapped through `AppConstants.resolveSource()` before deduplication in `uniqueSources` and before comparison in the source filter. This means raw `"127"` stored in the DB and the alpha `"TELEBIRR"` both collapse to a single `TELEBIRR` chip in the UI.

---

### 3. Domain Layer (Use Cases)

A dedicated layer of single-responsibility classes (e.g. `ParseSmsUseCase`, `GetFilteredTransactionsUseCase`) coordinates business logic. This ensures ViewModels stay concerned with state management and Repositories stay focused on data fetching.

---

### 4. Data Layer (Room Database)

All persistence is local-first via Room using Kotlin coroutines (`Flow` and `suspend` functions). No network calls. All manual instantiation of database instances has been removed in favor of Hilt injection.

#### Entities

| Entity | Table | Purpose |
|---|---|---|
| `BalancePackageEntity` | `balance_packages` | Telecom assets. Canonical IDs: `airtime-sim1`, `internet-sim1`, `voice-sim1`, `sms-sim1`, `bonus-sim1`. |
| `TransactionEntity` | `transactions` | Financial income/expense events. `source` field holds the resolved sender label (e.g., `"TELEBIRR"`, `"CBE"`). |
| `SmsLogEntity` | `sms_log` | Audit log of every raw SMS processed. Used for dedup (hash-based) and replayability. |
| `TransactionSourceEntity` | `transaction_sources` | User-configured financial sender profiles (name, abbreviation, USSD code, senderId). |
| `UssdEntity` | `ussd_log` | Raw USSD popup text captured by the Accessibility Service. |
| `SimCardEntity` | `sim_cards` | Detected SIM card slots with phone numbers. |

---

### 4. Service Layer

#### `SmsParser.kt` — Regex Parsing Engine

Multi-language, multi-scenario SMS parser with confidence scoring (threshold: 0.70).

**Detected scenarios (`SmsScenario`):**

| Scenario | Meaning |
|---|---|
| `SELF_PURCHASE` | User paid for a telecom package (dual-impact: financial expense + asset gain) |
| `EXPENSE` | Pure financial expense (payment, transfer, fee) |
| `GIFT_SENT` | Airtime/money sent to another subscriber (expense) |
| `RECHARGE_OR_GIFT_RECEIVED` | Airtime or package received (asset gain, no financial record) |
| `LOAN_TAKEN` | Airtime loan received |
| `INCOME` | Money received (credit, salary, refund) |
| `BALANCE_UPDATE` | Package balance update without a transaction |
| `BALANCE_QUERY` | *804# query response with current airtime balance |
| `UNKNOWN` | Below confidence threshold; discarded |

**Data parsing — Internet/Data (new patterns added April 2026):**
- `"5GB Monthly Internet package"` → number+unit before keyword
- `"paid 100 ETB for 5GB internet"` → Telebirr purchase format
- `"Internet: 2.5GB"` → keyword before amount+unit
- `"You have 450MB data remaining"` → remaining-balance format
- `"ኢንተርኔት 500MB"` → Amharic
- `"Intarneetii 1GB"` → Afaan Oromo
- All values normalised to **MB** internally; GB × 1024.

**Multi-segment parsing:**
Handles Telebirr balance-status SMS in `;`-delimited segments for `voice`, `internet`, `sms`, and `bonus` packages. Uses a "largest-total-wins" `addOrReplace` strategy to prevent bonus/partial segments from overwriting main packages.

#### `ReconciliationEngine.kt` — Dual-Tracking Core

Processes every `ParsedSmsResult` and writes to both the financial and telecom asset tables.

- `normalizeSender()` strips carrier prefixes: `+251127` → `127`, `0127` → `127`
- Dedup via `existsByHash(sender, timestamp, bodyHash)` — safe for historical rescans
- Routes each `SmsScenario` to the correct DB write path (transaction insert + package upsert, package upsert only, etc.)

#### `SmsRepository.kt` — Historical SMS Scanner

Scans the Android SMS Inbox (`content://sms/inbox`) for all known senders.

**Key behaviours (updated April 2026):**
- `scanAllTransactionSources(days = 90)` merges **both** user-configured sources AND `AppConstants.SMS_SENDER_WHITELIST` — Telebirr (`"127"`) is always scanned even if not manually configured by the user.
- **90-day lookback** (was 7 days) — captures 3 months of history on first run.
- **Exact address matching** instead of `LIKE '%127%'` — uses `address = "127" OR address = "+251127" OR address = "251127" OR address = "0127"` to avoid false positives.
- **Wider-window honoring** — `cutoffTime = min(lastTimestamp, windowStart)` ensures that even if a previous scan ran, re-widening the window recovers missed messages.

#### `SmsForegroundService.kt` — Real-time Monitor

Persistent foreground service that listens for incoming SMS via `SmsReceiver` and immediately routes them through `ReconciliationEngine`.

#### `UssdAccessibilityService.kt` — USSD Popup Reader

Captures *804# balance response popups from the phone dialer. Updated April 2026:

- **Removed deprecated `recycle()`** calls (framework handles this on Android 9+)
- **Dual-path text capture**: fast `event.text` path first, then recursive node-tree walk as fallback (supports Android 5.0+)
- **Multi-manufacturer dialer support**: `ussd_accessibility_config.xml` now covers `com.android.phone`, `com.google.android.dialer`, `com.samsung.android.dialer`, `com.huawei.phone`
- Broadcasts result to UI via explicit-package `ACTION_USSD_RESPONSE` intent
- `buildSettingsIntent()` companion method opens system Accessibility Settings directly

---

### 5. Configuration Layer

#### `AppConstants.kt`

Single source of truth for all sender IDs, source labels, USSD codes, and broadcast action strings.

- `SMS_SENDER_WHITELIST` — complete set of known sender IDs that triggers reconciliation
- `TELEBIRR_SENDERS = setOf("127")` — numeric senders unified under `SOURCE_TELEBIRR`
- `resolveSource(sender)` — maps any raw or normalized sender to a human-readable label

#### `AndroidManifest.xml`

- `UssdAccessibilityService` declared with `BIND_ACCESSIBILITY_SERVICE` guard and `@xml/ussd_accessibility_config` metadata
- Scoped permissions: `READ_SMS`, `RECEIVE_SMS`, `CALL_PHONE`, `READ_PHONE_STATE`, `FOREGROUND_SERVICE`

---

## Data Flow

### Real-Time SMS Processing
```
Incoming SMS
  → SmsReceiver (whitelist filter)
  → SmsForegroundService
  → ReconciliationEngine.processSms()
      → normalizeSender()
      → dedup check (SmsLogDao.existsByHash)
      → SmsParser.parse() [confidence > 0.70]
      → write TransactionEntity and/or upsert BalancePackageEntity
  → StateFlow updated → Compose UI recomposed
```

### Historical SMS Scan (Manual Refresh or First Run)
```
User taps ↻ (or app first launch)
  → TransactionViewModel.scanSmsHistory()
      → isScanningHistory = true (spinner shown)
      → SmsRepository.scanAllTransactionSources(days = 90)
          → merge AppConstants.SMS_SENDER_WHITELIST + DB sources
          → per sender: exact-match query on content://sms/inbox
          → cutoffTime = min(lastScanTimestamp, now - 90 days)
          → ReconciliationEngine.processSms() for each row
      → isScanningHistory = false
```

### USSD Balance Sync (*804#)
```
User taps Sync in TelecomScreen
  → TelecomViewModel.handleSync()
  → SmsRepository.dialUssd("*804#")
  → Android dialer opens USSD session
  → USSD popup appears
  → UssdAccessibilityService.onAccessibilityEvent()
      → harvest text (event.text → node tree fallback)
      → ReconciliationEngine.processSms("804", response, ...)
      → sendBroadcast(ACTION_USSD_RESPONSE) for live UI update
```

---

## Build System

| File | Role |
|---|---|
| `settings.gradle.kts` | Project name, plugin repositories, dependency resolution |
| `build.gradle.kts` (root) | AGP and Kotlin plugin declarations |
| `app/build.gradle.kts` | compileSdk 36, minSdk 24, Compose BOM `2024.12.01`, Room `2.6.1`, JVM target 21 |
| `gradle.properties` | USSD code overrides via `ethiobalance.ussd.*` keys, injected as `BuildConfig` fields |
| `.github/workflows/ci.yml` | GitHub Actions — `gradle/actions/setup-gradle@v3`, automated unit tests |

---

## Testing

### Unit Tests (`SmsParserTest.kt`)
Located at `android/app/src/test/java/com/ethiobalance/app/services/`.

Covers (40+ cases):
- Telebirr purchase (dual-impact: expense + internet package)
- All internet/data message formats: Telebirr purchase, keyword-before-unit, remaining-balance, Amharic, Afaan Oromo, MB vs GB normalisation
- Voice package parsing (multi-segment and single-segment)
- SMS package parsing
- Bonus fund (multi-segment and standalone)
- Airtime balance query (*804# response)
- Loan taken, gift sent, income, service fee
- Dedup via `existsByHash`

### Integration Testing (`scripts/test-workflow.sh` + `scripts/mock-data.json`)
Shell script that injects mock SMS via `adb shell am broadcast` and reads results from Room via `adb shell content query`. Covers all dual-impact scenarios.

---

## Key Design Decisions

| Decision | Rationale |
|---|---|
| All values in MB internally | Avoids double-conversion bugs in UI; `PackageCard` displays as GB only if value ≥ 1024 |
| `addOrReplace` largest-total-wins | Prevents bonus/partial segments from overwriting real package data |
| Canonical package IDs (`airtime-sim1`) | Upsert-safe; historical rescans don't duplicate |
| 90-day scan window | Covers typical billing cycles and new-install onboarding |
| `resolveSource()` at read time, not write time | Old DB rows (e.g., source=`"127"`) are transparently remapped without a DB migration |
| Accessibility Service, not `telephonyManager.sendUssdRequest` | Works across all Android versions and OEM dialers; no elevated permissions required |
