---
description: EthioBalance Android project coding standards and architecture guidelines
trigger: glob
globs: "**/*"
---

# EthioBalance Project Standards

## Project Overview

**EthioBalance** is a privacy-focused, offline-first financial and telecom asset management Android application for the Ethiopian market. It automatically parses SMS messages from Ethio Telecom, Telebirr, and major Ethiopian banks.

---

## Tech Stack

| Layer | Technology | Version |
|-------|-----------|---------|
| Language | Kotlin | 1.9.24 |
| JDK | Java | 21 |
| Build System | Gradle Kotlin DSL | 8.13 |
| UI Framework | Jetpack Compose (Material3) | BOM 2024.12.01 |
| Architecture | MVVM + Clean Architecture | - |
| Dependency Injection | Hilt (Dagger) | 2.51.1 |
| Database | Room | 2.6.1 |
| Preferences | DataStore | 1.1.1 |
| Async | Kotlin Coroutines + Flow | - |
| Min SDK | Android API | 24 |
| Target SDK | Android API | 36 |
| Font | Manrope (Google Fonts) | - |

---

## Architecture Patterns

### 1. Clean Architecture Layers

```
android/app/src/main/java/com/ethiobalance/app/
├── data/           # Entities, DAOs, Database (Room)
├── domain/         # Use cases, domain models
│   ├── model/
│   └── usecase/
├── repository/     # Data repositories (single source of truth)
├── services/       # Background services, engines
├── ui/             # UI layer (Compose)
│   ├── components/ # Reusable UI components
│   ├── screens/    # Screen composables
│   ├── theme/      # Colors, Typography, Theme
│   ├── viewmodel/  # ViewModels (Hilt-injected)
│   └── Translations.kt  # i18n (en, am, om)
├── constants/      # Constants (Avatars, Languages, Phone)
├── di/             # Dependency injection modules (Hilt)
└── AppConstants.kt # App-wide constants, SMS sender whitelist
```

### 2. MVVM Pattern

- **View**: Compose screens in `ui/screens/`
- **ViewModel**: `ui/viewmodel/`, annotated with `@HiltViewModel`, uses `StateFlow` for state
- **Model**: Room entities in `data/`, exposed via Repository pattern

### 3. Repository Pattern

All data access goes through Repository classes:
- `SmsRepository` - SMS scanning, USSD operations
- `TransactionRepository` - Transaction CRUD
- `BalanceRepository` - Package/balance queries
- `SettingsRepository` - User preferences (DataStore)

---

## Database (Room) Standards

### Entity Conventions

```kotlin
@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey val id: String,  // UUID, not auto-generated
    val type: String,            // Enum values as String constants
    val amount: Double,
    val timestamp: Long,         // Unix milliseconds
    val reference: String?,      // Nullable fields marked with ?
    // ...
)
```

### DAO Conventions

```kotlin
@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAll(): Flow<List<TransactionEntity>>  // Flow for reactive streams

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: TransactionEntity)  // suspend for coroutines

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getById(id: String): TransactionEntity?
}
```

### Database Versioning

- Increment `version` in `@Database` annotation for schema changes
- Use `fallbackToDestructiveMigration()` only in development
- Migration scripts required for production releases

---

## Dependency Injection (Hilt)

### Module Pattern

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(context, AppDatabase::class.java, "ethio_balance_db")
            .fallbackToDestructiveMigration()
            .build()
    }
}
```

### Injection Points

- **ViewModels**: `@HiltViewModel` with `@Inject` constructor
- **Use Cases**: `@Inject` constructor
- **Repositories**: `@Inject` constructor with `@ApplicationContext` for Context
- **Services**: Field injection for Android lifecycle components

---

## UI (Compose) Standards

### Screen Structure

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenName(
    param1: String,
    onEvent: () -> Unit,
    viewModel: ScreenViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    // Screen content
}
```

### State Management

- Use `StateFlow` in ViewModels
- Collect with `collectAsStateWithLifecycle()` in Compose
- Use `SharingStarted.WhileSubscribed(5000)` for automatic cleanup

### Theme Usage

- **Never** use hardcoded colors; use theme colors from `Color.kt`
- Import theme: `import com.ethiobalance.app.ui.theme.*`
- Available color scales: `Slate*`, `Blue*`, `Emerald*`, `Rose*`, `Purple*`, `Amber*`

### Component Guidelines

