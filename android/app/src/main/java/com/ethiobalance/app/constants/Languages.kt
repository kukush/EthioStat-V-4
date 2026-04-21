package com.ethiobalance.app.constants

/**
 * Supported application languages.
 * Centralized to avoid hardcoding language lists in UI components.
 */
object Languages {
    data class LanguageInfo(val code: String, val displayName: String)

    val SUPPORTED = listOf(
        LanguageInfo("en", "English"),
        LanguageInfo("am", "አማርኛ"),
        LanguageInfo("om", "Afaan Oromoo")
    )

    val DEFAULT = "en"

    fun getDisplayName(code: String): String {
        return SUPPORTED.find { it.code == code }?.displayName ?: code
    }
}
