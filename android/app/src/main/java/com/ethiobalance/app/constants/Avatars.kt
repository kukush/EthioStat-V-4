package com.ethiobalance.app.constants

/**
 * Avatar options for user profile selection.
 * Centralized to avoid hardcoding emoji lists in UI components.
 */
object Avatars {
    val OPTIONS = listOf(
        "👤", "🧑", "👩", "👨", "🧔", "👵", "🧑‍💼", "👩‍💻", "👨‍🎓",
        "🦁", "🐯", "🦊", "🐻", "🐼", "🐨", "🐸", "🦉", "🐝"
    )

    val DEFAULT = "👤"
}
