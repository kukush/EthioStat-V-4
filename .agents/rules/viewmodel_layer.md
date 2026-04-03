---
description: Enforce State management and clean architecture for ViewModels
activation: *ViewModel.kt
---

# ViewModel Layer Coding Standards

When working on `ViewModel` classes, enforce the following rules:

1. **State Management**: Use `StateFlow` (with a private backing `MutableStateFlow`) for managing and exposing UI state.
2. **Coroutines**: Always use `viewModelScope` to launch coroutines tied to the ViewModel's lifecycle.
3. **Clean Architecture**: Absolutely NO UI-specific code, Android View references, or Compose specific packages should be imported into or used within this layer. Keep it strictly focused on business logic and state preparation.
4. **Immutability**: Expose read-only state to the UI to maintain unidirectional data flow.
