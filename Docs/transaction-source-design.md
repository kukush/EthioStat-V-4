# Transaction Source Addition Design

## Goal

Provide a **single‑point** mechanism for developers to add new transaction sources (banks, telecom services) without touching multiple parts of the codebase. The workflow should be:

1. Add the source definition in `AppConstants.kt`.
2. Run a Gradle task that generates the required **Room migration** and updates the **default source list**.
3. The Settings UI automatically reflects the new source because it reads from the repository.

## 1. Central Source Definition

Create a data class inside `AppConstants.kt`:

```kotlin
data class TransactionSourceDef(
    val abbreviation: String,   // Upper‑case key used in TransactionEntity.source
    val displayName: String,    // Human readable name shown in UI
    val senderIds: List<String>,// All possible SMS sender strings (numeric or alpha)
    val ussd: String = ""      // Optional USSD code for balance checks
)
```

Add a **list** of default sources:

```kotlin
val DEFAULT_TRANSACTION_SOURCES_DEF = listOf(
    TransactionSourceDef(
        abbreviation = "CBE",
        displayName = "CBE",
        senderIds = listOf("889", "847", "CBE")
    ),
    TransactionSourceDef(
        abbreviation = "CBEBIRR",
        displayName = "CBEBirr",
        senderIds = listOf("CBEBirr", "CBEBIRR")
    ),
    TransactionSourceDef(
        abbreviation = "APOLLO",
        displayName = "Apollo",
        senderIds = listOf("apollo", "APOLLO")
    ),
    // …other existing sources
)
```

All other parts of the app will reference this list via `AppConstants.DEFAULT_TRANSACTION_SOURCES_DEF`.

## 2. Automatic Room Migration Generation

Add a **Gradle task** (`generateSourceMigration`) that:

- Reads `AppConstants.DEFAULT_TRANSACTION_SOURCES_DEF`.
- Generates a Kotlin file `GeneratedSourceMigration.kt` containing a `Migration` object that inserts any missing rows into `transaction_sources`.
- The task is hooked into `preBuild` so the migration is always up‑to‑date.

Sample generated code (simplified):

```kotlin
object SourceMigration {
    val MIGRATION_X_Y = object : Migration(X, Y) {
        override fun migrate(database: SupportSQLiteDatabase) {
            val sources = listOf(
                TransactionSourceEntity("CBE", "Commercial Bank of Ethiopia", "", "889,847,CBE", 1, System.currentTimeMillis()),
                TransactionSourceEntity("CBEBIRR", "CBEBirr", "", "CBEBirr,CBEBIRR", 1, System.currentTimeMillis()),
                TransactionSourceEntity("APOLLO", "Apollo", "", "apollo,APOLLO", 1, System.currentTimeMillis())
            )
            sources.forEach { src ->
                database.execSQL("""
                    INSERT OR IGNORE INTO transaction_sources
                    (abbreviation, name, ussd, senderId, isEnabled, lastUpdated)
                    VALUES ('${src.abbreviation}', '${src.name}', '${src.ussd}', '${src.senderId}', 1, ${src.lastUpdated})
                """)
            }
        }
    }
}
```

Developers only need to **add a new `TransactionSourceDef`**; the task will regenerate the migration and the app will pick it up automatically.

## 3. Settings UI Integration

The Settings screen already consumes the list from `TransactionRepository.getAllSources()`. No UI changes are required because the repository now returns the rows inserted by the migration.

If a developer wants to expose a toggle for a new source, they simply add the source to the list; the UI will display it automatically.

## 4. Testing Strategy

- **Unit test** for the Gradle task – verify that adding a new definition results in a new `INSERT` statement in the generated file.
- **Integration test** – run the migration on an in‑memory database and assert that the new source exists.
- Update existing `AppConstantsTest.kt` to assert that `DEFAULT_TRANSACTION_SOURCES_DEF` contains the expected abbreviations.

## 5. Documentation

- Keep this design file (`Docs/transaction-source-design.md`).
- Update `Docs/add-transaction-source.md` to point developers to the `TransactionSourceDef` list.

---

### Summary

1. **Single source of truth** – `TransactionSourceDef` list in `AppConstants.kt`.
2. **Gradle‑driven migration** – `generateSourceMigration` creates/updates the Room migration automatically.
3. **Zero UI changes** – Settings UI reads from the repository, so new sources appear instantly.
4. **Tests** – Verify generation and migration correctness.
