# Contributor Guide

This guide expands on the basic Git workflow described in
[`Docs/git-workflow.md`](Docs/git-workflow.md) and provides detailed instructions
for setting up the development environment, running tests, and submitting
contributions.

---

## Table of Contents

1. [Prerequisites](#prerequisites)
2. [Repository Setup](#repository-setup)
3. [Branching Strategy](#branching-strategy)
4. [Commit Guidelines](#commit-guidelines)
5. [Running the Project Locally](#running-the-project-locally)
6. [Testing](#testing)
7. [Continuous Integration](#continuous‑integration)
8. [Submitting a Pull Request](#submitting-a-pull‑request)
9. [Release Process](#release-process)
10. [Code of Conduct](#code-of-conduct)

---

## Prerequisites

- **JDK 21** – required for the Kotlin compiler.
- **Android SDK** (API 24+). Install via Android Studio or command line tools.
- **Kotlin 1.9.24** – the project uses the Kotlin DSL for Gradle.
- **Git** – version control.
- **Node.js** (optional) – for any auxiliary scripts.

## Repository Setup

```bash
git clone https://github.com/your-org/EthioStat-V-4.git
cd EthioStat-V-4
git remote add upstream https://github.com/your-org/EthioStat-V-4.git
```

Configure the Android SDK location if not automatically detected:

```bash
export ANDROID_SDK_ROOT=$HOME/Library/Android/sdk
```

## Branching Strategy

We follow **Gitflow** as outlined in the original workflow file. The main
branches are:

- `main` – protected, contains only released tags.
- `develop` – integration branch where all feature branches are merged.

Create short‑lived feature, fix, or chore branches off `develop`:

```bash
git checkout develop
git pull origin develop
git checkout -b feature/awesome‑feature
```

## Commit Guidelines

Use the conventional commit format `type(scope): message`.

- `feat` – new user‑visible feature
- `fix` – bug fix
- `chore` – tooling, dependencies, docs
- `test` – test additions or changes
- `docs` – documentation only
- `refactor` – code restructure without behaviour change
- `perf` – performance improvement

Example:

```text
feat(ui): add dark‑mode toggle to settings screen
```

## Running the Project Locally

```bash
cd android
./gradlew assembleDebug   # builds the APK
```

To run on an emulator or device:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.ethiobalance.app/.MainActivity
```

## Testing

Unit tests:

```bash
./gradlew testDebugUnitTest
```

Instrumentation / ADB integration tests (requires a connected device):

```bash
./scripts/test-workflow.sh
```

Coverage report (JaCoCo) is generated with the above unit‑test command and can be
opened at `app/build/reports/jacoco/jacocoDebugUnitTestReport/html/index.html`.

## Continuous Integration

The repository is configured with GitHub Actions that run the CI checklist from
the workflow file on every PR. Ensure all checks pass before requesting a merge.

## Submitting a Pull Request

1. Push your branch: `git push origin feature/awesome-feature`.
2. Open a PR on GitHub targeting `develop`.
3. Fill the PR template, linking any related issues.
4. Wait for CI to pass and address reviewer feedback.
5. Once approved, squash‑merge the PR.

## Release Process

1. Create a release branch from `main`.
2. Bump the version in `build.gradle.kts` following semantic versioning.
3. Tag the release: `git tag -a vX.Y.Z -m "Release vX.Y.Z"`.
4. Push tags: `git push origin main --tags`.
5. Draft a GitHub Release notes page.

## Code of Conduct

Please read and follow the [Contributor Covenant Code of Conduct]
(https://www.contributor-covenant.org/version/2/1/code_of_conduct/).
