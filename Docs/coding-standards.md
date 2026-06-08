# EthioStat Coding Standards

This document establishes comprehensive coding standards and conventions for the EthioStat native Android project. The application is **100% Kotlin** with **Jetpack Compose** UI, following **Clean Architecture + MVVM**.

---

## Tech Stack

| Layer | Technology | Version |
|-------|-----------|---------|
| Language | Kotlin | 1.9.24 |
| JDK | Java | 21 |
| Build System | Gradle Kotlin DSL | 8.13 |
| UI Framework | Jetpack Compose (Material 3) | BOM 2024.12.01 |
| Architecture | MVVM + Clean Architecture | - |
| Dependency Injection | Hilt (Dagger) | 2.51.1 |
| Database | Room | 2.6.1 |
| Preferences | DataStore | 1.1.1 |
| Async | Kotlin Coroutines + Flow | - |
| Min SDK | Android API | 24 |
| Target SDK | Android API | 36 |
| Font | Manrope (Google Fonts) | - |

---

## File Organization

### Root Directory Policy
- **Only `README.md`** allowed in the project root
- All documentation in `Docs/`
- Build and deployment scripts in `scripts/`
- Agent rules in `.agents/rules/`

### Project Structure
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

---

## Kotlin Style

### General Rules
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
| Boolean variables | `is`/`has`/`can` prefix | `isLoading`, `hasError` |

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

## Architecture Standards

### Clean Architecture Layers

```
Compose UI  →  ViewModel  →  Use Case  →  Repository  →  DAO / Room
(presentation)   (state)     (domain)      (data)        (persistence)
```

- **Presentation**: Compose screens, collect state with `collectAsStateWithLifecycle()`
- **ViewModel**: `@HiltViewModel`, expose `StateFlow`, delegate to Use Cases
- **Domain**: Pure Kotlin — zero Android imports. `operator fun invoke()` for Use Cases
- **Data**: Room entities, DAOs (`Flow` for reads, `suspend` for writes), Repositories

### Dependency Direction
Domain defines interfaces; Data implements them. Never import UI into ViewModel or Domain.

---

## Data Layer (Room)

### Entity Conventions

```kotlin
@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey val id: String,  // UUID, not auto-generated
    val type: String,            // Enum values as String constants
    val amount: Double,
    val timestamp: Long,         // Unix milliseconds
    val reference: String?,      // Nullable fields marked with ?
)
```

### DAO Conventions

