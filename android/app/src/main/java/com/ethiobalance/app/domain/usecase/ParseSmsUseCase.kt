package com.ethiobalance.app.domain.usecase

import com.ethiobalance.app.AppConstants
import com.ethiobalance.app.data.BalancePackageEntity
import com.ethiobalance.app.domain.model.ParsedSmsResult
import com.ethiobalance.app.domain.model.SmsScenario
import java.util.*
import javax.inject.Inject

class ParseSmsUseCase @Inject constructor() {

    // Helper data class for 4-value return
    private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

    // Composite package ID: {type}-{subType}-{YYYYMMDD} to avoid collisions
    private fun buildPackageId(type: String, subType: String, expiryMs: Long): String {
        val cal = Calendar.getInstance().apply { timeInMillis = expiryMs }
        val y = cal.get(Calendar.YEAR)
        val m = cal.get(Calendar.MONTH) + 1
        val d = cal.get(Calendar.DAY_OF_MONTH)
        val datePart = "%04d%02d%02d".format(y, m, d)
        val subPart = subType.ifEmpty { "default" }
        return "$type-$subPart-$datePart"
    }

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
        val _baseId = UUID.nameUUIDFromBytes(uniqueStr.toByteArray()).toString() // Unused - for future deterministic ID generation

        // 1. Multi-segment Package Detection (Status SMS)
        val isMultiSegment = Regex(";\\s+from ", RegexOption.IGNORE_CASE).containsMatchIn(body) &&
                body.contains("expiry date on", ignoreCase = true)

