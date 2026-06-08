# EthioStat — Play Store Compliance & Data Safety

---

## 1. Privacy Policy (Draft)

> **Note**: This draft must be hosted at a public URL before submission. Use GitHub Pages, Notion, or a simple static site. Google Play requires a clickable privacy policy link.

---

### EthioStat Privacy Policy

**Effective Date**: [INSERT DATE]
**Last Updated**: [INSERT DATE]

#### Overview

EthioStat ("the App") is a personal finance and telecom asset management application designed for the Ethiopian market. The App is developed and maintained by [DEVELOPER NAME / COMPANY].

**EthioStat is a 100% offline application. We do not collect, transmit, store, or share any user data with any third party. All data processing occurs entirely on your device.**

#### Information We Access

The App accesses the following on-device data to provide its core functionality:

| Data Type | Purpose | Stored Where | Shared? |
|---|---|---|---|
| **SMS Messages** | Parse financial transaction details from bank and Telebirr SMS messages | On-device Room database only | **Never** |
| **SMS Sender Address** | Identify which bank or telecom provider sent the message | On-device Room database only | **Never** |
| **SMS Timestamp** | Associate transactions with the correct date and time | On-device Room database only | **Never** |

#### Information We Do NOT Access

- **Contacts**: The App does not read your contacts
- **Location**: The App does not access GPS, network location, or any location data
- **Camera / Microphone**: The App does not access camera or microphone
- **Phone Calls**: The App does not make or intercept phone calls
- **Internet**: The App does not make network requests (no API calls, no analytics, no ads)
- **Device Identifiers**: The App does not read IMEI, Android ID, or advertising ID
- **Other Apps**: The App does not access data from other applications

#### Data Storage

All parsed transaction data, telecom asset information, and user preferences are stored **exclusively in a local Room database** on your device at:

```
/data/data/com.ethiobalance.app/databases/ethio_balance_db
```

This data is protected by Android's application sandbox and is not accessible to other applications.

#### Data Sharing

**We do not share any data with any third party.** Specifically:
- No data is sent to our servers (we have no servers)
- No data is sent to analytics services
- No data is sent to advertising networks
- No data is sent to data brokers
- No data is embedded in crash reports (we have no crash reporting)

#### Data Retention

All data remains on your device until:
- You uninstall the App (all data is deleted by Android)
- You clear the App's data via Android Settings
- You manually remove transaction sources in the App

#### User Rights

You have the right to:
- **Access**: View all your data within the App at any time
- **Delete**: Remove any or all data by clearing App data or uninstalling
- **Control**: Choose which bank/telecom sources the App monitors
- **Withdraw**: Revoke SMS permissions at any time via Android Settings

#### Children's Privacy

EthioStat is not directed at children under the age of 13. The App does not knowingly collect information from children.

#### Permissions Explained

| Permission | Why We Need It | What Happens Without It |
|---|---|---|
| `READ_SMS` | Read existing SMS messages from banks/Telebirr to parse financial transactions | The App cannot scan your SMS inbox for past transactions |
| `RECEIVE_SMS` | Detect new incoming SMS in real time to automatically log transactions | New transactions won't be detected until you manually open the App |
| `POST_NOTIFICATIONS` | Show a persistent notification for the background SMS monitoring service | Background SMS detection won't work on Android 13+ |

#### Changes to This Policy

We may update this Privacy Policy from time to time. Changes will be posted within the App and on our website. Continued use of the App after changes constitutes acceptance.

#### Contact

For questions about this Privacy Policy, contact:
- **Email**: [INSERT EMAIL]
- **Developer**: [INSERT NAME]

---

## 2. Data Safety Answers (Google Play Console)

These are the exact answers to provide in the Play Console Data Safety section.

### Section: Data Collection and Security

| Question | Answer |
|---|---|
| Does your app collect or share any of the required user data types? | **No** |
| Is all of the user data collected by your app encrypted in transit? | **Not applicable** — no data is transmitted |
| Do you provide a way for users to request that their data is deleted? | **Yes** — users can clear app data via Android Settings or uninstall the app |

### Section: Data Types

| Data Type | Collected? | Shared? | Purpose |
|---|---|---|---|
| Personal info (name, email, address) | **No** | **No** | — |
| Financial info (purchase history, credit info) | **No** — processed on-device only, not "collected" per Google's definition | **No** | — |
| Location | **No** | **No** | — |
| Web browsing history | **No** | **No** | — |
| Email or text messages | **No** — SMS is processed on-device only, not sent off-device | **No** | — |
| Photos or videos | **No** | **No** | — |
| Audio files | **No** | **No** | — |
| Files and docs | **No** | **No** | — |
| Calendar | **No** | **No** | — |
| Contacts | **No** | **No** | — |
| App activity | **No** | **No** | — |
| App info and performance | **No** | **No** | — |
| Device or other IDs | **No** | **No** | — |
| Health and fitness | **No** | **No** | — |