- Use `RoundedCornerShape(16.dp)` or `RoundedCornerShape(24.dp)` for cards
- Use `CircleShape` for avatars/action buttons
- Use `Modifier.padding(horizontal = 20.dp)` for screen horizontal padding
- Spacer pattern: `Spacer(Modifier.height(16.dp))` between sections

---

## Constants Management

### Location Standards

| Constant Type | Location | Example |
|--------------|----------|---------|
| App-wide (SMS senders, USSD) | `AppConstants.kt` | `SMS_SENDER_WHITELIST` |
| Bank metadata | `AppConstants.kt` | `KNOWN_BANKS` |
| Avatars | `constants/Avatars.kt` | `Avatars.OPTIONS` |
| Languages | `constants/Languages.kt` | `Languages.SUPPORTED` |
| Phone formatting | `constants/PhoneConstants.kt` | `COUNTRY_CODE` |
| UI-only constants | Within screen file | `private val ITEM_HEIGHT = 48.dp` |

### Hardcoded Value Rules

❌ **Never allow:**
- Magic numbers (e.g., `13` for phone length) → Use `PhoneConstants.MAX_FULL_LENGTH`
- Hardcoded phone prefixes (`"+251"`) → Use `PhoneConstants.COUNTRY_CODE`
- Inline color values (`Color(0xFF123456)`) → Use theme colors
- Duplicated bank lists → Use `AppConstants.KNOWN_BANKS`
- Duplicated telecom senders → Use `AppConstants.TELECOM_SENDERS`

---

## SMS Parsing Standards

### ParseSmsUseCase Pattern

```kotlin
class ParseSmsUseCase @Inject constructor() {
    operator fun invoke(sender: String, body: String, timestamp: Long): ParsedSmsResult {
        // Parsing logic with confidence scoring
        return ParsedSmsResult(scenario, confidence, ...)
    }
}
```

### Confidence Scoring

- `0.95f` - High confidence (Telebirr patterns, exact matches)
- `0.9f` - Standard confidence (bank patterns)
- `0.85f` - Lower confidence (generic patterns)
- `< 0.7f` - Reject (unknown/ambiguous)

### Regex Guidelines

- Use `RegexOption.IGNORE_CASE` for case-insensitive matching
- Support multilingual: English, Amharic (`[\u1200-\u137F]`), Afaan Oromoo
- Extract with non-capturing groups where appropriate: `(?:pattern)`

---

## USSD Dialing Standards

### Permission-Free Dialing

**Always** use `ACTION_DIAL` instead of `ACTION_CALL` to avoid `CALL_PHONE` permission:

```kotlin
fun dialUssd(code: String, context: Context) {
    val encodedCode = code.replace("#", "%23")
    val uri = android.net.Uri.parse("tel:$encodedCode")
    val intent = android.content.Intent(android.content.Intent.ACTION_DIAL, uri).apply {
        flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
    }
    context.startActivity(intent)
}
```

### UX Requirements

| Requirement | Implementation |
|-------------|----------------|
| ✅ Use `ACTION_DIAL` | Never use `ACTION_CALL` or `CALL_PHONE` permission |
| ✅ Confirmation Dialog | Show dialog before dialing: "Dial *804# to check balance?" |
| ✅ Graceful Fallback | Handle `ActivityNotFoundException` for dialer apps |

### Example Implementation

```kotlin
@Composable
fun UssdDialButton(code: String, label: String) {
    val context = LocalContext.current
    var showDialog by remember { mutableStateOf(false) }

    Button(onClick = { showDialog = true }) {
        Text(label)
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Confirm USSD") },
            text = { Text("Dial $code?") },
            confirmButton = {
                TextButton(onClick = {
                    dialUssd(code, context)
                    showDialog = false
                }) {
                    Text("Dial")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
```

---

## Internationalization (i18n)

### Translation Pattern

```kotlin
// Translations.kt
object Translations {
    private val strings = mapOf(
        "en" to mapOf("key" to "English text"),
        "am" to mapOf("key" to "አማርኛ ጽሑፍ"),
        "om" to mapOf("key" to "Barreeffama Oromoo")
    )

    fun t(lang: String, key: String): String =
        strings[lang]?.get(key) ?: strings["en"]?.get(key) ?: key
}

// Usage in UI
Text(Translations.t(language, "settings") ?: "Settings")
```

---

## Testing Standards

### Test Structure

