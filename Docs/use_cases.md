# EthioStat Dual-Tracking Use Cases

EthioStat functions strictly as an offline-first analytical tool, enforcing accurate dual tracking of physical money vs telecom assets. Below are the precise, tested scenarios and how the native `ReconciliationEngine` handles them.

---

## 1. Standard Self-Purchase (Telebirr / Airtime)
**Trigger**: User buys a 1GB Data package for themselves.
**Sequence**:
- SMS 1 (Telebirr 830): *"You paid 19 ETB for 1GB..."*
- SMS 2 (Ethio 251994): *"You purchased 1GB..."*

**Dual-Tracking Execution**:
- **Financial Update**: The 830 message triggers an **EXPENSE** of 19 ETB (category: `PURCHASE`), lowering the dashboard Net Balance.
- **Asset Update**: The 251994 message triggers an **ASSET GAIN**, appending a new 1GB `BalancePackageEntity` to the database. The engine explicitly ignores adding a duplicate 19 ETB expense here.

## 2. Telebirr Transfer (ETB Sent to Another Person)
**Trigger**: User sends money via Telebirr to a contact.
**Sequence**: SMS from 127: *"Dear Tesfaye You have transferred ETB 220.00 to Worku Mengistu (2519****3881) on 31/03/2026 10:59. Your new balance is ETB 1,234.56."*

**Dual-Tracking Execution**:
- **Financial Update**: Triggers an **EXPENSE** of 220 ETB (category: `GIFT`), lowering Net Balance.
- **Asset Update**: Airtime balance updated to 1,234.56 ETB from the "new balance" clause in the same message.
- **Regex Note**: Real Telebirr messages use `"transferred ETB 220.00"` (ETB before amount). All financial regexes handle this via `(?:ETB\s*)?` prefix before the capture group.

## 3. Airtime Transfer (Gift Sent via USSD)
**Trigger**: User uses USSD `*806*...` to transfer airtime to family.
**Sequence**: SMS from 806: *"You transferred 25 ETB..."*

**Dual-Tracking Execution**:
- **Financial Update**: Triggers an **EXPENSE** of 25 ETB (category: `GIFT`), lowering Net Balance.
- **Asset Update**: No telecommunication packages are gained.

## 4. Package Gift Sent (*999#)
**Trigger**: User uses USSD `*999#` to buy a package for someone else.
**Sequence**: SMS from 251994: *"You gifted 2GB... 29 ETB deducted."*

**Dual-Tracking Execution**:
- **Financial Update**: Triggers an **EXPENSE** of 29 ETB (category: `GIFT`), lowering Net Balance.
- **Asset Update**: Explicitly blocked by engine. Even though it is a package SMS, it is categorized as `GIFT_SENT`, meaning the user does not gain any MB/Minutes.

## 5. Receiving a Package Gift
**Trigger**: User receives a package from someone else.
**Sequence**: SMS from 251994: *"You received a gift of 500MB..."*

**Dual-Tracking Execution**:
- **Financial Update**: No expense or income logged (the user did not spend or gain liquid cash).
- **Asset Update**: Triggers an **ASSET GAIN**, increasing the user's available Internet package balance.

## 6. Airtime Recharge (Standard Income)
**Trigger**: User recharges via voucher or digital banking.
**Sequence**: SMS from 805 / CBE / Telebirr: *"Account recharged with 50 ETB."*

**Dual-Tracking Execution**:
- **Financial Update**: Triggers an **INCOME** of 50 ETB, immediately raising the Net Balance.
- **Asset Update**: No package assets gained.

## 7. Airtime Received from Another Person
**Trigger**: Someone sends airtime to the user.
**Sequence**: SMS from 127: *"Dear Customer You have received ETB 50.00 airtime from 251970824468 on 31/03/2026."*

**Dual-Tracking Execution**:
- **Financial Update**: Triggers an **INCOME** of 50 ETB, raising the Net Balance.
- **Asset Update**: No package assets gained (airtime balance is tracked separately via balance query messages).

## 8. Airtime Loans (Credit)
**Trigger**: User takes an emergency loan.
**Sequence**: SMS from 810: *"You have taken a loan of 10 ETB..."*

**Dual-Tracking Execution**:
- **Financial Update**: Triggers an **INCOME** of 10 ETB, raising the localized usable cash. A separate flag allows tracking the 1.50 ETB fee/debt for future recharges.
- **Asset Update**: No assets gained.

## 9. Loan Repayment
**Trigger**: User repays airtime loan upon next recharge.
**Sequence**: SMS from 810: *"Repayment of 11.50 ETB has been deducted..."*

