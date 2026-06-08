---
description: Generates a full Clean Architecture slice for a new feature.
---

# Workflow: Generate New Feature or new task 

**Command:** `/new-feature [FeatureName]`

## Steps
1. **Data Layer**: Create `data/entity/[FeatureName]Entity.kt` and `data/dao/[FeatureName]Dao.kt` inside the `android/app/src/main/java/com/ethiobalance/app/data/` or relevant package.
   - Use Room annotations.
   - Include standard CRUD operations.
2. **Domain Layer**: Create `domain/usecase/Get[FeatureName]UseCase.kt` inside `android/app/src/main/java/com/ethiobalance/app/domain/`.
   - Implement as a simple `invoke` operator function.
3. **State Layer**: Create `ui/viewmodel/[FeatureName]ViewModel.kt` inside `android/app/src/main/java/com/ethiobalance/app/ui/viewmodel/`.
   - Use `StateFlow` for UI state.
   - Inject the Use Case via Hilt.
4. **UI Layer**: Create `ui/screens/[FeatureName]Screen.kt` inside `android/app/src/main/java/com/ethiobalance/app/ui/screens/`.
   - Create a stateless `@Composable`.
   - Include a `@Preview`.
5. **DI Layer**: Update or create `di/[FeatureName]Module.kt` inside `android/app/src/main/java/com/ethiobalance/app/di/` to provide the DAO and Repository.

## Constraints
- Apply all rules from `.agents/rules/*.md`.
- Use the package name: `com.ethiobalance.app`
