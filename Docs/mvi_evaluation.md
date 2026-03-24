# MVI Architecture Evaluation & Proposal

This document evaluates the proposed **Model-View-Intent (MVI)** architecture plan and its application to the **EthioStat** project.

## 1. Current Project vs. Proposed MVI Plan

The proposed MVI plan is highly structured for **Native Android** (Jetpack Compose, Room, ViewModel). The current project is a **Hybrid React/Capacitor** application. 

### Comparison Table

| Component | Proposed (Native) | Current (Hybrid React) |
| :--- | :--- | :--- |
| **View** | Jetpack Compose | React Components (`src/screens/*.tsx`) |
| **Intent** | Sealed Classes (`DashboardIntent`) | TypeScript Union Types (`Intent` in `src/store.ts`) |
| **ViewModel** | Android ViewModel | `useReducer` and the `reducer` function in `src/store.ts` |
| **Use Cases** | Dedicated Logic Classes | Logic resides in `reducer` or direct service calls |
| **Repository** | Single Source of Truth | `persistenceService.ts` and `smsParser.ts` |
| **State** | Data Classes / StateFlow | `AppState` in `src/store.ts` / Central Store |
| **Storage** | Room (SQLite) | `localStorage` (Persisted via Capacitor) |

## 2. Evaluation

The proposed plan is **excellent** and conceptually aligns with the project's current Redux-like flow. However, to fully implement the **Native** architecture as described, a full rewrite of the UI (moving from React to Jetpack Compose) and Data layer (moving to Room) would be required.

### Strengths of the MVI Plan
- **Unidirectional Data Flow**: Prevents state inconsistencies.
- **Testability**: Use cases and repositories are decoupled from the UI.
- **Predictability**: Intents clearly define all possible user actions.

## 3. Proposed Solution: "Web-MVI" Alignment

Instead of a full native rewrite (unless intended), we can "fix" the current React structure to mirror the professional MVI pattern by adding the missing abstraction layers (**Use Cases** and **Repositories**).

### A. Extracting Use Cases
Move complex logic out of the `reducer` and into dedicated "Use Case" functions/classes.
- `ParseSmsUseCase.ts`
- `SyncBalanceUseCase.ts`
- `GetFinancialSummaryUseCase.ts`

### B. Implementing the Repository Pattern
Create a `Repository` layer that abstracts data sources (`persistenceService`, `smsParser`, and future Native SMS APIs).

### C. Folder Structure Refactor
```text
src/
  ├── domain/           (State, Intent types, Use Cases)
  ├── data/             (Repositories, API, Storage)
  ├── presentation/     (React Screens, Components)
  └── store/            (The Redux/MVI Glue)
```

## 4. Conclusion & Recommendation

1. **Keep the Hybrid Stack**: If speed to market and cross-platform (iOS/Android) support is a priority, continue with the React stack but refactor it using the "Web-MVI" layers (Use Cases & Repositories).
2. **Native Migration**: If maximum performance and deep Android integration (e.g., advanced background SMS processing) is required, follow the MVI draft exactly and begin a migration to Jetpack Compose.

---
**Recommendation**: Start by refactoring the `src/store.ts` logic into **domain/useCases** to improve testability without necessitating a full platform rewrite.
