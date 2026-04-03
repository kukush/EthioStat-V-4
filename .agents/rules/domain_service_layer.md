---
description: Enforce Clean Architecture rules for Domain and Service layers
activation: *UseCase.kt, *Service.kt, or files in domain/ and services/ directories
---

# Domain & Service Layer Coding Standards

When working on files in the **Domain** (Use Cases, Domain Models) or **Service** (Background Services, Parsers) layers, enforce the following standards:

1. **Single Responsibility**: Each Use Case or Service class should encapsulate one specific piece of business logic or background task.
2. **Invoke Operator**: For Use Cases, implement the logic inside an `operator fun invoke()` function to make instances callable like functions.
3. **Pure Kotlin**: The Domain layer should be completely devoid of Android UI or framework dependencies. It should represent pure, testable Kotlin business logic.
4. **Dependency Direction**: The Domain layer should define interfaces that the Data layer implements. Repositories and Services should be injected into Use Cases, never the reverse.
5. **Services**: For Android Services (like Accessibility or SMS parsing), keep the entry points minimal and delegate the heavy lifting to inject-able domain components.
