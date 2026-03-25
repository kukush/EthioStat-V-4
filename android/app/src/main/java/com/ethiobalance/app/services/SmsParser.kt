package com.ethiobalance.app.services

import com.ethiobalance.app.data.BalancePackageEntity
import java.util.UUID

enum class SmsScenario {
    SELF_PURCHASE,
    GIFT_SENT,
    RECHARGE_OR_GIFT_RECEIVED,
    LOAN_TAKEN,
    UNKNOWN
}

data class ParsedSmsResult(
    val scenario: SmsScenario,
    val confidence: Float,
    val packages: MutableList<BalancePackageEntity> = mutableListOf(),
    val deductedAmount: Double? = null,
    val addedAmount: Double? = null,
    val isRecharge: Boolean = false
)

object SmsParser {

    private fun detectLanguage(text: String): String {
        val amharicRegex = Regex("[\\u1200-\\u137F]")
        if (amharicRegex.containsMatchIn(text)) return "am"
        
        val oromoKeywords = listOf("haala", "galii", "herrega", "kennamee", "fudhattaniirtu")
        val lowerText = text.lowercase()
        if (oromoKeywords.any { lowerText.contains(it) }) return "om"
        
        return "en"
    }

    fun parse(sender: String, body: String): ParsedSmsResult {
        val lang = detectLanguage(body)
        val result = ParsedSmsResult(scenario = SmsScenario.UNKNOWN, confidence = 0f)
        var confidenceScore = 0f
        var scenario = SmsScenario.UNKNOWN
        var deductedAmount: Double? = null
        var addedAmount: Double? = null
        var isRecharge = false
        val now = System.currentTimeMillis()

        // 1. Package Regexes (Asset Gain)
        val dataMatch = Regex("([\\d,.]+)\\s*(MB|GB)\\s*(?:data|remaining|ኢንተርኔት|Intarneetii)", RegexOption.IGNORE_CASE).find(body)
        if (dataMatch != null) {
            val amount = dataMatch.groupValues[1].replace(",", "").toDoubleOrNull() ?: 0.0
            val unit = dataMatch.groupValues[2].uppercase()
            result.packages.add(BalancePackageEntity(
                id = "data-${UUID.randomUUID()}",
                simId = "sim1",
                type = "DATA_AIRTIME",
                totalAmount = amount,
                remainingAmount = amount,
                unit = unit,
                expiryDate = now + (24 * 60 * 60 * 1000L), // mock 1 day
                isActive = true,
                source = "SMS",
                lastUpdated = now
            ))
            confidenceScore = 0.8f
        }

        val voiceMatch = Regex("([\\d,.]+)\\s*(?:Min|ደቂቃ|Daqiiqaa)", RegexOption.IGNORE_CASE).find(body)
        if (voiceMatch != null) {
            val amount = voiceMatch.groupValues[1].replace(",", "").toDoubleOrNull() ?: 0.0
            result.packages.add(BalancePackageEntity(
                id = "voice-${UUID.randomUUID()}",
                simId = "sim1",
                type = "VOICE",
                totalAmount = amount,
                remainingAmount = amount,
                unit = "MIN",
                expiryDate = now + (24 * 60 * 60 * 1000L),
                isActive = true,
                source = "SMS",
                lastUpdated = now
            ))
            confidenceScore = 0.8f
        }

        val smsMatch = Regex("([\\d,.]+)\\s*(?:SMS|ኤስኤምኤስ)", RegexOption.IGNORE_CASE).find(body)
        if (smsMatch != null) {
            val amount = smsMatch.groupValues[1].replace(",", "").toDoubleOrNull() ?: 0.0
            result.packages.add(BalancePackageEntity(
                id = "sms-${UUID.randomUUID()}",
                simId = "sim1",
                type = "SMS",
                totalAmount = amount,
                remainingAmount = amount,
                unit = "SMS",
                expiryDate = now + (24 * 60 * 60 * 1000L),
                isActive = true,
                source = "SMS",
                lastUpdated = now
            ))
            confidenceScore = 0.8f
        }

        val isGiftReceived = body.lowercase().contains("gift") || body.lowercase().contains("received a gift")

        // 2. Financial Transaction Regexes (Expenses / Incomes)
        // Telebirr, EthioTelecom, or USSD interceptions
        if (sender.contains("TELEBIRR") || listOf("830", "806", "999", "810", "USSD").contains(sender)) {
            
            // Loan taken
            val loanMatch = Regex("(?:loan of|taken a loan of|received a loan of)\\s*([\\d,.]+)\\s*(?:ETB|ብር)?", RegexOption.IGNORE_CASE).find(body)
            if (loanMatch != null) {
                scenario = SmsScenario.LOAN_TAKEN
                addedAmount = loanMatch.groupValues[1].replace(",", "").toDoubleOrNull()
                confidenceScore = 0.9f
            }

            // Loan repayment
            val repayMatch = Regex("(?:repaid|repayment of|loan of.*has been repaid)\\s*([\\d,.]+)\\s*(?:ETB|ብር)?", RegexOption.IGNORE_CASE).find(body)
            if (repayMatch != null) {
                scenario = SmsScenario.SELF_PURCHASE // Expense
                deductedAmount = repayMatch.groupValues[1].replace(",", "").toDoubleOrNull()
                confidenceScore = 0.9f
            }

            // Payment / Purchase
            val paymentMatch = Regex("(?:paid|pay|payment|purchase)\\s*([\\d,.]+)\\s*(?:ETB|ብር)?", RegexOption.IGNORE_CASE).find(body)
            if (paymentMatch != null && loanMatch == null) {
                scenario = if (isGiftReceived) SmsScenario.RECHARGE_OR_GIFT_RECEIVED else SmsScenario.SELF_PURCHASE
                deductedAmount = paymentMatch.groupValues[1].replace(",", "").toDoubleOrNull()
                confidenceScore = 0.9f
            }

            // Transfer / Gift Sent
            val transferMatch = Regex("(?:transferred|gifted)\\s*([\\d,.]+)\\s*(?:ETB|ብር)?", RegexOption.IGNORE_CASE).find(body)
            if (transferMatch != null) {
                scenario = SmsScenario.GIFT_SENT
                deductedAmount = transferMatch.groupValues[1].replace(",", "").toDoubleOrNull()
                confidenceScore = 0.9f
            }
            
            // Recharges (Voucher/Electronic/USSD)
            val rechargeMatch = Regex("(?:recharged|topup|top-up|ሞልተዋል).*?([\\d,.]+)\\s*(?:ETB|ብር)", RegexOption.IGNORE_CASE).find(body)
            if (rechargeMatch != null) {
                scenario = SmsScenario.RECHARGE_OR_GIFT_RECEIVED
                isRecharge = true
                addedAmount = rechargeMatch.groupValues[1].replace(",", "").toDoubleOrNull()
                confidenceScore = 0.9f
            }
        }  

        // Inference logic if scenario is still UNKNOWN but packages exist
        if (scenario == SmsScenario.UNKNOWN && result.packages.isNotEmpty()) {
            if (isGiftReceived) {
                scenario = SmsScenario.RECHARGE_OR_GIFT_RECEIVED
            } else {
                // Determine if it's just a balance query vs a package update.
                // If it's *804# query from 251994, it might just be the current state (SELF_PURCHASE handles FULL_BALANCE properly in our engine)
                scenario = SmsScenario.SELF_PURCHASE
            }
        }

        return result.copy(
            scenario = scenario,
            confidence = confidenceScore,
            deductedAmount = deductedAmount,
            addedAmount = addedAmount,
            isRecharge = isRecharge
        )
    }
}
