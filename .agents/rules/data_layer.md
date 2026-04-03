---
description: Enforce Room best practices for the Data Layer
activation: Files in data/ or matching *Entity.kt and *Dao.kt
---

# Data Layer Coding Standards

When working on files in the data layer (such as DAOs and Entities), enforce the following best practices:

1. **Reactive Updates**: Use Kotlin `Flow` for queries that need to return reactive streams for continuous updates.
2. **One-Shot Writes**: Use `suspend` functions for one-shot database operations like inserts, updates, and deletes.
3. **Main-Safety**: Rely on Room's built-in main-safety for `suspend` functions and `Flow`. If custom data manipulations are necessary in repositories, explicitly use `Dispatchers.IO`.