**Dual-Tracking Execution**:
- **Financial Update**: Triggers an **EXPENSE** of 11.50 ETB (category: `REPAYMENT`), lowering Net Balance.
- **Asset Update**: No package change.

## 10. Service Fee Deduction
**Trigger**: System deducts a service fee after a transfer or loan.
**Sequence**: SMS from 127: *"Service fee of 5 ETB deducted."*

**Dual-Tracking Execution**:
- **Financial Update**: Triggers an **EXPENSE** of 5 ETB (category: `FEE`), lowering Net Balance.
- **Asset Update**: No package change.

## 11. Bank Credit (CBE / Awash / Dashen)
**Trigger**: User receives a bank deposit or transfer.
**Sequence**: SMS from CBE (847): *"Your account has been credited with 15,500.00 ETB."*

**Dual-Tracking Execution**:
- **Financial Update**: Triggers an **INCOME** of 15,500 ETB (source: `CBE`), raising Net Balance.
- **Asset Update**: No telecom packages affected.

## 12. Bank Debit
**Trigger**: User's bank account is debited for a purchase or bill payment.
**Sequence**: SMS from Awash (901): *"Your account has been debited 500 ETB for payment."*

**Dual-Tracking Execution**:
- **Financial Update**: Triggers an **EXPENSE** of 500 ETB (category: `EXPENSE`, source: `Awash`), lowering Net Balance.
- **Asset Update**: No telecom packages affected.

## 13. Telebirr Payment (Purchase via Telebirr)
**Trigger**: User pays a merchant via Telebirr.
**Sequence**: SMS from 127: *"Dear Tesfaye You have paid ETB 550.00 for Monthly Internet Package 12GB from telebirr on 31/03/2026. Your telebirr account balance is ETB 684.25."*

**Dual-Tracking Execution**:
- **Financial Update**: Triggers an **EXPENSE** of 550 ETB (category: `PURCHASE`), lowering Net Balance.
- **Asset Update**: Airtime balance updated to 684.25 ETB. If the package was purchased for self, the corresponding data/voice/SMS package is also gained.

## 14. Bonus Tracking
**Trigger**: User receives a bonus from Ethio Telecom for recharging or as part of a promotional offer.
**Sequence (standalone)**: SMS from 251994: *"You have been awarded an ETB 7.50 bonus for recharging your prepaid account."*
**Sequence (multi-segment)**: SMS from 251994: *"...;  from Bonus Fund is 7.50 Birr and validity date..."*

**Dual-Tracking Execution**:
- **Financial Update**: No transaction logged (bonuses are not liquid cash).
- **Asset Update**: `bonus-sim1` `BalancePackageEntity` upserted with the bonus amount in ETB.

EthioStat functions strictly as an offline-first analytical tool, enforcing accurate dual tracking of physical money vs telecom assets. Business logic is strictly encapsulated in **Domain Layer Use Cases** (e.g., `ParseSmsUseCase`), ensuring a single source of truth for message interpretation.

---

## 15. Historical Database Initialization (90-Day Scan)
**Trigger**: The user installs EthioStat for the first time, grants SMS permission, or manually initiates a refresh from the Transaction screen.
**Sequence**: `TransactionViewModel.scanSmsHistory()` triggers `SmsRepository.scanAllTransactionSources(days = 90)`.

**Execution**:
- **Permission Flow**: `ActivityCompat.requestPermissions()` is handled in `MainActivity`.
- **System Scan**: `SmsRepository` queries `Telephony.Sms.Inbox.CONTENT_URI` for all whitelisted and user-configured senders.
- **90-Day Window**: The scan now covers 3 months of history by default to ensure a comprehensive initial financial profile.
- **Idempotent Reconciliation**: `ReconciliationEngine` (injected via Hilt) processes each message. Hash-based deduplication in `SmsLogDao` prevents double-counting of transactions even if the same message is scanned multiple times.

---

## 16. Technical Implementation (Clean Architecture)

EthioStat uses a multi-layered approach to ensure reliability and testability:
- **Domain Layer**: Contains pure Kotlin Use Cases like `ParseSmsUseCase` and `SyncAirtimeUseCase`. This is where all the "Dual-Tracking" logic lives, independent of the Android framework.
- **Data Layer**: Room DAOs and Entities (now 100% Kotlin) provide reactive `Flow` streams to the UI.
- **Presentation Layer**: Hilt-injected ViewModels coordinate state and delegate actions to Use Cases.
- **Service Layer**: Background services like `SmsForegroundService` and `UssdAccessibilityService` leverage the same injected `ReconciliationEngine` to ensure consistent processing of real-time events.

---
*For live testing, execute `sh scripts/test-workflow.sh` with an Android device connected.*

