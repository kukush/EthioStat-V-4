# Plan to Improve EthioStat Project

## I. Code Quality and Maintenance

- [ ] Implement static analysis tools (e.g., Detekt, Android Lint) into the CI/CD pipeline to automatically identify unused variables, code smells, and potential bugs.
- [ ] Establish a process for regularly reviewing and addressing issues reported by static analysis tools.
- [ ] Define a clear bug reporting and tracking process.
- [ ] Integrate a crash reporting tool (e.g., Firebase Crashlytics) for production environments.

## II. Testing and Quality Assurance

- [ ] Integrate a code coverage tool (e.g., JaCoCo) to measure and track test coverage metrics.
- [ ] Increase unit test coverage for `repository`, `data`, and `other view models`.
- [ ] Develop UI/Instrumentation tests for all critical user flows and screens using Espresso or Compose testing frameworks.
- [ ] Expand existing integration tests (`scripts/test-workflow.sh`) to cover more edge cases and failure scenarios.
- [ ] Parameterize existing unit tests (e.g., `ParseSmsUseCaseTest`) with diverse inputs.

## III. Documentation Enhancements

- [ ] Generate KDoc for all public classes, functions, and properties in the `domain` and `data` layers.
- [ ] Update `README.md` to provide a high-level overview and links to all detailed documentation files.
- [ ] Create a `troubleshooting-guide.md` in the `Docs/` directory for common issues.
- [ ] Enhance `Docs/git-workflow.md` into a comprehensive `contributor-guide.md` covering setup, testing, and submission.
- [ ] Discuss the need for a user manual or FAQ.
