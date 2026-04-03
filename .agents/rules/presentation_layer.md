---
description: Enforce Material 3 and lifecycle-aware state collection in Compose
activation: @Composable functions
---

# Presentation Layer Coding Standards

When working with `@Composable` functions, enforce the following standards:

1. **State Hoisting**: Favor stateless Composables. Hoist state up to the caller (or ViewModel) and pass data down along with event callbacks.
2. **Material 3 APIs**: Utilize Material 3 APIs and components for modern styling and structure.
3. **State Collection**: Strictly collect state from ViewModels using `collectAsStateWithLifecycle()` to ensure UI state subscriptions are aware of the Android activity lifecycle, preventing background resource leaks.
4. **Granular Arguments**: Pass only the necessary primitives or data classes to low-level composables instead of whole ViewModels.