```kotlin
@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAll(): Flow<List<TransactionEntity>>  // Flow for reactive streams

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: TransactionEntity)  // suspend for writes

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
- **Services**: `@AndroidEntryPoint` for field injection in lifecycle components
- **App class**: `@HiltAndroidApp`

---

## ViewModel Layer

```kotlin
@HiltViewModel
class TransactionViewModel @Inject constructor(
    private val repository: TransactionRepository,
    private val parseUseCase: ParseSmsUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    fun loadData() {
        viewModelScope.launch {
            // ...
        }
    }
}
```

### Rules
- Use `StateFlow` with private `MutableStateFlow` backing
- Use `viewModelScope` for all coroutines
- **No Compose imports** — no `androidx.compose.*` in this layer
- Expose read-only state for unidirectional data flow

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
- Collect with `collectAsStateWithLifecycle()` — never bare `collectAsState()`
- Use `SharingStarted.WhileSubscribed(5000)` for automatic cleanup

### Theme Usage
- **Never** use hardcoded colors — use theme colors from `Color.kt`
- Import theme: `import com.ethiobalance.app.ui.theme.*`
- Available scales: `Slate*`, `Blue*`, `Emerald*`, `Rose*`, `Purple*`, `Amber*`

### Component Guidelines
- Use `RoundedCornerShape(16.dp)` or `RoundedCornerShape(24.dp)` for cards
- Use `CircleShape` for avatars / action buttons
- Use `Modifier.padding(horizontal = 20.dp)` for screen horizontal padding
- Spacer pattern: `Spacer(Modifier.height(16.dp))` between sections
- Accept `modifier: Modifier = Modifier` as first optional param in reusable composables
- Handle side effects with `LaunchedEffect`, `DisposableEffect`, or `SideEffect`

---

## Constants Management

| Constant Type | Location | Example |
|--------------|----------|---------|
| App-wide (SMS senders, USSD) | `AppConstants.kt` | `SMS_SENDER_WHITELIST` |
| Bank metadata | `AppConstants.kt` | `KNOWN_BANKS` |
| Avatars | `constants/Avatars.kt` | `Avatars.OPTIONS` |
| Languages | `constants/Languages.kt` | `Languages.SUPPORTED` |
| Phone formatting | `constants/PhoneConstants.kt` | `COUNTRY_CODE` |
| Build-time config | `gradle.properties` | `ethiobalance.ussd.balance_check` |
| UI-only constants | Within screen file | `private val ITEM_HEIGHT = 48.dp` |

### No Hardcoded Values

**Never allow:**
- Magic numbers (e.g., `13` for phone length) — use `PhoneConstants.MAX_FULL_LENGTH`
- Hardcoded phone prefixes (`"+251"`) — use `PhoneConstants.COUNTRY_CODE`
- Inline color values (`Color(0xFF123456)`) — use theme colors
- Duplicated bank lists — use `AppConstants.KNOWN_BANKS`
- Duplicated telecom senders — use `AppConstants.TELECOM_SENDERS`

---

## Internationalization (i18n)

```kotlin
// Translations.kt
object Translations {
    private val strings = mapOf(
        "en" to mapOf("key" to "English text"),
        "am" to mapOf("key" to "Amharic text"),
        "om" to mapOf("key" to "Afaan Oromo text")
    )

    fun t(lang: String, key: String): String =
        strings[lang]?.get(key) ?: strings["en"]?.get(key) ?: key
}

// Usage in UI
Text(Translations.t(language, "settings"))
```

- Three languages supported: **en**, **am**, **om**
- Ethiopian calendar formatting via `android.icu.util.EthiopicCalendar` (min SDK 24)
- Manual month name maps for Amharic and Oromo (ICU produces wrong names)

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
- `0.95f` — High confidence (Telebirr patterns, exact matches)
- `0.9f` — Standard confidence (bank patterns)
- `0.85f` — Lower confidence (generic patterns)
- `< 0.7f` — Reject (unknown/ambiguous)

### Regex Guidelines
- Use `RegexOption.IGNORE_CASE` for case-insensitive matching
- Support multilingual: English, Amharic (`[\u1200-\u137F]`), Afaan Oromo
- Use non-capturing groups: `(?:pattern)`

---

## SMS Source Scoping (Non-Negotiable)

- **Scanning**: `SmsRepository.scanAllTransactionSources()` scans ONLY user-configured sources in `transaction_sources` table
- **Real-time**: `SmsReceiver` gates on the DB-synced SharedPreferences whitelist (`sms_whitelist`) only
- **Default Sources**: Defined in `gradle.properties` via `ethiobalance.default_sources`, accessed through `BuildConfig`
- **Sender Variants**: Populate `senderId` with ALL known variants (comma-separated)
- **Metadata Only**: `KNOWN_BANKS` and `SMS_SENDER_WHITELIST` exist for metadata/parsing, not for acceptance gating

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

### Running Tests
```bash
# JVM unit tests
./gradlew testDebugUnitTest

# Integration tests (device required)
./scripts/test-workflow.sh
```

---

## Code Review Checklist

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
- [ ] No Compose imports in ViewModel layer
- [ ] No Android imports in Domain layer

---

## Enforcement

These standards are enforced through:
- **`.agents/rules/`** — AI agent rules auto-applied during development
- **Gradle build** — compile-time type safety via Kotlin DSL
- **CI pipeline** — `./gradlew testDebugUnitTest` in GitHub Actions
- **Code review process** — checklist above
- **Documentation updates** — when standards evolve

This document serves as the authoritative source for all EthioStat development standards and should be referenced during development, code reviews, and onboarding.
