# Adding a New Transaction Source

This document explains how developers can add a new transaction source (e.g., a new bank) to EthioBalance so that it can be selected from the Settings screen.

## 1. Update `AppConstants.kt`

- Locate the `AppConstants` object in `android/app/src/main/java/com/ethiobalance/app/AppConstants.kt`.
- Add a new constant for the source name and its sender IDs. Example for **Apollo**:

```kotlin
const val APOLLO = "apollo"
val APOLLO_SENDER_IDS = listOf("APOLLO", "apollo")
```

- If you are splitting an existing source (e.g., separating **CBEBirr** from **CBE**), create separate constants:

```kotlin
const val CBE = "CBE"
val CBE_SENDER_IDS = listOf("889", "847", "CBE")

const val CBEBIRR = "CBEBirr"
val CBEBIRR_SENDER_IDS = listOf("CBEBirr", "CBEBIRR")
```

## 2. Database Seed

- Open `AppDatabase.kt` and locate the migration or initial seed logic.
- Insert a new row for the source using the `TransactionSourceEntity` fields (`abbreviation`, `name`, `ussd`, `senderId`, `isEnabled`). Example:

```kotlin
database.execSQL("""
    INSERT INTO transaction_sources (abbreviation, name, ussd, senderId, isEnabled, lastUpdated)
    VALUES ('APOLLO', 'Apollo', '', 'apollo,APOLLO', 1, ${System.currentTimeMillis()})
""")
```

- For split sources, ensure the old entry is updated or removed accordingly.

## 3. UI – Settings Screen

- In `SettingsViewModel.kt` (or the relevant ViewModel), expose the list of transaction sources from the repository.
- Add a UI component (e.g., a dropdown or multi‑select) in `SettingsScreen.kt` that reads this list and allows the user to enable/disable a source.
- Bind the selection to the repository so that changes persist to the database.

## 4. Repository Layer

- `TransactionRepository` already provides methods to fetch and update `TransactionSourceEntity` objects. Ensure you have a method like `addOrUpdateSource(source: TransactionSourceEntity)`.
- If not present, implement it using the DAO `transactionSourceDao()`.

## 5. Tests

- Add unit tests in `AppConstantsTest.kt` to verify the new constants are defined.
- Add integration tests in `TransactionSourceDaoTest.kt` to confirm the source can be inserted and retrieved.
- Update existing tests that reference the old CBE constants to use the new split constants.

## 6. Documentation

- Update this file whenever a new source is added.
- Reference this guide from the main `README.md` under a **Developer Guide** section.

---

**Tip:** Keep the list of default sources in a single place (e.g., `AppConstants.kt`) to avoid duplication across the codebase.
