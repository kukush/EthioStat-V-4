package com.ethiobalance.app.constants

/**
 * Phone number formatting constants for Ethiopian numbers.
 */
object PhoneConstants {
    const val COUNTRY_CODE = "+251"
    const val COUNTRY_CODE_INT = 251
    const val FLAG_EMOJI = "🇪🇹"

    // Maximum length for Ethiopian phone number (without country code)
    const val MAX_LOCAL_LENGTH = 9

    // Maximum length with country code
    const val MAX_FULL_LENGTH = 13 // +251 + 9 digits

    // Local prefix that gets replaced with country code
    const val LOCAL_PREFIX = "0"

    // Regex for Ethiopian phone number validation: must start with 7 or 9, exactly 9 digits
    val PHONE_REGEX = Regex("^[79]\\d{8}$")

    /**
     * Validates if a phone number is a valid Ethiopian phone number.
     * Must start with 7 or 9 and be exactly 9 digits.
     */
    fun isValidEthiopianPhone(phone: String): Boolean {
        return PHONE_REGEX.matches(phone)
    }
}