```
android/app/src/test/java/com/ethiobalance/app/
├── domain/usecase/     # Use case unit tests
├── repository/         # Repository tests (mock DAOs)
├── services/           # Service logic tests
└── ui/                 # ViewModel tests
```

### Testing Patterns

- Use JUnit 4 (`@Test` annotation)
- Mock dependencies with manual fakes or Mockito
- Test file naming: `{ClassUnderTest}Test.kt`
- Test method naming: `methodName_condition_expectedResult()`

---

## Code Style Guidelines

### Kotlin Style

- **Trailing commas** in multi-line parameter lists
- **Explicit visibility** modifiers (`private`, `internal`)
- **Expression bodies** for single-expression functions
- **Type inference** where obvious

### Naming Conventions

| Type | Pattern | Example |
|------|---------|---------|
| Classes | PascalCase | `TransactionRepository` |
| Functions | camelCase | `scanHistory()` |
| Constants | UPPER_SNAKE | `SMS_SENDER_WHITELIST` |
| Compose functions | PascalCase | `SettingsScreen()` |
| Private vals | camelCase | `telecomTypes` |

### Import Organization

```kotlin
// 1. Android imports
import android.content.Context
import android.provider.Telephony

// 2. AndroidX imports
import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel

// 3. Project imports (alphabetical by package)
import com.ethiobalance.app.AppConstants
import com.ethiobalance.app.data.TransactionEntity
import com.ethiobalance.app.ui.theme.*

// 4. Third-party imports
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject
```

---

## Build Configuration

### Gradle Properties (gradle.properties)

```properties
ethiobalance.ussd.balance_check=*804#
ethiobalance.ussd.recharge_self=*805*
ethiobalance.ussd.recharge_other=*805*
ethiobalance.ussd.transfer_airtime=*806*
ethiobalance.ussd.gift_package=*999#
ethiobalance.phone_app_package=com.android.phone
ethiobalance.default_sources=CBE,TELEBIRR
```

### BuildConfig Access

```kotlin
// In AppConstants.kt
val USSD_BALANCE_CHECK: String get() = BuildConfig.USSD_BALANCE_CHECK
```

---

## Checklist for New Code

- [ ] Uses existing constants from `AppConstants` or appropriate `constants/` file
- [ ] No hardcoded colors (use theme colors)
- [ ] No hardcoded phone numbers/prefixes (use `PhoneConstants`)
- [ ] No magic numbers (use named constants)
- [ ] Proper Hilt injection (`@Inject` constructor or `@HiltViewModel`)
- [ ] Room entities use appropriate nullable markers (`?`)
- [ ] DAO methods use `suspend` for IO operations
- [ ] ViewModels expose `StateFlow` with proper `SharingStarted` strategy
- [ ] UI uses `collectAsStateWithLifecycle()`
- [ ] Screens use `RoundedCornerShape` and theme-consistent spacing
- [ ] Translations use `Translations.t()` with fallback
- [ ] Tests added for new use cases and repositories

---

## SMS Source Scoping (Non-Negotiable)

Rules governing how SMS sources are managed and processed:

- **Scanning Restriction**: `SmsRepository.scanAllTransactionSources()` MUST scan ONLY user-configured sources in `transaction_sources` table. NEVER union with `AppConstants.SMS_SENDER_WHITELIST`.

- **Real-time Restriction**: `SmsReceiver` MUST gate incoming SMS on the DB-synced SharedPreferences whitelist (`sms_whitelist`) only. The broad `resolveSource()` fallback must NOT be used as an acceptance gate. Telecom senders (994, 804, etc.) are accepted via separate check.

- **Default Sources**: Defined in `gradle.properties` via `ethiobalance.default_sources` and accessed through `BuildConfig.DEFAULT_TRANSACTION_SOURCES` → `AppConstants.DEFAULT_TRANSACTION_SOURCES`. Do not hardcode them in Kotlin.

- **Sender Variants**: When storing transaction sources, populate `senderId` with ALL known variants (comma-separated) using `getAllSenderIdsForBank()`. Example: CBE stores `"889,847,CBE,CBEBirr,CBEBIRR"`.

- **Metadata Only**: `KNOWN_BANKS` and `SMS_SENDER_WHITELIST` exist for metadata/parsing only, not for deciding which SMS to process.

- **UI Filtering**: Add-source UI MUST filter out already-configured sources (defaults + user-added) to prevent duplicates.
