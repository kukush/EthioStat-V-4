# EthioStat Architecture

## Overview
EthioStat is a telecom-grade, dual-tracking native Android application designed to function as an offline-first billing engine. The active application operates as a **100% Pure Native Kotlin Application**, using a **MVVM / MVI** architecture compiled via a strictly typed **Kotlin DSL (`.kts`)** build system.

EthioStat tracks a user's telecom balances (Assets: Airtime, Internet, Voice, SMS, Bonus) separately from their financial history (Transactions: Income and Expenses). Every piece of state flows from a single offline-first Room database, mediated by a centralized Domain/UseCase layer, and graduation-level dependency injection powered by **Dagger Hilt**.

---

## Clean Architecture Layers

### 1. Presentation Layer (UI — Jetpack Compose)

All screens are built with 100% native Jetpack Compose. ViewModel state is collected using `collectAsStateWithLifecycle()` for lifecycle-aware reactivity.

#### Screens (`android/app/src/main/java/.../ui/screens/`)
... (existing content simplified for brevity)

### 2. State Management Layer (ViewModels)

All ViewModels are annotated with `@HiltViewModel` and use constructor injection. They delegate business logic to Use Cases.

| ViewModel | Responsibility |
|---|---|
| `HomeViewModel` | Dashboard state, financial summaries, and asset overview. |
| `TelecomViewModel` | Package management, USSD syncing, and airtime actions. |
| `TransactionViewModel` | Transaction history, filtering, search, and CSV export. |
| `SettingsViewModel` | User profile, theme, language, and SIM card management. |

### 3. Domain Layer (Use Cases)

A dedicated layer of single-responsibility classes that encapsulate business logic. This layer is pure Kotlin and independent of Android frameworks.

| Use Case | Purpose |
|---|---|
| `ParseSmsUseCase` | Business logic for converting raw SMS into structured results. |
| `FormatTransactionUseCase` | Logic for filtering and searching transaction lists. |
| `GetFinancialSummaryUseCase` | Aggregation logic for income/expense summaries. |
| `SyncAirtimeUseCase` | Orchestration of airtime-related USSD actions. |

### 4. Data Layer (Room & Repositories)

All persistence is local-first via Room using Kotlin coroutines (`Flow` and `suspend` functions). Repositories are injected via Hilt and handle data sourcing.

#### Repositories
- `TransactionRepository`: Manages financial transactions and SMS scanning.
- `BalanceRepository`: Manages telecom packages and balances.
- `SettingsRepository`: Manages user preferences via DataStore.
- `SmsRepository`: Low-level SMS inbox access and USSD dialing.

#### Entities

| Entity | Table | Purpose |
|---|---|---|
| `BalancePackageEntity` | `balance_packages` | Telecom assets. Canonical IDs: `airtime-sim1`, `internet-sim1`, `voice-sim1`, `sms-sim1`, `bonus-sim1`. |
| `TransactionEntity` | `transactions` | Financial income/expense events. `source` field holds the resolved sender label (e.g., `"TELEBIRR"`, `"CBE"`). |
| `SmsLogEntity` | `sms_log` | Audit log of every raw SMS processed. Used for dedup (hash-based) and replayability. |
| `TransactionSourceEntity` | `transaction_sources` | User-configured financial sender profiles (name, abbreviation, USSD code, senderId). |
| `UssdEntity` | `ussd_log` | Historical USSD event log (legacy; retained for DB compatibility). |

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
- `scanAllTransactionSources(days = 90)` scans **only** user-configured sources in the `transaction_sources` table. `AppConstants.SMS_SENDER_WHITELIST` is used for metadata/parsing only, not for deciding which SMS to scan.
- **90-day lookback** (was 7 days) — captures 3 months of history on first run.
- **Exact address matching** instead of `LIKE '%127%'` — uses `address = "127" OR address = "+251127" OR address = "251127" OR address = "0127"` to avoid false positives.
- **Wider-window honoring** — `cutoffTime = min(lastTimestamp, windowStart)` ensures that even if a previous scan ran, re-widening the window recovers missed messages.

#### `SmsForegroundService.kt` — Real-time Monitor & Sync Auto-Return

Persistent foreground service that listens for incoming SMS via `SmsReceiver` and immediately routes them through `ReconciliationEngine`.

When processing a telecom sender (994), the service also:
- Sends `ACTION_TELECOM_SMS_ARRIVED` broadcast to complete the sync early
- Shows a high-priority heads-up notification ("Telecom data updated — tap to return") so the user can return from the dialer
- Uses a dedicated `SyncNotificationChannel` (IMPORTANCE_HIGH) for heads-up visibility

---

