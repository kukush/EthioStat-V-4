package com.ethiobalance.app.domain.usecase

import com.ethiobalance.app.AppConstants
import com.ethiobalance.app.data.BalancePackageEntity
import com.ethiobalance.app.domain.model.ParsedSmsResult
import com.ethiobalance.app.domain.model.SmsScenario
import java.util.*
import javax.inject.Inject

class ParseSmsUseCase @Inject constructor() {

    private fun detectLanguage(text: String): String {
        val amharicRegex = Regex("[\\u1200-\\u137F]")
        if (amharicRegex.containsMatchIn(text)) return "am"
        
        val oromoKeywords = listOf("haala", "galii", "herrega", "kennamee", "fudhattaniirtu")
        val lowerText = text.lowercase()
        if (oromoKeywords.any { lowerText.contains(it) }) return "om"
        
        return "en"
    }

    operator fun invoke(sender: String, body: String, timestamp: Long): ParsedSmsResult {
        val now = System.currentTimeMillis()
        val result = ParsedSmsResult(scenario = SmsScenario.UNKNOWN, confidence = 0f)
        var confidenceScore = 0f
        var scenario = SmsScenario.UNKNOWN
        var deductedAmount: Double? = null
        var addedAmount: Double? = null
        var isRecharge = false
        var transactionCategory: String? = null
        var transactionSubType: String? = null
        var reference: String? = null
        var airtimeBalance: Double? = null
        var partyName: String? = null
        
        // Generate a deterministic base ID
        val uniqueStr = "$sender-$timestamp-${body.hashCode()}"
        val baseId = UUID.nameUUIDFromBytes(uniqueStr.toByteArray()).toString()

        // 1. Multi-segment Package Detection (Status SMS)
        val isMultiSegment = Regex(";\\s+from ", RegexOption.IGNORE_CASE).containsMatchIn(body) &&
                body.contains("expiry date on", ignoreCase = true)

        fun addOrReplace(pkg: BalancePackageEntity) {
            val existingIdx = result.packages.indexOfFirst { it.type == pkg.type }
            when {
                existingIdx < 0 -> result.packages.add(pkg)
                pkg.totalAmount > result.packages[existingIdx].totalAmount -> result.packages[existingIdx] = pkg
            }
        }

        if (isMultiSegment) {
            val segments = body.split(Regex(";\\s*"))
            segments.forEachIndexed { _, seg ->
                val segLower = seg.lowercase()
                val expiryMs = parseExpiryMs(seg, now)

                when {
                    segLower.contains("min") || segLower.contains("voice") || segLower.contains("ደቂቃ") -> {
                        val isIdx = seg.indexOf("is ", ignoreCase = true)
                        val beforeIs = if (isIdx > 0) seg.substring(0, isIdx) else seg
                        val totalMatch = Regex("""(\d[\d,.]*)\s*Min(?!ute)""", RegexOption.IGNORE_CASE).find(beforeIs)
                        val totalVal = totalMatch?.groupValues?.get(1)?.replace(",","")?.toDoubleOrNull() ?: 0.0
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
                    segLower.contains("mb") || segLower.contains("gb") || segLower.contains("internet") -> {
                        val isIdx = seg.indexOf("is ", ignoreCase = true)
                        val beforeIs = if (isIdx > 0) seg.substring(0, isIdx) else seg
                        val totalGb = Regex("""(\d[\d,.]*)\s*GB""", RegexOption.IGNORE_CASE).find(beforeIs)
                        val totalMb = Regex("""(\d[\d,.]*)\s*MB""", RegexOption.IGNORE_CASE).find(beforeIs)
                        val totalValMB: Double = when {
                            totalGb != null -> (totalGb.groupValues[1].replace(",","").toDoubleOrNull() ?: 0.0) * 1024.0
                            totalMb != null -> totalMb.groupValues[1].replace(",","").toDoubleOrNull() ?: 0.0
                            else -> 0.0
                        }
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
        }

        // 2. Airtime & Financial (Same logic as SmsParser)
        val isTrustedSender = sender.contains("TELEBIRR", ignoreCase = true) || AppConstants.SMS_SENDER_WHITELIST.contains(sender)
        
        if (isTrustedSender) {
            // Party Name Extraction (before financial matching)
            val partyNamePatterns = listOf(
                // CBE/BOA Transfer: "to Lenco Getachew"
                Regex("(?:transfered|transferred)\\s+ETB\\s+[\\d,.]+\\s+to\\s+([A-Za-z0-9\\s./]+?)(?:\\s+on|from your account)", RegexOption.IGNORE_CASE),
                // Telebirr Merchant/Utility: "to Ethiopian Electric Utility"
                Regex("paid\\s+[\\d,.]+\\s+ETB\\s+to\\s+([^.]+?)(?:\\s+for|\\. Your current balance)", RegexOption.IGNORE_CASE),
                // Cash In/Out: "by Agent [Agent Name/ID]"
                Regex("by\\s+(Agent\\s*\\[[^\\]]+\\])", RegexOption.IGNORE_CASE),
                // CBE/BOA Credit: "by A/r Tele Birr"
                Regex("by\\s+([A-Za-z0-9\\s./]+?)\\. Available Balance", RegexOption.IGNORE_CASE),
                // Telebirr Transfer: "to 0911XXXXXX (Abebe Kebede)" or "to 0922123456" (most generic, last)
                Regex("to\\s+([^\\.]+)", RegexOption.IGNORE_CASE)
            )

            for (pattern in partyNamePatterns) {
                val match = pattern.find(body)
                if (match != null) {
                    partyName = match.groupValues[1].trim()
                    break
                }
            }

            // Financial matching logic
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

            // Bank credit / income
            val creditMatch = Regex(
                """(?:credited|credit of|has been credited|received\s+a\s+gift\s+of|received\s+ETB|transferred\s+to\s+you)\s*(?:with\s+)?(?:ETB\s*)?([\d,]+\.?\d*)""",
                RegexOption.IGNORE_CASE
            ).find(body)
            
            if (creditMatch != null && loanMatch == null) {
                scenario = SmsScenario.INCOME
                addedAmount = creditMatch.groupValues[1].replace(",", "").toDoubleOrNull()
                transactionSubType = "Received"
                confidenceScore = 0.9f
            }

            // Debit / Deduction
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
                scenario = SmsScenario.SELF_PURCHASE
                deductedAmount = paymentMatch.groupValues[1].replace(",", "").toDoubleOrNull()
                transactionSubType = "Payment"
                transactionCategory = when {
                    body.contains("utility", ignoreCase = true) -> "UTILITY"
                    body.contains("airtime", ignoreCase = true) -> "AIRTIME"
                    else -> "PURCHASE"
                }
                confidenceScore = 0.9f
            }

            // Transfer / Gift Sent
            val transferMatch = Regex("(?:transferred|transfered|sent)\\s*(?:ETB\\s*)?([\\d,.]+)\\s*(?:ETB|ብር)?", RegexOption.IGNORE_CASE).find(body)
            if (transferMatch != null) {
                scenario = SmsScenario.EXPENSE
                deductedAmount = transferMatch.groupValues[1].replace(",", "").toDoubleOrNull()
                transactionCategory = if (body.contains("gift", ignoreCase = true)) "GIFT" else "TRANSFER"
                transactionSubType = "Transfer"
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
            
            // Recharges
            val rechargeMatch = Regex("(?:recharged|topup|top-up|ሞልተዋል)\\s*(?:ETB\\s*)?([\\d,.]+)\\s*(?:ETB|ብር)?", RegexOption.IGNORE_CASE).find(body)
            if (rechargeMatch != null) {
                scenario = SmsScenario.RECHARGE_OR_GIFT_RECEIVED
                isRecharge = true
                addedAmount = rechargeMatch.groupValues[1].replace(",", "").toDoubleOrNull()
                confidenceScore = 0.9f
            }

            // Reference ID extraction
            val refMatch = Regex("""(?:Trans\s*ID|Transaction\s*ID|Ref\s*No|Reference|TRX\s*ID|Trx)[:\s]*([A-Z0-9]+)""", RegexOption.IGNORE_CASE).find(body)
            if (refMatch != null && refMatch.groupValues[1].length > 4) {
                reference = refMatch.groupValues[1]
            }

            // Package parsing
            parsePackageDetails(body, now, result)

            // Check for airtime balance (only if no financial scenario detected)
            val balanceMatch = Regex(
                """(?:your\s+(?:telebirr\s+)?(?:account\s+)?(?:new\s+)?balance\s+(?:after\s+\S+\s+)?(?:is|:)|(?:new\s+)?balance[:\s]+|ቀሪ\s*(?:ሒሳ\S*|ብዛ)?|current\s+balance)[\s:]*(?:ETB\s*)?([\d,]+\.?\d*)\s*(?:ETB|ብር)?""",
                RegexOption.IGNORE_CASE
            ).find(body)
            
            balanceMatch?.groupValues?.get(1)?.replace(",", "")?.toDoubleOrNull()?.let { airtimeBal ->
                airtimeBalance = airtimeBal
                addOrReplace(BalancePackageEntity(
                    id = "airtime-sim1", simId = "sim1", type = "airtime",
                    totalAmount = airtimeBal, remainingAmount = airtimeBal, unit = "ETB",
                    expiryDate = now + (30 * 24 * 60 * 60 * 1000L), isActive = true, source = "SMS", lastUpdated = now
                ))
                if (scenario == SmsScenario.UNKNOWN) {
                    scenario = SmsScenario.BALANCE_QUERY
                    confidenceScore = 0.85f
                }
            }
        }

        // 3. Asset inference if still UNKNOWN but packages found
        if (scenario == SmsScenario.UNKNOWN && result.packages.isNotEmpty()) {
            scenario = SmsScenario.BALANCE_UPDATE
            confidenceScore = 0.8f
        }

        return result.copy(
            scenario = scenario,
            confidence = confidenceScore,
            deductedAmount = deductedAmount,
            addedAmount = addedAmount,
            isRecharge = isRecharge,
            airtimeBalance = airtimeBalance,
            transactionCategory = transactionCategory,
            transactionSubType = transactionSubType,
            reference = reference,
            partyName = partyName
        )
    }

    private fun parsePackageDetails(body: String, now: Long, result: ParsedSmsResult) {
        fun addOrReplace(pkg: BalancePackageEntity) {
            val existingIdx = result.packages.indexOfFirst { it.type == pkg.type }
            when {
                existingIdx < 0 -> result.packages.add(pkg)
                pkg.totalAmount > result.packages[existingIdx].totalAmount -> result.packages[existingIdx] = pkg
            }
        }

        // Voice minutes
        val voiceMatch = Regex("([\\d,.]+)\\s*(?:Min(?:ute)?s?|ደቂቃ|Daqiiqaa)", RegexOption.IGNORE_CASE).find(body)
        if (voiceMatch != null) {
            val amount = voiceMatch.groupValues[1].replace(",", "").toDoubleOrNull() ?: 0.0
            if (amount > 0) {
                addOrReplace(BalancePackageEntity(
                    id = "voice-sim1", simId = "sim1", type = "voice",
                    totalAmount = amount, remainingAmount = amount, unit = "MIN",
                    expiryDate = now + (30 * 24 * 60 * 60 * 1000L),
                    isActive = true, source = "SMS", lastUpdated = now
                ))
            }
        }

        // Internet / Data
        val dataPatterns = listOf(
            Regex("""for\s+([\d,.]+)\s*(MB|GB)\s*(?:\w+\s+)*?(?:internet|internate|data|Intarneetii|ኢንተርኔት)""", RegexOption.IGNORE_CASE),
            Regex("""([\d,.]+)\s*(MB|GB)\s*(?:\w+\s+)*?(?:internet|internate|data|Intarneetii|ኢንተርኔት)\s*(?:package|bundle|plan)?""", RegexOption.IGNORE_CASE),
            Regex("""(?:internet|internate|data|Intarneetii|ኢንተርኔት)[\s:,]+([\d,.]+)\s*(MB|GB)""", RegexOption.IGNORE_CASE),
            Regex("""(?:remaining|you\s+have|left)[\s:]+([\d,.]+)\s*(MB|GB)""", RegexOption.IGNORE_CASE),
            Regex("""([\d,.]+)\s*(MB|GB)""", RegexOption.IGNORE_CASE)
        )
        for (pat in dataPatterns) {
            val m = pat.find(body) ?: continue
            val rawNum  = m.groupValues[1].replace(",", "")
            val rawUnit = m.groupValues[2].uppercase().ifEmpty {
                Regex("(MB|GB)", RegexOption.IGNORE_CASE).find(m.value)?.groupValues?.get(1)?.uppercase() ?: "MB"
            }
            val amount = rawNum.toDoubleOrNull() ?: 0.0
            if (amount <= 0) continue
            val amountMB = if (rawUnit == "GB") amount * 1024.0 else amount
            addOrReplace(BalancePackageEntity(
                id = "internet-sim1", simId = "sim1", type = "internet",
                totalAmount = amountMB, remainingAmount = amountMB, unit = "MB",
                expiryDate = now + (30 * 24 * 60 * 60 * 1000L),
                isActive = true, source = "SMS", lastUpdated = now
            ))
            break
        }

        // SMS messages
        val smsMatch = Regex("([\\d,.]+)\\s*(?:SMS|ኤስኤምኤስ)", RegexOption.IGNORE_CASE).find(body)
        if (smsMatch != null) {
            val amount = smsMatch.groupValues[1].replace(",", "").toDoubleOrNull() ?: 0.0
            if (amount > 0) {
                addOrReplace(BalancePackageEntity(
                    id = "sms-sim1", simId = "sim1", type = "sms",
                    totalAmount = amount, remainingAmount = amount, unit = "SMS",
                    expiryDate = now + (30 * 24 * 60 * 60 * 1000L),
                    isActive = true, source = "SMS", lastUpdated = now
                ))
            }
        }
    }

    private fun parseExpiryMs(seg: String, now: Long): Long {
        val m = Regex("""with expiry date on (\d{4})-(\d{2})-(\d{2})""", RegexOption.IGNORE_CASE).find(seg)
        return if (m != null) {
            try {
                val cal = Calendar.getInstance()
                cal.set(m.groupValues[1].toInt(), m.groupValues[2].toInt() - 1, m.groupValues[3].toInt(), 23, 59, 59)
                cal.timeInMillis
            } catch (e: Exception) { now + (7 * 24 * 60 * 60 * 1000L) }
        } else { now + (7 * 24 * 60 * 60 * 1000L) }
    }
}