        if (isMultiSegment) {
            // Clear stale telecom packages before re-parsing; each segment creates its own entity
            result.packages.removeAll { it.type in setOf("voice", "internet", "sms", "bonus") }

            val segments = body.split(Regex(";\\s*"))
            segments.forEach { seg ->
                val segLower = seg.lowercase()
                val expiryMs = parseExpiryMs(seg, now)
                val isIdx = seg.indexOf("is ", ignoreCase = true)
                val beforeIs = if (isIdx > 0) seg.substring(0, isIdx) else seg

                // ── Voice: independent check ──
                val hasMinuteData = segLower.contains("min") || segLower.contains("voice") || segLower.contains("ደቂቃ")
                if (hasMinuteData) {
                    // Match "125 Min", "63Min", "50 minutes" — all minute variants
                    val allTotals = Regex("""(\d[\d,.]*)\s*Min(?:ute)?s?""", RegexOption.IGNORE_CASE).findAll(beforeIs).toList()
                    // Remaining: "is 44 minute and 3 second" or "is 52 minute and 1 second"
                    val remainMinMatch = Regex("""is\s+(\d[\d,.]*)\s*(?:Min(?:ute)?s?|ደቂቃ)""", RegexOption.IGNORE_CASE).find(seg)
                    val remainMinBase = remainMinMatch?.groupValues?.get(1)?.replace(",","")?.toDoubleOrNull() ?: 0.0
                    // Extract seconds: "and 3 second" → +3/60
                    val secMatch = Regex("""and\s+(\d+)\s*second""", RegexOption.IGNORE_CASE).find(seg)
                    val remainSec = secMatch?.groupValues?.get(1)?.toDoubleOrNull() ?: 0.0
                    val remainVal = Math.round(remainMinBase + remainSec / 60.0).toDouble()

                    if (remainVal > 0 && allTotals.isNotEmpty()) {
                        val hasNight = segLower.contains("night") || segLower.contains("ምሽት")
                        val hasBonus = segLower.contains("bonus") || segLower.contains("free") || segLower.contains("promo")
                        val hasRecurring = segLower.contains("recurring")

                        val quotas = allTotals.mapNotNull {
                            it.groupValues[1].replace(",","").toDoubleOrNull()
                        }.sorted()

                        val (totalVal, pkgType, _, subType) = when {
                            // Combined plan with night (e.g., 125 + 63 Min)
                            hasNight && quotas.size >= 2 -> {
                                val smallerQuota = quotas[0] // 63
                                val largerQuota = quotas[1]  // 125
                                if (remainVal <= smallerQuota) {
                                    Quadruple(smallerQuota, "voice", "Night Package", "Night")
                                } else {
                                    Quadruple(largerQuota, "voice", "Regular Package", "Recurring")
                                }
                            }
                            // Free/Bonus voice (e.g., "50 minutes + 200 MB Free")
                            hasBonus -> {
                                val total = quotas.firstOrNull() ?: remainVal
                                val sub = if (segLower.contains("free") || segLower.contains("promo")) "Free" else "Bonus"
                                Quadruple(total, "voice", "Free Package", sub)
                            }
                            // Recurring only
                            hasRecurring -> {
                                val total = quotas.lastOrNull() ?: remainVal
                                Quadruple(total, "voice", "Recurring Package", "Recurring")
                            }
                            // Single quota voice
                            else -> {
                                val total = quotas.firstOrNull() ?: remainVal
                                Quadruple(total, "voice", "Voice Package", extractSubType(segLower))
                            }
                        }

                        val voiceId = buildPackageId(pkgType, subType, expiryMs)
                        result.packages.add(BalancePackageEntity(
                            id = voiceId, simId = "", type = pkgType,
                            subType = subType, totalAmount = totalVal,
                            remainingAmount = remainVal, unit = "MIN",
                            expiryDate = expiryMs, isActive = true, source = "SMS", lastUpdated = now
                        ))
                        confidenceScore = 0.9f
                    }
                }

                // ── Internet: independent check ──
                val hasMbGb = segLower.contains("mb") || segLower.contains("gb")
                val hasInternetKeyword = segLower.contains("internet")
                if (hasMbGb || hasInternetKeyword) {
                    val totalGb = Regex("""(\d[\d,.]*)(\s*GB)""", RegexOption.IGNORE_CASE).find(beforeIs)
                    val totalMb = Regex("""(\d[\d,.]*)(\s*MB)""", RegexOption.IGNORE_CASE).find(beforeIs)
                    val totalValMB: Double = when {
                        totalGb != null -> (totalGb.groupValues[1].replace(",","").toDoubleOrNull() ?: 0.0) * 1024.0
                        totalMb != null -> totalMb.groupValues[1].replace(",","").toDoubleOrNull() ?: 0.0
                        else -> 0.0
                    }
                    // Only use remaining MB/GB values (after "is"), not minute values
                    val remainGb = Regex("""is\s+(\d[\d,.]*)\s*GB""", RegexOption.IGNORE_CASE).find(seg)
                    val remainMb = Regex("""is\s+(\d[\d,.]*)\s*MB""", RegexOption.IGNORE_CASE).find(seg)
                    val remainValRaw: Double = when {
                        remainGb != null -> (remainGb.groupValues[1].replace(",","").toDoubleOrNull() ?: 0.0) * 1024.0
                        remainMb != null -> remainMb.groupValues[1].replace(",","").toDoubleOrNull() ?: 0.0
                        else -> totalValMB // No remaining specified → use total (full allocation)
                    }
                    val remainVal = Math.round(remainValRaw).toDouble()
                    if (remainVal > 0) {
                        val inetSubType = extractSubType(segLower)
                        val inetId = buildPackageId("internet", inetSubType, expiryMs)
                        result.packages.add(BalancePackageEntity(
                            id = inetId, simId = "", type = "internet",
                            subType = inetSubType,
                            totalAmount = if (totalValMB > 0) totalValMB else remainVal,
                            remainingAmount = remainVal, unit = "MB",
                            expiryDate = expiryMs, isActive = true, source = "SMS", lastUpdated = now
                        ))
                        confidenceScore = 0.9f
                    }
                }

                // ── SMS: independent check ──
                if (segLower.contains("sms") || segLower.contains("ኤስኤምኤስ")) {
                    val remainSms = Regex("""is\s+(\d[\d,.]*)\s*(?:SMS|ኤስኤምኤስ)""", RegexOption.IGNORE_CASE).find(seg)
                        ?: Regex("""(\d[\d,.]*)\s*(?:SMS|ኤስኤምኤስ)""", RegexOption.IGNORE_CASE).find(seg)
                    val remainVal = Math.round(remainSms?.groupValues?.get(1)?.replace(",","")?.toDoubleOrNull() ?: 0.0).toDouble()
                    if (remainVal > 0) {
                        val smsSubType = extractSubType(segLower)
                        val smsId = buildPackageId("sms", smsSubType, expiryMs)
                        result.packages.add(BalancePackageEntity(
                            id = smsId, simId = "", type = "sms",
                            subType = smsSubType, totalAmount = remainVal,
                            remainingAmount = remainVal, unit = "SMS",
                            expiryDate = expiryMs, isActive = true, source = "SMS", lastUpdated = now
                        ))
                        confidenceScore = 0.9f
                    }
                }

                // ── Bonus ETB: only if segment has ETB context (not voice segments mentioning "bonus") ──
                if ((segLower.contains("birr") || segLower.contains("gift")) && !hasMinuteData) {
                    val bonusMatch = Regex("""(?:gift|bonus)(?:\s+balance)?[:\s]+(?:ETB\s*)?(\d[\d,.]*)""", RegexOption.IGNORE_CASE).find(seg)
                    val bonusVal = bonusMatch?.groupValues?.get(1)?.replace(",","")?.toDoubleOrNull() ?: 0.0
                    if (bonusVal > 0) {
                        val bonusId = buildPackageId("bonus", "Bonus", expiryMs)
                        result.packages.add(BalancePackageEntity(
                            id = bonusId, simId = "", type = "bonus",
                            subType = "Bonus", totalAmount = bonusVal,
                            remainingAmount = bonusVal, unit = "ETB",
                            expiryDate = expiryMs, isActive = true, source = "SMS", lastUpdated = now
                        ))
                        confidenceScore = 0.9f
                    }
                }
            }
        }