> **Key Google Play distinction**: Data that is processed on-device and never sent off the device is NOT considered "collected" under Google Play's Data Safety definitions. See [Google's policy](https://support.google.com/googleplay/android-developer/answer/10787469).

### Section: Security Practices

| Question | Answer |
|---|---|
| Is all collected data encrypted in transit? | **Not applicable** (no data leaves the device) |
| Can users request that data be deleted? | **Yes** (clear app data or uninstall) |
| Is this app committed to following the Families Policy? | **No** (not a children's app) |

---

## 3. Content Rating (IARC)

### Recommended Rating: **IARC 3+ / PEGI 3 / Everyone (E)**

### Questionnaire Answers

| Question | Answer |
|---|---|
| Does the app contain violence? | No |
| Does the app contain sexual content? | No |
| Does the app contain profanity or crude humor? | No |
| Does the app contain drug references? | No |
| Does the app contain gambling? | No |
| Does the app allow user-generated content? | No |
| Does the app allow uncontrolled internet access? | No |
| Does the app share personal info with third parties? | No |
| Does the app allow purchases? | No |
| Does the app contain ads? | No |
| Does the app contain horror or fear themes? | No |

**Justification**: EthioStat is a utility/finance tool that processes on-device data only. It contains no objectionable content, user interaction features, or online functionality.

---

## 4. Permission Justification (Play Console Declarations)

Google Play requires a justification for sensitive permissions. Use these exact texts.

### `READ_SMS`

**Declaration Type**: Core functionality

**Justification Text**:
> EthioStat requires READ_SMS to scan the user's SMS inbox for financial transaction messages from Ethiopian banks (CBE, Awash, Dashen, and 25+ others) and mobile money providers (Telebirr). The app parses these SMS messages on-device to automatically detect and categorize income, expenses, transfers, and telecom package purchases. This is the core functionality of the app — without SMS access, transactions cannot be detected. All SMS processing occurs entirely on-device. No SMS content is transmitted, stored remotely, or shared with any third party.

### `RECEIVE_SMS`

**Declaration Type**: Core functionality

**Justification Text**:
> EthioStat requires RECEIVE_SMS to detect new incoming SMS messages from banks and telecom providers in real time. When a new financial SMS arrives (e.g., a Telebirr payment confirmation or a CBE debit notification), the app immediately parses and categorizes the transaction without requiring the user to manually open the app. This enables automatic, real-time financial tracking. All processing occurs on-device. No SMS content is transmitted or shared.

### `POST_NOTIFICATIONS`

**Declaration Type**: Foreground service requirement

**Justification Text**:
> EthioStat uses a foreground service to continuously monitor incoming SMS messages for financial transactions. Android requires POST_NOTIFICATIONS permission (Android 13+) to display the persistent foreground service notification. This notification informs users that EthioStat is actively monitoring for bank and Telebirr SMS messages. The notification provides transparency about the background service and allows users to stop it at any time.

### `FOREGROUND_SERVICE`

**Declaration Type**: Background processing

**Justification Text**:
> EthioStat uses a shortService foreground service for brief SMS processing tasks. When a financial SMS arrives (from a bank or Telebirr), the service starts, parses the message into a structured transaction, saves it to the local Room database, and immediately stops itself. The service type is shortService because processing completes in under 1 second. No network activity occurs — all data stays on-device.

---

## 5. Pre-Launch Checklist

### Required Before Submission

- [ ] **Privacy Policy URL** — host the policy above at a public URL
- [ ] **App Icon** — 512x512 PNG uploaded
- [ ] **Feature Graphic** — 1024x500 PNG uploaded
- [ ] **Screenshots** — minimum 4, recommended 8 (1080x1920)
- [ ] **Short Description** — 80 characters max
- [ ] **Full Description** — 4,000 characters max
- [ ] **Content Rating** — complete IARC questionnaire
- [ ] **Data Safety** — complete all data safety declarations
- [ ] **Target Audience** — set to 18+ (finance app)
- [ ] **App Category** — Finance (primary), Tools (secondary)
- [ ] **Contact Email** — provide developer contact email
- [ ] **SMS Permission Declaration** — submit justification for READ_SMS and RECEIVE_SMS (Google requires review for SMS permissions)

### SMS Permission Review (Critical)

Google restricts SMS permissions to apps whose **core functionality** requires them. EthioStat qualifies because:

1. The app's primary purpose is **automatic financial transaction tracking via SMS parsing**
2. There is **no alternative method** to achieve this functionality (banks don't provide APIs)
3. SMS content is **never transmitted** off-device
4. The app does **not** use SMS for authentication, verification, or marketing

**Submission path**: Apply under the **"Financial transactions and account statements"** use case in the SMS/Call Log Permission Declaration form.

### Google Play Policy Compliance Notes

- [ ] **No deceptive behavior** — app does exactly what the description says
- [ ] **No misleading claims** — "100% offline" is verifiable (no INTERNET permission needed for core function)
- [ ] **No data exfiltration** — zero network calls for user data
- [ ] **Minimal permissions** — only 4 manifest permissions (2 restricted + 1 normal + 1 runtime)
- [ ] **Foreground service disclosure** — persistent notification informs users about background monitoring

---

## 6. Google Play Compliance Evaluation — All Permissions

### ✅ `READ_SMS` — KEEP (restricted, requires SMS Declaration Form)

| Criteria | Status |
|---|---|
| **Google Play policy** | Restricted. Only allowed for apps whose **core functionality** requires SMS. Must submit SMS/Call Log Permission Declaration Form. |
| **EthioStat usage** | Core functionality — parses bank/telecom SMS for financial transactions. Used in `SmsRepository.scanHistory()`, `scanAllTransactionSources()`, `refreshTelecomSmart()`. |
| **Submission category** | "Financial transactions and account statements" |
| **Verdict** | ✅ **Required.** App cannot function without it. |

### ✅ `RECEIVE_SMS` — KEEP (restricted, requires SMS Declaration Form)

| Criteria | Status |
|---|---|
| **Google Play policy** | Restricted. Same declaration form as READ_SMS. |
| **EthioStat usage** | Real-time SMS detection via `SmsReceiver` BroadcastReceiver → `SmsForegroundService` → `ReconciliationEngine` for instant transaction logging. |
| **Verdict** | ✅ **Required.** Without it, transactions only detected on manual app open. |

### ✅ `FOREGROUND_SERVICE` — KEEP (normal permission, no declaration needed)

| Criteria | Status |
|---|---|
| **Google Play policy** | Normal permission. Required for any app using `startForeground()`. Low scrutiny. |
| **EthioStat usage** | `SmsForegroundService.startForeground()` — processes incoming SMS briefly to avoid being killed by the system. |
| **Service type** | `shortService` — auto-stops at 3 minutes. Actual processing: <1 second. |
| **Verdict** | ✅ **Required.** Base permission for the foreground service. |

### ✅ `POST_NOTIFICATIONS` — KEEP (runtime permission, Android 13+)

| Criteria | Status |
|---|---|
| **Google Play policy** | Standard runtime permission since Android 13. No special declaration. |
| **EthioStat usage** | Foreground service notification + heads-up "Telecom data updated — tap to return" notification after USSD sync. |
| **Verdict** | ✅ **Required.** Without it, foreground service cannot show notification on Android 13+. |

### ❌ `RECEIVE_BOOT_COMPLETED` — NOT NEEDED

| Criteria | Status |
|---|---|
| **Google Play policy** | Normal permission, low scrutiny. Unused permissions signal poor quality to reviewers. |
| **EthioStat usage** | **None.** No `BootReceiver` exists in code or manifest. |
| **Notification flow** | Does NOT depend on this. `SmsReceiver` is manifest-registered — Android delivers `SMS_RECEIVED` automatically even after reboot. |
| **Verdict** | ❌ **Removed.** Dead permission. |

### ❌ `FOREGROUND_SERVICE_DATA_SYNC` — NOT NEEDED

| Criteria | Status |
|---|---|
| **Google Play policy** | Android 14+ type-specific permission. **High scrutiny** — Google requires video + written justification. Designed for apps that sync data with **remote servers**. |
| **EthioStat usage** | App is 100% offline (no INTERNET permission). Using `dataSync` for local processing is misleading and will likely trigger rejection. |
| **Verdict** | ❌ **Not appropriate.** Switched to `shortService` type which requires no type-specific permission. |

### ❌ `FOREGROUND_SERVICE_REMOTE_MESSAGING` — NOT NEEDED

| Criteria | Status |
|---|---|
| **Google Play policy** | For messaging client apps (WhatsApp, Telegram) that maintain persistent server connections. |
| **EthioStat usage** | Not a messaging app. Parses already-received SMS locally. No network, fully offline. |
| **Verdict** | ❌ **Not applicable.** |

### Final Manifest (4 permissions only)

```xml
<uses-permission android:name="android.permission.READ_SMS" />
<uses-permission android:name="android.permission.RECEIVE_SMS" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
```
