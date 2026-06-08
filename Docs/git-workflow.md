# EthioStat Git Workflow

## Branch Strategy (Gitflow)

```
main          ← stable, tagged releases only  (v1.x.x)
  └── develop ← integration branch; all feature PRs merge here first
        ├── feature/<name>   new capabilities
        ├── fix/<name>       bug fixes
        └── chore/<name>     tooling, deps, docs
```

### Branch Rules
- `main` is **protected** — never commit directly; only merge via PR from `develop`
- `develop` is the default working branch — all work branches off here
- Delete feature/fix branches after merge

---

## Commit Convention (`type(scope): message`)

| Type | When to use |
|------|-------------|
| `feat` | New user-visible feature |
| `fix` | Bug fix |
| `chore` | Build, tooling, dependency update |
| `test` | Test additions or changes |
| `docs` | Documentation only |
| `refactor` | Code restructure with no behaviour change |
| `perf` | Performance improvement |

**Examples:**
```
fix(android): stop SmsForegroundService looping — START_NOT_STICKY + stopSelf
fix(parser): add AIRTIME_BALANCE regex to SmsParser for senders 127/804/994
fix(ui): case-insensitive dedup of Telebirr/TELEBIRR in sources list
feat(startup): trigger 7-day SMS scan for all configured sources on launch
fix(scan): use real SMS sender number (senderId) in scanHistory call
feat(parser): create web-side smsParser.ts with parseEthioSMS function
test(sms): add ADB end-to-end workflow test script
```

---

## Day-to-Day Workflow

### Starting a new fix or feature
```bash
git checkout develop
git pull origin develop
git checkout -b fix/your-bug-name    # or feature/your-feature-name
```

### During development
```bash
git add -p                           # stage hunks selectively
git commit -m "fix(scope): message"
```

### Finishing — open a PR to develop
```bash
git push origin fix/your-bug-name
# Open PR on GitHub: fix/your-bug-name → develop
# After review + CI green → Squash and merge
git branch -d fix/your-bug-name
```

### Releasing to main
```bash
git checkout main
git merge --no-ff develop
git tag -a v1.x.x -m "Release v1.x.x"
git push origin main --tags
```

---

## Current Fix Branches (this sprint)

| Branch | Fixes | Status |
|--------|-------|--------|
| `fix/duplicate-telebirr-source` | Case-insensitive source dedup in `App.tsx` | ✅ Merged to develop |
| `fix/sms-balance-parsing` | Airtime ETB regex in `SmsParser.kt` + `BALANCE_QUERY` in `ReconciliationEngine` | ✅ Merged to develop |
| `fix/foreground-service-loop` | `START_NOT_STICKY` + `stopSelf(startId)` in `SmsForegroundService` | ✅ Merged to develop |
| `fix/scan-history-sender-id` | Use `bank.senderId` (real number) in `scanHistory` calls | ✅ Merged to develop |
| `fix/startup-scan` | Trigger 7-day scan for all existing sources on app launch | ✅ Merged to develop |
| `feat/web-sms-parser` | Create `src/data/smsParser.ts` + fix `smsParser.test.ts` | ✅ Merged to develop |
| `feat/test-workflow-script` | `scripts/test-workflow.sh` ADB integration test | ✅ Merged to develop |

---

## CI Checklist (before every PR merge)

- [ ] `./gradlew testDebugUnitTest` — all JVM unit tests pass
- [ ] `./scripts/test-workflow.sh` — all ADB integration assertions pass on a real device
- [ ] `./gradlew assembleDebug` — app builds cleanly with zero errors
- [ ] Manual smoke test: open app, navigate all tabs — Home, Transactions, Telecom, Settings

---

## Tagging Releases

```bash
git tag -a v1.0.0 -m "Initial stable release — SMS pipeline fully operational"
git tag -a v1.1.0 -m "SMS balance parsing, dedup fix, startup scan"
```

Use semantic versioning: `MAJOR.MINOR.PATCH`
- **MAJOR**: breaking change
- **MINOR**: new backward-compatible feature
- **PATCH**: bug fix
