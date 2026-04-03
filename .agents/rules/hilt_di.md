---
description: Enforce Dependency Injection best practices using Hilt
activation: *Module.kt, App.kt, @Inject, @HiltViewModel, @AndroidEntryPoint
---

# Hilt Dependency Injection Standards

When working with Dependency Injection (DI) and Hilt, adhere to the following rules:

1. **Constructor Injection**: Prefer constructor injection `class MyClass @Inject constructor(...)` for Repositories, Use Cases, and Utilities over field injection whenever possible.
2. **ViewModels**: Always annotate ViewModels with `@HiltViewModel` and use constructor injection for their dependencies.
3. **Component Scoping**: Carefully choose your Hilt components. Use `@InstallIn(SingletonComponent::class)` for app-wide singletons (e.g., Room databases, network clients) and restrict narrower scopes (like `ViewModelComponent` or `ActivityComponent`) to dependencies that shouldn't outlive their hosts.
4. **Bindings**: When injecting interfaces, prefer using `@Binds` in an `abstract class` module over `@Provides` for better performance and less boilerplate, unless you require logic to instantiate the dependency.
5. **Entry Points**: Use `@AndroidEntryPoint` on any Activity or Fragment that requires injection. Ensure the Application class is annotated with `@HiltAndroidApp`.
