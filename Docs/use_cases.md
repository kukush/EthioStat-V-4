# EthioStat Dual-Tracking Use Cases 

EthioStat functions strictly as an offline-first analytical tool, enforcing accurate dual tracking of physical money vs telecom assets. Below are the precise, tested scenarios and how the native `ReconciliationEngine` handles them.

## 1. Standard Self-Purchase (Telebirr / Airtime)
**Trigger**: User buys a 1GB Data package for themselves.
**Sequence**: 
- SMS 1 (Telebirr 830): *"You paid 19 ETB for 1GB..."*
- SMS 2 (Ethio 251994): *"You purchased 1GB..."*
**Dual-Tracking Execution**:
- **Financial Update**: The 830 message triggers an **EXPENSE** of 19 ETB, lowering the dashboard Net Balance.
- **Asset Update**: The 251994 message triggers an **ASSET GAIN**, appending a new 1GB `BalancePackageEntity` to the database. The engine explicitly ignores adding a duplicate 19 ETB expense here.

## 2. Airtime Transfer (Gift Sent)
**Trigger**: User uses USSD `*806*...` to transfer airtime to family.
**Sequence**: SMS from 806: *"You transferred 25 ETB..."*
**Dual-Tracking Execution**:
- **Financial Update**: Triggers an **EXPENSE** of 25 ETB, lowering Net Balance.
- **Asset Update**: No telecommunication packages are gained. 

## 3. Package Gift Sent (*999#)
**Trigger**: User uses USSD `*999#` to buy a package for someone else.
**Sequence**: SMS from 251994: *"You gifted 2GB... 29 ETB deducted."*
**Dual-Tracking Execution**:
- **Financial Update**: Triggers an **EXPENSE** of 29 ETB, lowering Net Balance.
- **Asset Update**: Explicitly blocked by engine. Even though it is a package SMS, it is categorized as `GIFT_SENT`, meaning the user does not gain any MB/Minutes.

## 4. Receiving a Package Gift
**Trigger**: User receives a package from someone else.
**Sequence**: SMS from 251994: *"You received a gift of 500MB..."*
**Dual-Tracking Execution**:
- **Financial Update**: No expense or income logged (the user did not spend or gain liquid cash).
- **Asset Update**: Triggers an **ASSET GAIN**, increasing the user's available Internet package balance.

## 5. Airtime Recharge (Standard Income)
**Trigger**: User recharges via voucher or digital banking.
**Sequence**: SMS from 805 / CBE / Telebirr: *"Account recharged with 50 ETB."*
**Dual-Tracking Execution**:
- **Financial Update**: Triggers an **INCOME** of 50 ETB, immediately raising the Net Balance. 
- **Asset Update**: No package assets gained.

## 6. Airtime Loans (Credit)
**Trigger**: User takes an emergency loan.
**Sequence**: SMS from 810: *"You have taken a loan of 10 ETB..."*
**Dual-Tracking Execution**:
- **Financial Update**: Triggers an **INCOME** of 10 ETB, raising the localized usable cash. A separate flag allows tracking the 1.50 ETB fee/debt for future recharges.
- **Asset Update**: No assets gained.

## 7. Historical Database Initialization (7-Day Read)
**Trigger**: The user installs EthioStat for the first time or manually adds a new bank source.
**Sequence**: App invokes `SmsMonitorPlugin.scanHistory(7)`.
**Dual-Tracking Execution**:
- **System Scan**: The plugin queries the native Android Telephony Provider for SMS messages belonging to configured numbers over the last 7 days.
- **Reconciliation Reset**: Messages are fed sequentially into the `ReconciliationEngine` to deterministically re-build the exact Net Financial Balance and Telecom Asset states accurately up to the current second.

## 8. Reset App / Unrecognized Formats (No Transaction Logged)
**Trigger**: A corrupted USSD result or an unmapped generic SMS arrives from 251994.
**Sequence**: SMS from 251994: *"Welcome to Ethio telecom! Dial *999#..."*
**Dual-Tracking Execution**:
- **Confidence Check Fails**: The native `SmsParser` assigns a confidence score evaluating below the `0.70` integrity threshold.
- **Action Omitted**: The `ReconciliationEngine` flags the entry as `UNKNOWN` and aborts Room DB writes for both `BalancePackageEntity` and `TransactionEntity`. This definitively protects the integrity of the math and drops "No Transaction" logs gracefully. In explicit resetting scenarios (e.g. user wipes data from settings), the database executes `deleteAll()` cleanly, leaving states 0.

---
*For live testing, execute `sh scripts/mock_sms.sh` with an Android emulator open to visually verify each of these explicitly calculated flows.*
