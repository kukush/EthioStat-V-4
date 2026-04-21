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
}
