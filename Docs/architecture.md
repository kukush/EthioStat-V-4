# EthioStat Architecture

## Overview
EthioStat is a telecom-grade, dual-tracking hybrid mobile application designed to function as an offline-first billing engine. Built with React and Capacitor, its business logic operates primarily in Android native memory, ensuring deterministic state processing.

The app tracks a user's telecom balances (Assets: internet, voice, SMS, airtime, and bonuses) separately from its financial history (Transactions: income and expenses).

## System Architecture

### 1. Presentation Layer (UI)
React functional components are **strictly read-only** from a business state perspective.
- **Screens** (`src/presentation/screens/`):
  - `HomeScreen`: Displays dynamically computed net balances and current remaining telecom packages.
  - `TelecomScreen`: Reads active `BalancePackageEntity` stats from the database.
  - `TransactionScreen`: Reads `TransactionEntity` history with **color-coded income/expense cards** (emerald for income, rose for expense).
  - `SettingsScreen`: Manages UI configuration (theme, language, transaction sources).

- **Components** (`src/presentation/components/`):
  - `TransactionItem`: Renders individual transactions with type-aware coloring:
    - **Income**: emerald background (`bg-emerald-50/40`), emerald icon container, green amount text (`text-emerald-600`).
    - **Expense**: rose background (`bg-rose-50/40`), rose icon container, red amount text (`text-rose-600`).
  - `PackageCard`: Renders telecom package cards including bonus type.

### 2. State Management Layer (`useNativeData`)
React strictly prohibits telecom packages and transactions from residing in a Javascript `store`.
- React state relies on `useNativeData.ts`, which polls or observes Capacitor Plugins to pull directly from the native SQLite standard.

### 3. Native Integration Layer (Capacitor Bridge)
- **SmsMonitorPlugin** (`plugins/SmsMonitorPlugin.kt`):
  - `getBalances()` / `getTransactions()`: Query Room Database and return structured JSON.
  - `scanHistory({ senderId, days })`: Reads historical SMS from Android Telephony Provider and feeds them through `ReconciliationEngine` for idempotent processing.
  - `checkPermissions()` / `requestPermissions()`: Capacitor built-in permission flow for `READ_SMS` + `RECEIVE_SMS`.
  - `startMonitoring()`: No-op (permissions fully owned by Capacitor's built-in flow from JS).
  - `updateTransactionSources()`: Persists user-configured bank/financial senders to the whitelist.
  - `dialUssd()`: Opens the Android dialer with an encoded USSD code.

- **useNativeBridge** (`src/presentation/hooks/useNativeBridge.ts`):
  - On mount: `checkPermissions()` → `requestPermissions()` → `startMonitoring()` → `scanHistory()` for all `ALWAYS_SCAN_SENDERS` and user-configured sources.
  - `ALWAYS_SCAN_SENDERS`: `['127', '251994', '804', '810', '994', '830']` (EthioTelecom/Telebirr system senders).

### 4. Native Android Layer (The Single Source of Truth)
All computing executes offline in Kotlin.
- **Room Database**: Consists of explicit entities:
  - `BalancePackageEntity` (STATE) — telecom asset balances with canonical IDs (`airtime-sim1`, `internet-sim1`, `voice-sim1`, `sms-sim1`, `bonus-sim1`).
  - `TransactionEntity` (EVENTS) — financial income/expense records.
  - `SmsLogEntity` (AUDIT) — raw SMS strings for replayability.
  - `TransactionSourceEntity` — user-configured bank/financial senders.

- **SmsForegroundService**: Background service capturing all incoming SMS signals.

- **SmsReceiver**: BroadcastReceiver that filters incoming SMS by whitelist (system senders + user-configured + "TELEBIRR" keyword).

- **SmsParser**: Smart Regex engine with the following capabilities:
  - **Multi-segment parsing**: Handles Telebirr balance-status SMS with `;`-delimited segments for internet, voice, SMS, and **bonus** packages.
  - **Largest-total-wins strategy** (`addOrReplace`): Prevents bonus/partial segments from overwriting main packages.
  - **Trilingual regex**: English, Amharic (ሒሳ\S*, ቀሪ, ብር, ደቂቃ, ኤስኤምኤስ), and Afaan Oromo (Daqiiqaa, Intarneetii).
  - **ETB-before-amount support**: All financial regexes accept both `"transferred ETB 220.00"` (real Telebirr format) and `"transferred 220.00 ETB"` via `(?:ETB\s*)?` prefix before capture groups.
  - **SmsScenario enum**: `SELF_PURCHASE`, `EXPENSE`, `GIFT_SENT`, `RECHARGE_OR_GIFT_RECEIVED`, `LOAN_TAKEN`, `INCOME`, `BALANCE_UPDATE`, `BALANCE_QUERY`, `UNKNOWN`.
  - **Financial transaction regexes**: loan, repayment, credit, debit, payment, transfer, fee, recharge — each with `transactionCategory` for granular classification.
  - **Bonus parsing**: Multi-segment (`"Bonus Fund is 7.50 Birr"`) and standalone (`"awarded an ETB 7.50 bonus"`).
  - **Airtime balance**: Handles `"balance is 500 ETB"`, `"new balance is ETB 1,234.56"`, `"balance after transaction is ETB X"`.

- **ReconciliationEngine**: The heart of the system. Processes `ParsedSmsResult` by scenario:
  - `SELF_PURCHASE` / `BALANCE_UPDATE` / `BALANCE_QUERY`: Upsert packages via `insertOrUpdate`.
  - `INCOME`: Insert `TransactionEntity(type="INCOME")`.
  - `EXPENSE`: Insert `TransactionEntity(type="EXPENSE")` with category (PURCHASE, GIFT, REPAYMENT, FEE, EXPENSE) + upsert any associated packages.
  - `GIFT_SENT`: Insert expense transaction (no asset gain).
  - `RECHARGE_OR_GIFT_RECEIVED`: Upsert packages (asset gain, no financial transaction unless recharge).
  - `LOAN_TAKEN`: Insert income transaction.

## Data Flow

### Real-Time SMS Processing
1. **Event Reception**: SMS received by `SmsReceiver` → forwarded to `SmsForegroundService`.
2. **Audit Tracking**: Raw string logged to `SmsLogEntity` for replayability.
3. **Parse & Confidence Check**: `SmsParser` maps strings to `SmsScenario` enums (threshold > 0.70).
4. **Reconciliation**: `ReconciliationEngine` segregates assets from financial transactions.
5. **UI Rendering**: `SmsMonitorPlugin.getBalances()` returns packages + computed `NetBalance`.

### Historical SMS Scanning (App Startup)
1. **Permission Check**: `useNativeBridge` calls `checkPermissions()` / `requestPermissions()` via Capacitor built-in flow.
2. **Scan Trigger**: On `READ_SMS` grant, `scanHistory()` fires for each sender in `ALWAYS_SCAN_SENDERS` + user sources.
3. **Native Query**: `SmsMonitorPlugin.performSmsScan()` queries `content://sms/inbox` with `address LIKE %senderId%` and 7-day cutoff.
4. **Idempotent Processing**: Each message fed to `ReconciliationEngine.processSms()` — canonical IDs ensure upsert deduplication.

## Web Parser Parity
A TypeScript mirror parser (`src/data/smsParser.ts`) maintains feature parity with the Kotlin `SmsParser`:
- Same regex patterns for all financial transactions with `(?:ETB\s*)?` prefix support.
- Same multi-segment parsing with bonus segment handling.
- Same `addOrReplace` largest-total-wins strategy.
- Validated by 33+ automated test cases (`src/data/smsParser.test.ts`).
