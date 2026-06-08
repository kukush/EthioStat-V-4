package com.ethiobalance.app.domain.model

enum class TelecomPackageType(val label: String) {
    NIGHT_VOICE("Night Voice"),
    RECURRING_VOICE("Recurring Voice"),
    BONUS_VOICE("Bonus Voice"),
    DATA("Internet Data"),
    SMS("SMS"),
    BONUS_FUND("Bonus Fund"),
    UNKNOWN("Package");

    companion object {
        fun classify(segmentText: String, type: String): TelecomPackageType {
            val lower = segmentText.lowercase()
            return when {
                type == "sms" -> SMS
                type == "bonus" -> BONUS_FUND
                type == "internet" -> DATA
                type == "voice" && (lower.contains("free") || lower.contains("promo")) -> BONUS_VOICE
                type == "voice" && lower.contains("night") -> {
                    // Distinguish night vs recurring within same "night package bonus" plan:
                    // The night portion has remaining <= 63 (the night quota)
                    // but we classify by segment position — first segment = night, second = recurring
                    // Caller should use classifyVoicePair() for same-name pairs instead
                    NIGHT_VOICE
                }
                type == "voice" && lower.contains("recurring") -> RECURRING_VOICE
                type == "voice" -> BONUS_VOICE
                else -> UNKNOWN
            }
        }

        /**
         * For EthioTelecom's combined "125 Min and 63Min night package bonus" plan,
         * the SMS lists two segments with identical names but different remaining amounts.
         * The segment with remaining <= night quota (63) is NIGHT, the other is RECURRING.
         */
        fun classifyVoicePair(remaining: Double, nightQuota: Double = 63.0): TelecomPackageType {
            return if (remaining <= nightQuota) NIGHT_VOICE else RECURRING_VOICE
        }
    }
}