### 5. Configuration Layer

#### `AppConstants.kt`

Single source of truth for all sender IDs, source labels, USSD codes, and broadcast action strings.

- `SMS_SENDER_WHITELIST` — complete set of known sender IDs that triggers reconciliation
- `TELEBIRR_SENDERS = setOf("127")` — numeric senders unified under `SOURCE_TELEBIRR`
- `resolveSource(sender)` — maps any raw or normalized sender to a human-readable label

#### `AndroidManifest.xml`

- Scoped permissions: `READ_SMS`, `RECEIVE_SMS`, `FOREGROUND_SERVICE`, `POST_NOTIFICATIONS`
- No accessibility service — USSD sync uses `ACTION_DIAL` (no `CALL_PHONE` needed) and reads 994 SMS responses

---

## Permission Handling

EthioStat requires **READ_SMS** and **RECEIVE_SMS** to function. The app **never crashes** if permissions are denied — it gracefully degrades and guides the user to grant permissions from Settings.

### Required Permissions

| Permission | Purpose |
|---|---|
| `READ_SMS` | Read SMS inbox for transactions, telecom packages, balance queries |
| `RECEIVE_SMS` | Real-time incoming SMS via `SmsReceiver` broadcast |
| `POST_NOTIFICATIONS` | Foreground service notification |

### Crash Prevention (Defensive Guards)

All SMS content provider queries are guarded at two levels:

1. **Early return** — `SmsRepository.hasSmsPermission()` at the top of `scanHistory()`, `refreshTelecomSmart()`, `scanAllTransactionSources()`. Returns 0 immediately if `READ_SMS` is not granted.
2. **SecurityException catch** — All `contentResolver.query()` calls wrapped in `try/catch(SecurityException)` as a fallback if permission is revoked mid-operation.
3. **ViewModel guards** — `TelecomViewModel.handleSync()` checks permission before dialing USSD; sets user-friendly `syncError` instead of crashing. `SettingsViewModel.addTransactionSource()` skips SMS scan when permission denied.

### Feature Disabling (Permission Denied State)

When `READ_SMS` or `RECEIVE_SMS` is not granted:

| Screen | Disabled Feature | Behavior |
|---|---|---|
| **Telecom** | Sync, Recharge, Transfer buttons | Grayed out (`enabled = false`); amber warning banner shown |
| **Settings** | "Add New" source action | Hidden from `SectionHeader`; `AddSourceSheet` blocked |
| **Settings** | Default source seeding | `seedDefaultSourcesIfEmpty()` returns empty list — no CBE/Telebirr seeded |
| **BottomNavBar** | Settings tab | Red badge dot (10dp) on Settings icon to draw user attention |

### Warning & Grant Flow

When permissions are missing, `SettingsScreen` displays a prominent **Permission Warning Card** (red surface, `RoundedCornerShape(32.dp)`) containing:

