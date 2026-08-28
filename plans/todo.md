# Plan to Improve EthioStat Project

## I. Code Quality and Maintenance

- [ ] Implement static analysis tools (e.g., Detekt, Android Lint) into the CI/CD pipeline to automatically identify unused variables, code smells, and potential bugs.
- [ ] Establish a process for regularly reviewing and addressing issues reported by static analysis tools.
- [ ] Define a clear bug reporting and tracking process.
- [ ] Integrate a crash reporting tool (e.g., Firebase Crashlytics) for production environments.

## II. Testing and Quality Assurance

- [x] Comprehensive Unit & Integration test suite established with Vitest & React Testing Library (11 test suites, 49 passing tests).
- [x] Unit test coverage for storage persistence, SMS parser regex, translations, USSD dialogs, and simulated SMS ingestion.
- [x] Integration test coverage for dual-SIM switching, balance synchronization (*804#), transaction filtering, and localization switching.
- [x] Expanded edge cases and failure scenarios across Ethiopian financial and telecom sources.

## III. Documentation Enhancements

- [x] Update `README.md` and `CHANGELOG.md` with layout improvements and test suite execution guides.
- [x] Maintained `troubleshooting-guide.md` in the `Docs/` directory for common build and migration issues.
- [x] Maintained `contributor-guide.md` covering workflow, architecture, testing, and contribution standards.

