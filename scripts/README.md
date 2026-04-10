# EthioStat Test Scripts

## test-workflow.sh — End-to-End SMS Workflow Test

Validates the full SMS → Parse → Transaction pipeline using **real device SMS formats** captured from actual Ethiopian bank and telecom messages.

### Prerequisites

- `adb` in PATH (Android SDK platform-tools)
- Device connected with USB debugging enabled
- App `com.ethiobalance.app` installed on device
- `sqlite3` installed locally

### Usage

```bash
chmod +x scripts/test-workflow.sh
./scripts/test-workflow.sh
```

### What It Does

1. **Resets** app data via `adb shell pm clear` for a clean test
2. **Launches** the app to initialize the Room database
3. **Injects** 16 real-world SMS messages via `SmsForegroundService`
4. **Asserts** that correct transactions appear in the database
5. **Dumps** the DB and logcat for debugging

### Test Matrix

| # | Bank | Type | Amount (ETB) | Scenario |
|---|------|------|-------------|----------|
| 1 | CBE | INCOME | 7,000.00 | Simple credit |
| 2 | CBE | INCOME | 20,000.00 | Credit from named party |
| 3 | CBE | EXPENSE | 5,005.75 | Debit with service charge + VAT |
| 4 | CBE | EXPENSE | 50,011.50 | Large debit with service charge |
| 5 | CBE | EXPENSE | 50,011.50 | Debit for named party |
| 6 | CBE | EXPENSE | 2,005.75 | Debit for utility payment |
| 7 | CBE | INCOME | 60,000.00 | Large credit |
| 8 | BOA | INCOME | 5,000.00 | Cash deposit with party name |
| 9 | BOA | EXPENSE | 50,000.00 | Simple debit |
| 10 | BOA | INCOME | 100,000.00 | Large cash deposit |
| 11 | Awash | INCOME | 1,300.00 | Credit with party name |
| 12 | Awash | INCOME | 50.00 | Small transfer credit |
| 13 | Awash | INCOME | 4,000.00 | Credit |
| 14 | Telebirr | INCOME | 25.00 | Airtime received |
| 15 | Telebirr | EXPENSE | 100.00 | Package purchase |
| 16 | Telebirr | EXPENSE | 450.50 | Transfer/send |

### Adding New Test Cases

1. Capture a real SMS from the device:
   ```bash
   adb shell "content query --uri content://sms/inbox --projection address:body:date --sort 'date DESC'" | head -5
   ```
2. Add an `inject_sms` call with the real sender and body
3. Add a matching `assert_tx` line
4. Run the script to verify

### Troubleshooting

- **All tests fail**: Check logcat for `ReconciliationEngine` — confidence < 0.7 means the parser didn't recognize the SMS format
- **Permission denied**: Run `adb shell pm grant com.ethiobalance.app android.permission.READ_SMS`
- **"already exists"**: The script uses `pm clear` to reset; if you skip reset, duplicate IDs are expected