        // Helper: upsert by package ID (used for balance/airtime packages below)
        fun addOrReplace(pkg: BalancePackageEntity) {
            val existingIdx = result.packages.indexOfFirst { it.id == pkg.id }
            when {
                existingIdx < 0 -> result.packages.add(pkg)
                else -> result.packages[existingIdx] = pkg
            }
        }

        // 2. Airtime & Financial (Same logic as SmsParser)
        val isTrustedSender = sender.contains("TELEBIRR", ignoreCase = true) || AppConstants.SMS_SENDER_WHITELIST.any { it.equals(sender, ignoreCase = true) }
        
        // Early exit: reject promotional/loyalty SMS (e.g. Telebirr "received 1 point and 1 lottery ticket")
        val lowerBody = body.lowercase()
        val isPromoMessage = lowerBody.contains("lottery") ||
                Regex("received\\s+\\d+\\s+point", RegexOption.IGNORE_CASE).containsMatchIn(body) ||
                lowerBody.contains("teleplay") ||
                body.contains("ቴሌፕለይ") ||
                body.contains("ቴሌኮይን") ||
                body.contains("በቴሌብር ስላደረጉት")
        if (isPromoMessage) {
            return result.copy(confidence = 0f, scenario = SmsScenario.UNKNOWN)
        }

