package com.ethiobalance.app.services

import com.ethiobalance.app.AppConstants
import com.ethiobalance.app.data.BalancePackageEntity
import java.util.UUID

enum class SmsScenario {
    SELF_PURCHASE,
    EXPENSE,
    GIFT_SENT,
    RECHARGE_OR_GIFT_RECEIVED,
    LOAN_TAKEN,
    INCOME,
    BALANCE_UPDATE,
    BALANCE_QUERY,
    UNKNOWN
}

data class ParsedSmsResult(
    val scenario: SmsScenario,
    val confidence: Float,
    val packages: MutableList<BalancePackageEntity> = mutableListOf(),
    val deductedAmount: Double? = null,
    val addedAmount: Double? = null,
    val isRecharge: Boolean = false,
    val airtimeBalance: Double? = null,
    val transactionCategory: String? = null
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

    fun parse(sender: String, body: String, timestamp: Long): ParsedSmsResult {
        detectLanguage(body)
        val result = ParsedSmsResult(scenario = SmsScenario.UNKNOWN, confidence = 0f)
        var confidenceScore = 0f
        var scenario = SmsScenario.UNKNOWN
        var deductedAmount: Double? = null
        var addedAmount: Double? = null
        var isRecharge = false
        var transactionCategory: String? = null
        val now = System.currentTimeMillis()
        
        // Generate a deterministic base ID so historical rescans don't duplicate entities
        val uniqueStr = "$sender-$timestamp-${body.hashCode()}"
        val baseId = UUID.nameUUIDFromBytes(uniqueStr.toByteArray()).toString()

        // ── Helper: parse "with expiry date on YYYY-MM-DD" → epoch ms
        fun parseExpiryMs(seg: String): Long {
            val m = Regex("""with expiry date on (\d{4})-(\d{2})-(\d{2})""", RegexOption.IGNORE_CASE).find(seg)
            return if (m != null) {
                try {
                    val cal = java.util.Calendar.getInstance()
                    cal.set(m.groupValues[1].toInt(), m.groupValues[2].toInt() - 1, m.groupValues[3].toInt(), 23, 59, 59)
                    cal.timeInMillis
                } catch (e: Exception) { now + (7 * 24 * 60 * 60 * 1000L) }
            } else { now + (7 * 24 * 60 * 60 * 1000L) }
        }

        // 1. Package Regexes (Asset Gain)
        // Detect multi-segment remaining-balance format used by Telebirr package status SMS.
        // Separator can be ";  from " (2+ spaces), so use regex instead of literal string match.
        val isMultiSegment = Regex(";\\s+from ", RegexOption.IGNORE_CASE).containsMatchIn(body) &&
            body.contains("expiry date on", ignoreCase = true)

        // C7: Adds or replaces a package using "largest total wins" strategy.
        // Prevents a bonus/partial segment from overwriting a main package of the same type.
        fun addOrReplace(pkg: BalancePackageEntity) {
            val existingIdx = result.packages.indexOfFirst { it.type == pkg.type }
            when {
                existingIdx < 0 -> result.packages.add(pkg)
                pkg.totalAmount > result.packages[existingIdx].totalAmount -> result.packages[existingIdx] = pkg
                // else: keep existing (first seen wins when totals are equal)
            }
        }

        if (isMultiSegment) {
            val segments = body.split(Regex(";\\s*"))
            segments.forEachIndexed { segIdx, seg ->
                val segLower = seg.lowercase()
                val expiryMs = parseExpiryMs(seg)
                val segId = "$baseId-seg$segIdx"

                when {
                    // Voice segment — contains "min" (case-insensitive) or "voice"
                    segLower.contains("min") || segLower.contains("voice") || segLower.contains("ደቂቃ") -> {
                        // Total: first "X Min" BEFORE the word "is " (the original package size)
                        val isIdx = seg.indexOf("is ", ignoreCase = true)
                        val beforeIs = if (isIdx > 0) seg.substring(0, isIdx) else seg
                        val totalMatch = Regex("""(\d[\d,.]*)\s*Min(?!ute)""", RegexOption.IGNORE_CASE).find(beforeIs)
                        val totalVal = totalMatch?.groupValues?.get(1)?.replace(",","")?.toDoubleOrNull() ?: 0.0
                        // Remaining: "is X minute" or "is X Min"
                        val remainMatch = Regex("""is\s+(\d[\d,.]*)\s*(?:Min(?:ute)?|ደቂቃ)""", RegexOption.IGNORE_CASE).find(seg)
                        val remainVal = remainMatch?.groupValues?.get(1)?.replace(",","")?.toDoubleOrNull() ?: totalVal
                        if (remainVal > 0) {
                            addOrReplace(BalancePackageEntity(
                                id = "voice-sim1", simId = "sim1", type = "voice",
                                totalAmount = if (totalVal > 0) totalVal else remainVal,
                                remainingAmount = remainVal, unit = "MIN",
                                expiryDate = expiryMs, isActive = true, source = "SMS", lastUpdated = now
                            ))
                            confidenceScore = 0.9f
                        }
                    }
                    // Data/Internet segment
                    segLower.contains("mb") || segLower.contains("gb") || segLower.contains("internet") -> {
                        // Total: first "X GB" or "X MB" before "is "
                        val isIdx = seg.indexOf("is ", ignoreCase = true)
                        val beforeIs = if (isIdx > 0) seg.substring(0, isIdx) else seg
                        val totalGb = Regex("""(\d[\d,.]*)\s*GB""", RegexOption.IGNORE_CASE).find(beforeIs)
                        val totalMb = Regex("""(\d[\d,.]*)\s*MB""", RegexOption.IGNORE_CASE).find(beforeIs)
                        val totalValMB: Double = when {
                            totalGb != null -> (totalGb.groupValues[1].replace(",","").toDoubleOrNull() ?: 0.0) * 1024.0
                            totalMb != null -> totalMb.groupValues[1].replace(",","").toDoubleOrNull() ?: 0.0
                            else -> 0.0
                        }
                        // Remaining: "is X MB" or "is X GB"
                        val remainGb = Regex("""is\s+(\d[\d,.]*)\s*GB""", RegexOption.IGNORE_CASE).find(seg)
                        val remainMb = Regex("""is\s+(\d[\d,.]*)\s*MB""", RegexOption.IGNORE_CASE).find(seg)
                        val remainVal: Double = when {
                            remainGb != null -> (remainGb.groupValues[1].replace(",","").toDoubleOrNull() ?: 0.0) * 1024.0
                            remainMb != null -> remainMb.groupValues[1].replace(",","").toDoubleOrNull() ?: 0.0
                            else -> totalValMB
                        }
                        if (remainVal > 0) {
                            addOrReplace(BalancePackageEntity(
                                id = "internet-sim1", simId = "sim1", type = "internet",
                                totalAmount = if (totalValMB > 0) totalValMB else remainVal,
                                remainingAmount = remainVal, unit = "MB",
                                expiryDate = expiryMs, isActive = true, source = "SMS", lastUpdated = now
                            ))
                            confidenceScore = 0.9f
                        }
                    }
                    // SMS segment
                    segLower.contains("sms") || segLower.contains("ኤስኤምኤስ") -> {
                        val remainSms = Regex("""is\s+(\d[\d,.]*)(\s*(?:SMS|ኤስኤምኤስ))""", RegexOption.IGNORE_CASE).find(seg)
                            ?: Regex("""(\d[\d,.]*)(\s*(?:SMS|ኤስኤምኤስ))""", RegexOption.IGNORE_CASE).find(seg)
                        val remainVal = remainSms?.groupValues?.get(1)?.replace(",","")?.toDoubleOrNull() ?: 0.0
                        if (remainVal > 0) {
                            addOrReplace(BalancePackageEntity(
                                id = "sms-sim1", simId = "sim1", type = "sms",
                                totalAmount = remainVal, remainingAmount = remainVal, unit = "SMS",
                                expiryDate = expiryMs, isActive = true, source = "SMS", lastUpdated = now
                            ))
                            confidenceScore = 0.9f
                        }
                    }
                    // Bonus Fund segment — "Bonus Fund is 7.50 Birr"
                    segLower.contains("bonus") -> {
                        val bonusMatch = Regex("""is\s+(\d[\d,.]*)\s*(?:Birr|ETB|ብር)""", RegexOption.IGNORE_CASE).find(seg)
                        val bonusVal = bonusMatch?.groupValues?.get(1)?.replace(",","")?.toDoubleOrNull() ?: 0.0
                        if (bonusVal > 0) {
                            addOrReplace(BalancePackageEntity(
                                id = "bonus-sim1", simId = "sim1", type = "bonus",
                                totalAmount = bonusVal, remainingAmount = bonusVal, unit = "ETB",
                                expiryDate = expiryMs, isActive = true, source = "SMS", lastUpdated = now
                            ))
                            confidenceScore = 0.9f
                        }
                    }
                }
            }
        } else {
            // Single-match fallback for simple purchase/gift/recharge messages
            val dataMatch = Regex("([\\d,.]+)\\s*(MB|GB)(?:\\s+\\w+)*\\s*(?:data|internet|remaining|ኢንተርኔት|Intarneetii)", RegexOption.IGNORE_CASE).find(body)
            if (dataMatch != null) {
                val amount = dataMatch.groupValues[1].replace(",", "").toDoubleOrNull() ?: 0.0
                val unit = dataMatch.groupValues[2].uppercase()
                result.packages.add(BalancePackageEntity(
                    id = "internet-sim1", simId = "sim1", type = "internet",
                    totalAmount = amount, remainingAmount = amount, unit = unit,
                    expiryDate = now + (24 * 60 * 60 * 1000L), isActive = true, source = "SMS", lastUpdated = now
                ))
                confidenceScore = 0.8f
            }

            val voiceMatch = Regex("([\\d,.]+)\\s*(?:Min(?:ute)?|ደቂቃ|Daqiiqaa)", RegexOption.IGNORE_CASE).find(body)
            if (voiceMatch != null) {
                val amount = voiceMatch.groupValues[1].replace(",", "").toDoubleOrNull() ?: 0.0
                result.packages.add(BalancePackageEntity(
                    id = "voice-sim1", simId = "sim1", type = "voice",
                    totalAmount = amount, remainingAmount = amount, unit = "MIN",
                    expiryDate = now + (24 * 60 * 60 * 1000L), isActive = true, source = "SMS", lastUpdated = now
                ))
                confidenceScore = 0.8f
            }

            val smsMatch = Regex("([\\d,.]+)\\s*(?:SMS|ኤስኤምኤስ)", RegexOption.IGNORE_CASE).find(body)
            if (smsMatch != null) {
                val amount = smsMatch.groupValues[1].replace(",", "").toDoubleOrNull() ?: 0.0
                result.packages.add(BalancePackageEntity(
                    id = "sms-sim1", simId = "sim1", type = "sms",
                    totalAmount = amount, remainingAmount = amount, unit = "SMS",
                    expiryDate = now + (24 * 60 * 60 * 1000L), isActive = true, source = "SMS", lastUpdated = now
                ))
                confidenceScore = 0.8f
            }
        }

        // Standalone bonus message: "awarded an ETB 7.50 bonus" or "Bonus Fund is 7.50 Birr"
        if (!isMultiSegment) {
            val bonusMatch = Regex("""(?:awarded\s+(?:an\s+)?(?:ETB\s*)?(\d[\d,.]*)\s*(?:bonus)|Bonus\s+Fund\s+is\s+(\d[\d,.]*)\s*(?:Birr|ETB|ብር))""", RegexOption.IGNORE_CASE).find(body)
            if (bonusMatch != null) {
                val bonusVal = (bonusMatch.groupValues[1].takeIf { it.isNotEmpty() } ?: bonusMatch.groupValues[2])
                    .replace(",", "").toDoubleOrNull() ?: 0.0
                if (bonusVal > 0) {
                    addOrReplace(BalancePackageEntity(
                        id = "bonus-sim1", simId = "sim1", type = "bonus",
                        totalAmount = bonusVal, remainingAmount = bonusVal, unit = "ETB",
                        expiryDate = now + (30 * 24 * 60 * 60 * 1000L), isActive = true, source = "SMS", lastUpdated = now
                    ))
                    confidenceScore = 0.85f
                }
            }
        }

        val isGiftReceived = body.lowercase().contains("gift") || body.lowercase().contains("received a gift")

        // 2. Airtime Balance Regex — senders 127 (Telebirr), 804, 251994, 994
        // Covers: "Your balance is 145.50 ETB", "balance: 10.00 ETB", "ቀሪ ሒሳብ 50.00 ብር", Telebirr balance
        val ethioTelecomBalanceSenders = setOf("127", "804", "994", "251994", "8994", "810")
        val isBalanceSender = ethioTelecomBalanceSenders.contains(sender) ||
            sender.contains("TELEBIRR", ignoreCase = true)
        var airtimeBalance: Double? = null
        if (isBalanceSender) {
            val balanceMatch = Regex(
                """(?:your\s+(?:telebirr\s+)?(?:account\s+)?(?:new\s+)?balance\s+(?:after\s+\S+\s+)?(?:is|:)|(?:new\s+)?balance[:\s]+|ቀሪ\s*(?:ሒሳ\S*|ብዛ)?|current\s+balance)[\s:]*(?:ETB\s*)?([\d,]+\.?\d*)\s*(?:ETB|ብር)?""",
                RegexOption.IGNORE_CASE
            ).find(body)
            if (balanceMatch != null) {
                airtimeBalance = balanceMatch.groupValues[1].replace(",", "").toDoubleOrNull()
                if (airtimeBalance != null) {
                    result.packages.add(BalancePackageEntity(
                        id = "airtime-sim1",
                        simId = "sim1",
                        type = "airtime",
                        // canonical ID ensures upsert replaces the single row
                        totalAmount = airtimeBalance,
                        remainingAmount = airtimeBalance,
                        unit = "ETB",
                        expiryDate = now + (30 * 24 * 60 * 60 * 1000L),
                        isActive = true,
                        source = "SMS",
                        lastUpdated = now
                    ))
                    scenario = SmsScenario.BALANCE_QUERY
                    confidenceScore = 0.85f
                }
            }
        }

        // 3. Financial Transaction Regexes (Expenses / Incomes)
        // Filter: only process SMS from Telebirr / EthioTelecom senders defined in AppConstants.
        // NOTE: "USSD" is excluded here — USSD responses arrive via AccessibilityService, not as an SMS sender.
        val isTrustedSender = sender.contains("TELEBIRR", ignoreCase = true) ||
            AppConstants.SMS_SENDER_WHITELIST.contains(sender)
        if (isTrustedSender) {
            
            // Loan taken
            val loanMatch = Regex("(?:loan of|taken a loan of|received a loan of)\\s*(?:ETB\\s*)?([\\d,.]+)\\s*(?:ETB|ብር)?", RegexOption.IGNORE_CASE).find(body)
            if (loanMatch != null) {
                scenario = SmsScenario.LOAN_TAKEN
                addedAmount = loanMatch.groupValues[1].replace(",", "").toDoubleOrNull()
                confidenceScore = 0.9f
            }

            // Loan repayment
            val repayMatch = Regex("(?:repaid|repayment of|loan of.*has been repaid)\\s*(?:ETB\\s*)?([\\d,.]+)\\s*(?:ETB|ብር)?", RegexOption.IGNORE_CASE).find(body)
            if (repayMatch != null) {
                scenario = SmsScenario.EXPENSE
                deductedAmount = repayMatch.groupValues[1].replace(",", "").toDoubleOrNull()
                transactionCategory = "REPAYMENT"
                confidenceScore = 0.9f
            }

            // Bank credit / income ("credited with 15,500.00 ETB", "credit of X ETB")
            val creditMatch = Regex(
                """(?:credited|credit of|has been credited)\s*(?:with\s+)?(?:ETB\s*)?([\d,]+\.?\d*)""",
                RegexOption.IGNORE_CASE
            ).find(body)
            if (creditMatch != null && loanMatch == null) {
                scenario = SmsScenario.INCOME
                addedAmount = creditMatch.groupValues[1].replace(",", "").toDoubleOrNull()
                confidenceScore = 0.9f
            }

            // Debit / Deduction (bank debits, account deductions)
            val debitMatch = Regex("(?:debited|debit of|deducted)\\s*(?:with\\s+)?(?:ETB\\s*)?(\\d[\\d,]*\\.?\\d*)", RegexOption.IGNORE_CASE).find(body)
            if (debitMatch != null && loanMatch == null && repayMatch == null) {
                scenario = SmsScenario.EXPENSE
                deductedAmount = debitMatch.groupValues[1].replace(",", "").toDoubleOrNull()
                transactionCategory = "EXPENSE"
                confidenceScore = 0.9f
            }

            // Payment / Purchase
            val paymentMatch = Regex("(?<!re)(?:paid|pay|payment|purchase)\\s*(?:ETB\\s*)?([\\d,.]+)\\s*(?:ETB|ብር)?", RegexOption.IGNORE_CASE).find(body)
            if (paymentMatch != null && loanMatch == null && creditMatch == null && repayMatch == null && debitMatch == null) {
                scenario = if (isGiftReceived) SmsScenario.RECHARGE_OR_GIFT_RECEIVED else SmsScenario.SELF_PURCHASE
                deductedAmount = paymentMatch.groupValues[1].replace(",", "").toDoubleOrNull()
                transactionCategory = "PURCHASE"
                confidenceScore = 0.9f
            }

            // Transfer / Gift Sent
            val transferMatch = Regex("(?:transferred|gifted)\\s*(?:ETB\\s*)?([\\d,.]+)\\s*(?:ETB|ብር)?", RegexOption.IGNORE_CASE).find(body)
            if (transferMatch != null) {
                scenario = SmsScenario.EXPENSE
                deductedAmount = transferMatch.groupValues[1].replace(",", "").toDoubleOrNull()
                transactionCategory = "GIFT"
                confidenceScore = 0.9f
            }

            // Service fee
            val feeMatch = if (loanMatch == null && repayMatch == null) Regex("(?:service fee of|fee\\s+of)\\s*(?:ETB\\s*)?([\\d,.]+)\\s*(?:ETB|ብር)?", RegexOption.IGNORE_CASE).find(body) else null
            if (feeMatch != null) {
                scenario = SmsScenario.EXPENSE
                deductedAmount = feeMatch.groupValues[1].replace(",", "").toDoubleOrNull()
                transactionCategory = "FEE"
                confidenceScore = 0.9f
            }
            
            // Recharges (Voucher/Electronic/USSD)
            val rechargeMatch = Regex("(?:recharged|topup|top-up|ሞልተዋል)\\s*(?:ETB\\s*)?([\\d,.]+)\\s*(?:ETB|ብር)?", RegexOption.IGNORE_CASE).find(body)
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
                // If there's no deducted amount found but packages are found, it's a BALANCE_UPDATE.
                scenario = SmsScenario.BALANCE_UPDATE
            }
        }

        return result.copy(
            scenario = scenario,
            confidence = confidenceScore,
            deductedAmount = deductedAmount,
            addedAmount = addedAmount,
            isRecharge = isRecharge,
            airtimeBalance = airtimeBalance,
            transactionCategory = transactionCategory
        )
    }
}