- **Header**: "Permission Required" with warning icon
- **Main message**: "To track balance, data, and expenses. Recharge easily. Permission is needed."
- **Explanation bullets** (3 items with icons):
  - Only chosen banks/wallets from Settings track transactions
  - Telecom packages: reads messages from 251994, 994
  - Balance checks (*804#, *805#) via USSD dial
- **"Grant Permission" button** — triggers `ActivityResultContracts.RequestMultiplePermissions` for `READ_SMS` + `RECEIVE_SMS`

Permission state is checked in `EthioBalanceAppUI` using `ContextCompat.checkSelfPermission()` and **re-checked on every `ON_RESUME`** lifecycle event (handles grant from system settings or dialog).

Translations provided in **en**, **am** (Amharic), **om** (Afaan Oromo) for all permission strings.

### Post-Grant Recovery

When permissions are granted (from Settings card or initial prompt):

```
permissionLauncher callback (all granted)
  → SettingsViewModel.onPermissionGranted()
    → settingsRepo.seedDefaultSourcesIfEmpty()     // Seeds CBE, Telebirr
    → smsRepo.refreshTelecomSmart()                 // Reads 994 SMS
    → smsRepo.scanAllTransactionSources(days = 90)  // 90-day history scan
    → settingsRepo.pruneEmptyDefaultSources()        // Remove sources with 0 transactions
  → smsPermissionGranted = true
  → UI recomposes: badge removed, buttons enabled, AddSource available
```

### Permission State Flow

```
App Launch (MainActivity)
  → checkSelfPermission(READ_SMS, RECEIVE_SMS, POST_NOTIFICATIONS)
  → All granted? → runStartupSeedAndScan(smsGranted=true)
  → Any denied?  → requestPermissionLauncher.launch(permissions)
    → Granted in dialog → runStartupSeedAndScan(smsGranted=true)
    → Denied in dialog  → runStartupSeedAndScan(smsGranted=false)
                            → seedDefaultSourcesIfEmpty() inserts nothing
                            → SMS scans skipped
                            → UI shows: badge on Settings, disabled features, permission card

Settings Screen (later)
  → User taps "Grant Permission"
  → permissionLauncher.launch([READ_SMS, RECEIVE_SMS])
  → Granted → onPermissionGranted() → full seed + scan
  → Denied  → no change, card remains visible
```

### Files Modified for Permission Handling

| File | Changes |
|---|---|
| `SmsRepository.kt` | `hasSmsPermission()` public method, guards + try-catch on all SMS queries |
| `SettingsRepository.kt` | `hasSmsPermission()`, `hasReceiveSmsPermission()`, `areAllPermissionsGranted()` public; skip seeding when denied |
| `EthioBalanceAppUI.kt` | Permission state with `ON_RESUME` re-check, `permissionLauncher`, passes state to all screens |
| `BottomNavBar.kt` | `hasPermissionWarning` param, red badge on Settings tab |
| `SettingsScreen.kt` | `smsPermissionGranted` + `onRequestPermissions` params, permission card, disabled AddSource |
| `TelecomScreen.kt` | `smsPermissionGranted` param, disabled buttons, amber warning banner |
| `SettingsViewModel.kt` | Permission guard in `addTransactionSource()`, `onPermissionGranted()` method |
| `TelecomViewModel.kt` | Permission guard in `handleSync()` |
| `Translations.kt` | 7 new keys (en/am/om): `permissionRequired`, `permissionMainMessage`, `permissionBankInfo`, `permissionTelecomInfo`, `permissionUssdInfo`, `grantPermission`, `smsPermissionNeeded` |

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
  → snapshot current packages (for change detection)
  → SmsRepository.dialUssd("*804#") — opens dialer with ACTION_DIAL
  → User presses Call → USSD popup → OK
  → Ethio Telecom sends 994 SMS with balance data
  → SmsReceiver → SmsForegroundService → ReconciliationEngine
      → processSms("994", body, timestamp)
      → sendBroadcast(ACTION_TELECOM_SMS_ARRIVED)
      → heads-up notification ("tap to return")
  → TelecomViewModel receives broadcast OR user presses Back
      → CompletableDeferred completes
      → refreshTelecomFromLatestSms(limit=10) best-effort re-read
      → compare packages: if no change → show 5s warning
  → StateFlow updated → Compose UI recomposed
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

### Unit Tests (`ParseSmsUseCaseTest.kt`)
Located at `android/app/src/test/java/com/ethiobalance/app/domain/usecase/`.

Covers (40+ cases):
- Telebirr purchase (dual-impact: expense + internet package)
- All internet/data message formats: Telebirr purchase, keyword-before-unit, remaining-balance, Amharic, Afaan Oromo, MB vs GB normalisation
- Voice package parsing (multi-segment and single-segment)
- SMS package parsing
- Bonus fund (multi-segment and standalone)
- Airtime balance query (*804# response)
- Loan taken, gift sent, income, service fee
- Dedup via `existsByHash`

### Permission Guard Tests (`PermissionGuardTest.kt`)
Located at `android/app/src/test/java/com/ethiobalance/app/ui/viewmodel/`.

Covers (9 cases):
- **Permission-denied contracts**: `scanHistory`, `refreshTelecomSmart`, `scanAllTransactionSources` return 0; `seedDefaultSourcesIfEmpty` inserts nothing; `addTransactionSource` skips scan; `handleSync` sets error message
- **Permission-granted contracts**: default sources are seeded, known banks resolve correctly for scanning, telecom senders defined for refresh

### TelecomViewModelTest (`TelecomViewModelTest.kt`)
Located at `android/app/src/test/java/com/ethiobalance/app/ui/viewmodel/`.

Covers (16 cases):
- Initial state, package filtering, data normalization
- `handleSync` state transitions (with `hasSmsPermission()` mocked `true`)
- Data change detection logic
- `rechargeViaUssd` and `transferAirtime` delegation

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
| SMS-based USSD sync (no Accessibility Service) | Uses `ACTION_DIAL` + 994 SMS reading; no special permissions; works across all OEM dialers |
| Graceful degradation on permission denial | No crash path exists; all SMS queries guarded with early-return + SecurityException catch; UI disables features and shows grant card |
| Permission re-check on ON_RESUME | Covers both in-app dialog grant and manual grant from Android system settings |
| Post-grant full recovery | `onPermissionGranted()` re-seeds defaults + scans 90-day history so user loses no data |