        if (isTrustedSender) {
            // Party Name Extraction (before financial matching)
            val partyNamePatterns = listOf(
                // Telebirr Package Purchase: "for package Night Internet Package 600 MB purchase"
                Regex("for\\s+package\\s+(.+?)\\s+purchase", RegexOption.IGNORE_CASE),
                // Telebirr Package Purchase alt: "paid ETB X for [package name] purchase made for"
                Regex("paid\\s+ETB\\s+[\\d,.]+\\s+for\\s+(.+?)\\s+purchase\\s+made", RegexOption.IGNORE_CASE),
                // CBE/BOA Transfer: "to Lenco Getachew"
                Regex("(?:transfered|transferred)\\s+(?:ETB\\s*)?[\\d,.]+\\s+to\\s+([A-Za-z0-9\\s./]+?)(?:\\s+on|from your account)", RegexOption.IGNORE_CASE),
                // Awash School Fee: "paid 2,574 BIRR School Fee for YN/566/18 - Hermon Faris in"
                Regex("paid\\s+[\\d,]+\\s+BIRR\\s+.+?\\s+for\\s+[^-]+-\\s*([A-Za-z\\s]+?)\\s+in\\s", RegexOption.IGNORE_CASE),
                // Awash generic: "for [description] - [Name]"
                Regex("for\\s+[A-Z0-9/]+\\s*-\\s*([A-Za-z\\s]+?)\\s+in\\s", RegexOption.IGNORE_CASE),
                // Telebirr Merchant/Utility: "paid ETB X to [merchant]"
                Regex("paid\\s+[\\d,.]+\\s+(?:ETB|BIRR)\\s+to\\s+([^.]+?)(?:\\s+for|\\. Your current balance)", RegexOption.IGNORE_CASE),
                Regex("paid\\s+(?:ETB|BIRR)\\s*[\\d,.]+\\s+to\\s+([^.]+?)(?:\\s+for|\\. Your current balance)", RegexOption.IGNORE_CASE),
                // Cash In/Out: "by Agent [Agent Name/ID]"
                Regex("by\\s+(Agent\\s*\\[[^\\]]+\\])", RegexOption.IGNORE_CASE),
                // CBE/BOA Credit: "by A/r Tele Birr"
                Regex("by\\s+([A-Za-z0-9\\s./]+?)\\. Available Balance", RegexOption.IGNORE_CASE),
                // Telebirr Transfer: "to 0911XXXXXX (Abebe Kebede)"
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
                """(?:credited|credit of|has been credited|received\s+a\s+gift\s+of|received\s+ETB|received\s+BIRR|transferred\s+to\s+you)\s*(?:with\s+)?(?:ETB\s*|BIRR\s*)?([\d,]+\.?\d*)""",
                RegexOption.IGNORE_CASE
            ).find(body)
            
            // Alternative: "ETB X has been credited" (amount before "credited")
            val creditMatchAlt = Regex(
                """(?:ETB\s*)?([\d,]+\.?\d*)\s*(?:has\s+been\s+credited|credited)""",
                RegexOption.IGNORE_CASE
            ).find(body)
            
            val effectiveCreditMatch = creditMatch ?: creditMatchAlt
            if (effectiveCreditMatch != null && loanMatch == null) {
                scenario = SmsScenario.INCOME
                addedAmount = effectiveCreditMatch.groupValues[1].replace(",", "").toDoubleOrNull()
                transactionSubType = "Received"
                confidenceScore = 0.9f
            }

            // Debit / Deduction
            val debitMatch = Regex("(?:debited|debit of|deducted)\\s*(?:with\\s+)?(?:ETB\\s*|BIRR\\s*)?(\\d[\\d,]*\\.?\\d*)", RegexOption.IGNORE_CASE).find(body)
            
            // Alternative: "debited for [party] with ETB X" (amount after "with ETB")
            val debitMatchAlt = Regex("debited\\s+for\\s+.+?\\swith\\s+ETB\\s*(\\d[\\d,]*\\.?\\d*)", RegexOption.IGNORE_CASE).find(body)
            
            val effectiveDebitMatch = debitMatch ?: debitMatchAlt
            if (effectiveDebitMatch != null && loanMatch == null && repayMatch == null) {
                scenario = SmsScenario.EXPENSE
                deductedAmount = effectiveDebitMatch.groupValues[1].replace(",", "").toDoubleOrNull()
                transactionCategory = "EXPENSE"
                confidenceScore = 0.9f
            }

            // Payment / Purchase (Telebirr/Awash: "You have paid X ETB" or "paid 2,574 BIRR")
            val telebirrPaymentMatch = Regex("(?:you\\s+have\\s+)?(?:successfully\\s+)?(?:paid|payment|purchase).*?([\\d,.]+)\\s*(?:ETB|BIRR|ብር)", RegexOption.IGNORE_CASE).find(body)
            if (telebirrPaymentMatch != null && loanMatch == null && creditMatch == null && repayMatch == null && debitMatch == null) {
                scenario = SmsScenario.SELF_PURCHASE
                deductedAmount = telebirrPaymentMatch.groupValues[1].replace(",", "").toDoubleOrNull()
                transactionSubType = "Payment"
                transactionCategory = when {
                    body.contains("utility", ignoreCase = true) -> "UTILITY"
                    body.contains("airtime", ignoreCase = true) -> "AIRTIME"
                    body.contains("internet", ignoreCase = true) -> "INTERNET"
                    body.contains("voice", ignoreCase = true) || body.contains("minute", ignoreCase = true) -> "VOICE"
                    body.contains("sms", ignoreCase = true) -> "SMS"
                    else -> "PURCHASE"
                }
                confidenceScore = 0.95f
            }

            // Transfer / Gift Sent
            val telebirrTransferMatch = Regex("(?:you\\s+have\\s+)?(?:sent|transfer).*?([\\d,.]+)\\s*(?:ETB|BIRR|ብር)", RegexOption.IGNORE_CASE).find(body)
            if (telebirrTransferMatch != null) {
                scenario = SmsScenario.EXPENSE
                deductedAmount = telebirrTransferMatch.groupValues[1].replace(",", "").toDoubleOrNull()
                transactionCategory = if (body.contains("gift", ignoreCase = true)) "GIFT" else "TRANSFER"
                transactionSubType = "Transfer"
                confidenceScore = 0.95f
            }

            // Received / Cash In
            val telebirrReceivedMatch = Regex("(?:you\\s+have\\s+)?(?:received).*?([\\d,.]+)\\s*(?:ETB|BIRR|ብር)", RegexOption.IGNORE_CASE).find(body)
            if (telebirrReceivedMatch != null && scenario == SmsScenario.UNKNOWN) {
                scenario = SmsScenario.INCOME
                addedAmount = telebirrReceivedMatch.groupValues[1].replace(",", "").toDoubleOrNull()
                transactionSubType = "Received"
                transactionCategory = "CASH_IN"
                confidenceScore = 0.95f
            }

            // Generic patterns (CBE/Bank style)
            val bankPaymentMatch = Regex("(?<!re)(?:paid|pay|payment|purchase)\\s*(?:ETB\\s*)?([\\d,.]+)\\s*(?:ETB|ብር)?", RegexOption.IGNORE_CASE).find(body)
            if (bankPaymentMatch != null && loanMatch == null && creditMatch == null && repayMatch == null && debitMatch == null && scenario == SmsScenario.UNKNOWN) {
                scenario = SmsScenario.SELF_PURCHASE
                deductedAmount = bankPaymentMatch.groupValues[1].replace(",", "").toDoubleOrNull()
                transactionSubType = "Payment"
                transactionCategory = "PURCHASE"
                confidenceScore = 0.85f
            }

            val bankTransferMatch = Regex("(?:transferred|transfered|sent)\\s*(?:ETB\\s*)?([\\d,.]+)\\s*(?:ETB|ብር)?", RegexOption.IGNORE_CASE).find(body)
            if (bankTransferMatch != null && scenario == SmsScenario.UNKNOWN) {
                scenario = SmsScenario.EXPENSE
                deductedAmount = bankTransferMatch.groupValues[1].replace(",", "").toDoubleOrNull()
                transactionCategory = if (body.contains("gift", ignoreCase = true)) "GIFT" else "TRANSFER"
                transactionSubType = "Transfer"
                confidenceScore = 0.85f
            }

            // Service fee (only if no other scenario already detected)
            val feeMatch = if (loanMatch == null && repayMatch == null && scenario == SmsScenario.UNKNOWN) Regex("(?:service fee of|fee\\s+of|service fee)[:\\s]*(?:ETB\\s*)?([\\d,.]+)", RegexOption.IGNORE_CASE).find(body) else null
            if (feeMatch != null) {
                scenario = SmsScenario.EXPENSE
                deductedAmount = feeMatch.groupValues[1].replace(",", "").toDoubleOrNull()
                transactionCategory = "FEE"
                confidenceScore = 0.9f
            }
            
            // Cash In / Cash Out (Specific to Telebirr/Mobile Money)
            val cashInMatch = Regex("(?:cash\\s*in|received|deposit|deposited)[:\\s]*(?:ETB\\s*)?([\\d,.]+)", RegexOption.IGNORE_CASE).find(body)
            if (cashInMatch != null && scenario == SmsScenario.UNKNOWN) {
                scenario = SmsScenario.INCOME
                addedAmount = cashInMatch.groupValues[1].replace(",", "").toDoubleOrNull()
                transactionCategory = "CASH_IN"
                confidenceScore = 0.95f
            }

            val cashOutMatch = Regex("(?:cash\\s*out|withdrawn|withdraw)[:\\s]*(?:ETB\\s*)?([\\d,.]+)", RegexOption.IGNORE_CASE).find(body)
            if (cashOutMatch != null && scenario == SmsScenario.UNKNOWN) {
                scenario = SmsScenario.EXPENSE
                deductedAmount = cashOutMatch.groupValues[1].replace(",", "").toDoubleOrNull()
                transactionCategory = "CASH_OUT"
                confidenceScore = 0.95f
            }
            
            // Recharges
            val rechargeMatch = Regex("(?:recharged|topup|top-up|ሞልተዋል|recharge)[:\\s]*(?:ETB\\s*)?([\\d,.]+)", RegexOption.IGNORE_CASE).find(body)
            if (rechargeMatch != null) {
                scenario = SmsScenario.RECHARGE_OR_GIFT_RECEIVED
                isRecharge = true
                addedAmount = rechargeMatch.groupValues[1].replace(",", "").toDoubleOrNull()
                confidenceScore = 0.95f
            }

            // Reference ID extraction
            val refMatch = Regex("""(?:Trans\s*ID|Transaction\s*ID|Ref\s*No|Reference|TRX\s*ID|Trx)[:\s]*([A-Z0-9]+)""", RegexOption.IGNORE_CASE).find(body)
            if (refMatch != null && refMatch.groupValues[1].length > 4) {
                reference = refMatch.groupValues[1]
            }

            // Package parsing (skip if multi-segment already parsed).
            // ONLY run for Ethio Telecom senders (994 and variants) — other
            // sources like Telebirr (127) or CBE can mention MB/GB/Min in
            // promo/purchase SMS but must NOT create telecom asset rows.
            // Telecom assets are authoritatively driven by 994 multi-segment
            // balance SMS (purge + replace) and 994 single-segment "You have
            // received …" SMS (additive upsert) via refreshTelecomSmart.
            val isTelecomSender = AppConstants.TELECOM_SENDERS.any {
                it.equals(sender, ignoreCase = true)
            }
            if (!isMultiSegment && isTelecomSender) {
                parsePackageDetails(body, now, result)
            }

            // Check for balance (wallet, bank, or airtime depending on sender)
            val balanceMatch = Regex(
                """(?:your\s+(?:telebirr\s+)?(?:account\s+)?(?:new\s+)?balance\s+(?:after\s+\S+\s+)?(?:is|:)|(?:new\s+)?balance[:\s]+|ቀሪ\s*(?:ሒሳ\S*|ብዛ)?|current\s+balance)[\s:]*(?:ETB\s*)?([\d,]+\.?\d*)\s*(?:ETB|ብር)?""",
                RegexOption.IGNORE_CASE
            ).find(body)
            
            // Alternative: "Account Balance is : ETB X" (with "is" before colon)
            val balanceMatchAlt = Regex(
                """balance\s+is\s*[:\s]*(?:ETB\s*)?([\d,]+\.?\d*)\s*(?:ETB|ብር)?""",
                RegexOption.IGNORE_CASE
            ).find(body)
            
            // Alternative: "Available Balance is ETB X" or "Available Balance: ETB X"
            val balanceMatchAvailable = Regex(
                """available\s+balance[:\s]+(?:ETB\s*)?([\d,]+\.?\d*)""",
                RegexOption.IGNORE_CASE
            ).find(body)
            
            val effectiveBalanceMatch = balanceMatch ?: balanceMatchAlt ?: balanceMatchAvailable
            effectiveBalanceMatch?.groupValues?.get(1)?.replace(",", "")?.toDoubleOrNull()?.let { bal ->
                // Balance type: only EthioTelecom senders (994, 804, 806, 830) are airtime (telecom asset).
                // Everything else (Telebirr, banks) is bank_balance (per-source).
                val resolvedSource = AppConstants.resolveSource(sender)
                val isEthioTelecom = resolvedSource == AppConstants.SOURCE_AIRTIME
                val balanceType = if (isEthioTelecom) "airtime" else "bank_balance"
                val balanceId = if (isEthioTelecom) "airtime" else "bank_balance-$resolvedSource"
                
                airtimeBalance = bal
                addOrReplace(BalancePackageEntity(
                    id = balanceId, simId = resolvedSource, type = balanceType,
                    totalAmount = bal, remainingAmount = bal, unit = "ETB",
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
            partyName = partyName,
            isMultiSegmentBalance = isMultiSegment
        )
    }

    private fun parsePackageDetails(body: String, now: Long, result: ParsedSmsResult) {
        val bodyLower = body.lowercase()
        val pkgSubType = extractSubType(bodyLower)
        
        // Parse expiry from single-segment SMS (e.g., "expired on 17-04-2026 06:59:59")
        val expiryMatch = Regex("""(?:expired? on|expiry date on|expire after)\s+(\d{1,2})-(\d{1,2})-(\d{4})\s+(\d{2}):(\d{2}):(\d{2})""", RegexOption.IGNORE_CASE).find(body)
            ?: Regex("""(?:expired? on|expiry date on)\s+(\d{4})-(\d{2})-(\d{2})""", RegexOption.IGNORE_CASE).find(body)
        val expiryMs = if (expiryMatch != null) {
            try {
                val cal = Calendar.getInstance()
                val groups = expiryMatch.groupValues
                if (groups.size >= 7) {
                    // DD-MM-YYYY HH:mm:ss format
                    cal.set(groups[3].toInt(), groups[2].toInt() - 1, groups[1].toInt(), groups[4].toInt(), groups[5].toInt(), groups[6].toInt())
                } else {
                    // YYYY-MM-DD format
                    cal.set(groups[1].toInt(), groups[2].toInt() - 1, groups[3].toInt(), 23, 59, 59)
                }
                cal.timeInMillis
            } catch (_: Exception) { now + (30 * 24 * 60 * 60 * 1000L) }
        } else {
            // Check for "expire after 24 hr" style
            val hoursMatch = Regex("""expire after\s+(\d+)\s*hr""", RegexOption.IGNORE_CASE).find(body)
            if (hoursMatch != null) {
                now + (hoursMatch.groupValues[1].toLongOrNull() ?: 24L) * 60 * 60 * 1000L
            } else {
                now + (30 * 24 * 60 * 60 * 1000L)
            }
        }
        
        fun addOrReplace(pkg: BalancePackageEntity) {
            val existingIdx = result.packages.indexOfFirst { it.id == pkg.id }
            when {
                existingIdx < 0 -> result.packages.add(pkg)
                else -> result.packages[existingIdx] = pkg
            }
        }

        // Voice minutes
        val voiceMatch = Regex("([\\d,.]+)\\s*(?:Min(?:ute)?s?|ደቂቃ|Daqiiqaa)", RegexOption.IGNORE_CASE).find(body)
        if (voiceMatch != null) {
            val amount = Math.round(voiceMatch.groupValues[1].replace(",", "").toDoubleOrNull() ?: 0.0).toDouble()
            if (amount > 0) {
                val voiceId = buildPackageId("voice", pkgSubType, expiryMs)
                addOrReplace(BalancePackageEntity(
                    id = voiceId, simId = "", type = "voice",
                    subType = pkgSubType,
                    totalAmount = amount, remainingAmount = amount, unit = "MIN",
                    expiryDate = expiryMs,
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
            val amountMB = Math.round(if (rawUnit == "GB") amount * 1024.0 else amount).toDouble()
            val internetId = buildPackageId("internet", pkgSubType, expiryMs)
            addOrReplace(BalancePackageEntity(
                id = internetId, simId = "", type = "internet",
                subType = pkgSubType,
                totalAmount = amountMB, remainingAmount = amountMB, unit = "MB",
                expiryDate = expiryMs,
                isActive = true, source = "SMS", lastUpdated = now
            ))
            break
        }

        // SMS messages
        val smsMatch = Regex("([\\d,.]+)\\s*(?:SMS|ኤስኤምኤስ)", RegexOption.IGNORE_CASE).find(body)
        if (smsMatch != null) {
            val amount = Math.round(smsMatch.groupValues[1].replace(",", "").toDoubleOrNull() ?: 0.0).toDouble()
            if (amount > 0) {
                val smsId = buildPackageId("sms", pkgSubType, expiryMs)
                addOrReplace(BalancePackageEntity(
                    id = smsId, simId = "", type = "sms",
                    subType = pkgSubType,
                    totalAmount = amount, remainingAmount = amount, unit = "SMS",
                    expiryDate = expiryMs,
                    isActive = true, source = "SMS", lastUpdated = now
                ))
            }
        }
    }

    private fun extractSubType(textLower: String): String {
        return when {
            textLower.contains("create your own") -> "Custom"
            textLower.contains("night") || textLower.contains("ምሽት") -> "Night"
            textLower.contains("free") || textLower.contains("promo") -> "Free"
            textLower.contains("bonus") -> "Bonus"
            textLower.contains("daily") -> "Daily"
            textLower.contains("weekly") -> "Weekly"
            textLower.contains("monthly") -> "Monthly"
            textLower.contains("yearly") -> "Yearly"
            textLower.contains("recurring") -> "Recurring"
            else -> ""
        }
    }

    private fun extractSegmentLabel(seg: String): String {
        // Strip leading "from " or "Dear Customer, your remaining amount from "
        var cleaned = seg.replace(Regex("^\\s*(?:Dear Customer,?\\s*)?(?:your remaining amount from\\s+)?(?:from\\s+)?", RegexOption.IGNORE_CASE), "").trim()
        // Take everything before "is <number>" (the package name)
        val isMatch = Regex("""^(.+?)\s+is\s+\d""", RegexOption.IGNORE_CASE).find(cleaned)
        var label = isMatch?.groupValues?.get(1)?.trim() ?: cleaned.split(Regex("\\s+")).take(6).joinToString(" ")
        
        // Remove noise: "from telebirr to be expired after N days"
        label = label.replace(Regex("\\s+from\\s+telebirr.*", RegexOption.IGNORE_CASE), "")
        // Remove noise: "to be expired after N days"
        label = label.replace(Regex("\\s+to be expired.*", RegexOption.IGNORE_CASE), "")
        
        // Remove redundant keywords and amount values
        // Remove "Internet" keyword (redundant for internet package cards)
        label = label.replace(Regex("\\bInternet\\b", RegexOption.IGNORE_CASE), "")
        // Remove amount values: 12GB, 5GB, 125MB, 120 Min, 63Min, 200 SMS, etc.
        label = label.replace(Regex("\\d+[.,]?\\d*\\s*(GB|MB|KB|Min|minute|minutes|SMS|ETB|MB/s)?\\b", RegexOption.IGNORE_CASE), "")
        // Remove "and" conjunctions left after removing amounts
        label = label.replace(Regex("\\s+and\\s+", RegexOption.IGNORE_CASE), " ")
        
        // Preserve important package type indicators: capture period + type keywords
        val periodPattern = Regex("(Monthly|Daily|Weekly|Yearly)", RegexOption.IGNORE_CASE)
        val typePattern = Regex("(Recurring|Night|Bonus|Free|Normal|Promo|Create Your Own)", RegexOption.IGNORE_CASE)
        val periodMatch = periodPattern.find(label)
        val typeMatch = typePattern.find(label)
        
        // Build concise label: Period + Type + "Package"
        val labelBuilder = StringBuilder()
        periodMatch?.let { labelBuilder.append(it.value).append(" ") }
        typeMatch?.let { labelBuilder.append(it.value).append(" ") }
        
        // Add "Package" keyword if the original had it (and it's not already a type)
        if (label.contains("package", ignoreCase = true) || label.contains("bundle", ignoreCase = true)) {
            if (!labelBuilder.toString().contains("Package", ignoreCase = true)) {
                labelBuilder.append("Package ")
            }
        }
        
        label = labelBuilder.toString().trim()
        
        // Truncate if still too long
        if (label.length > 50) label = label.take(50) + "…"
        return label.ifEmpty { "Package" }
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
