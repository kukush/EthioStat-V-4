# Troubleshooting Guide

This guide provides solutions to common issues developers may encounter while working
with the EthioStat project.

## Table of Contents

1. [Build Failures](#build-failures)
2. [Database Migration Issues](#database-migration-issues)
3. [Testing Problems](#testing-problems)
4. [Runtime Crashes](#runtime-crashes)
5. [Dependency Conflicts](#dependency-conflicts)

### Build Failures

- **Problem:** Gradle sync fails with `Could not resolve all files for configuration`.
- **Solution:** Ensure you have the correct Android SDK version installed and that the
  `gradle-wrapper.properties` points to a compatible Gradle distribution. Run `./gradlew
clean` and then `./gradlew assembleDebug`.

### Database Migration Issues

- **Problem:** App crashes on startup after a schema change.
- **Solution:** Verify that the migration objects are correctly defined in
  `AppDatabase.kt`. Increment the database version and provide a proper `Migration`
  implementation for each version bump.

### Testing Problems

- **Problem:** Unit tests fail with `NoClassDefFoundError`.
- **Solution:** Make sure the test source sets include the necessary dependencies in
  `build.gradle.kts`. Run `./gradlew testDebugUnitTest` to see detailed output.

### Runtime Crashes

- **Problem:** Crash reports indicate `NullPointerException` in `SmsReceiver`.
- **Solution:** Add null‑checks before accessing nullable properties and ensure the
  receiver is correctly registered in the manifest.

### Dependency Conflicts

- **Problem:** Duplicate classes from different library versions.
- **Solution:** Use Gradle's `resolutionStrategy` to force a single version, e.g.:
  ```kotlin
  configurations.all {
      resolutionStrategy.eachDependency {
          if (requested.group == "org.jetbrains.kotlin") {
              useVersion("1.8.0")
          }
      }
  }
  ```

For any issues not covered here, please open an issue on the repository or consult the
project maintainers.
