---
description: Apply coding standards for Kotlin UI components
activation: Model Decision
---

# Kotlin Component Coding Standards

When working on UI files in the `android/app/src/main/java/com/ethiobalance/app/ui` (or `android/app/src/ui`) directory, follow these coding standards for Kotlin UI components:

1. **State Hoisting**: Keep Composables stateless where possible. Hoist state to the caller by passing state values and event callbacks as parameters.
2. **Modifiers**: Always include a `modifier: Modifier = Modifier` parameter as the first optional argument in custom Composable functions to allow for external styling and layout modifications.
3. **Theming**: Use `MaterialTheme` attributes for colors, typography, and shapes. Avoid hardcoded values (like `Color(0xFF...)` or literal `dp`/`sp` sizes) when a theme token is applicable.
4. **Previews**: Provide `@Preview` annotations for all major UI components to ensure they can be visualized independently. Use different configurations (e.g., dark mode, different screen sizes) if necessary.
5. **Naming Conventions**: Use `PascalCase` for Composable functions. Helper non-Composable functions should remain `camelCase`.
6. **Side Effects**: Handle side effects responsibly using `LaunchedEffect`, `DisposableEffect`, or `SideEffect` to avoid triggering multiple unnecessary background tasks on recomposition.
