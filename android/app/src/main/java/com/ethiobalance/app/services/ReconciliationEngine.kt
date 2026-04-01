package com.ethiobalance.app.services

import com.ethiobalance.app.AppConstants
import com.ethiobalance.app.data.AppDatabase
import com.ethiobalance.app.data.SmsLogEntity
import com.ethiobalance.app.data.TransactionEntity
import java.util.UUID

object ReconciliationEngine {

    /**
     * Normalize SMS sender addresses so that the same logical sender always
     * produces the same string regardless of how Android formats the number.
     * Examples: "+251127" → "127", "0127" → "127", "251994" → "994",
     *           "TELEBIRR" → "TELEBIRR" (alpha senders unchanged).
     */
    fun normalizeSender(raw: String): String {
        var s = raw.trim()
        // Alpha senders (contain letters) — return uppercased as-is
        if (s.any { it.isLetter() }) return s.uppercase()
        // Strip leading '+'
        if (s.startsWith("+")) s = s.substring(1)
        // Strip Ethiopian country code "251" if the remainder is a known short code (≤6 digits)
        if (s.startsWith("251") && s.length > 3) {
            val remainder = s.substring(3)
            if (remainder.length <= 6) s = remainder
        }
        // Strip leading '0'
        if (s.startsWith("0") && s.length > 1) s = s.substring(1)
        return s
    }
    
    suspend fun processSms(sender: String?, body: String, timestamp: Long, db: AppDatabase) {
        if (sender == null) return

        val normalizedSender = normalizeSender(sender)
        
        // 1. Dedup check — skip if this exact message was already logged
        val logDao = db.smsLogDao()
        val bodyHash = body.hashCode()
        if (logDao.existsByHash(normalizedSender, timestamp, bodyHash)) {
            return
        }
        
        // 2. Log Raw SMS
        logDao.insert(SmsLogEntity(sender = normalizedSender, message = body, parsedType = AppConstants.SMS_LOG_TYPE_PROCESSING, confidence = 0f, processed = false, timestamp = timestamp, bodyHash = bodyHash))

        // 3. Parse SMS
        val parsedResult = SmsParser.parse(normalizedSender, body, timestamp)
        if (parsedResult.confidence < 0.7f) {
            return
        }

        // 4. Apply Dual Tracking Rules based on Parsed Output
        val uniqueStr = "$normalizedSender-$timestamp-${body.hashCode()}"
        val transactionId = UUID.nameUUIDFromBytes(uniqueStr.toByteArray()).toString()

        when (parsedResult.scenario) {
            SmsScenario.SELF_PURCHASE -> {
                db.transactionDao().insert(TransactionEntity(
                    id = transactionId,
                    type = "EXPENSE",
                    amount = parsedResult.deductedAmount ?: 0.0,
                    category = "PURCHASE",
                    source = AppConstants.resolveSource(normalizedSender),
                    timestamp = timestamp,
                    reference = null
                ))
                parsedResult.packages.forEach { pkg ->
                    db.balancePackageDao().insertOrUpdate(pkg)
                }
            }

            SmsScenario.EXPENSE -> {
                db.transactionDao().insert(TransactionEntity(
                    id = transactionId,
                    type = "EXPENSE",
                    amount = parsedResult.deductedAmount ?: 0.0,
                    category = parsedResult.transactionCategory ?: "EXPENSE",
                    source = AppConstants.resolveSource(normalizedSender),
                    timestamp = timestamp,
                    reference = null
                ))
                // Also persist any packages found alongside the expense (e.g. airtime balance update)
                parsedResult.packages.forEach { pkg ->
                    db.balancePackageDao().insertOrUpdate(pkg)
                }
            }

            SmsScenario.GIFT_SENT -> {
                // Airtime/money transfers are expenses
                db.transactionDao().insert(TransactionEntity(
                    id = transactionId,
                    type = "EXPENSE",
                    amount = parsedResult.deductedAmount ?: 0.0,
                    category = parsedResult.transactionCategory ?: "GIFT",
                    source = AppConstants.resolveSource(normalizedSender),
                    timestamp = timestamp,
                    reference = null
                ))
            }

            SmsScenario.RECHARGE_OR_GIFT_RECEIVED -> {
                // Ignore raw airtime recharges as financial transactions per user rule.
                if (!parsedResult.isRecharge) {
                    parsedResult.packages.forEach { pkg ->
                        db.balancePackageDao().insertOrUpdate(pkg)
                    }
                }
            }

            SmsScenario.INCOME -> {
                db.transactionDao().insert(TransactionEntity(
                    id = transactionId,
                    type = "INCOME",
                    amount = parsedResult.addedAmount ?: 0.0,
                    category = "CREDIT",
                    source = AppConstants.resolveSource(normalizedSender),
                    timestamp = timestamp,
                    reference = null
                ))
            }

            SmsScenario.LOAN_TAKEN -> {
                // Airtime loans are telecom assets, not financial transactions.
            }
            
            SmsScenario.BALANCE_UPDATE -> {
                parsedResult.packages.forEach { pkg ->
                    db.balancePackageDao().insertOrUpdate(pkg)
                }
            }

            SmsScenario.BALANCE_QUERY -> {
                // Upsert the airtime balance package so the UI always reflects the latest ETB balance.
                parsedResult.packages.forEach { pkg ->
                    db.balancePackageDao().insertOrUpdate(pkg)
                }
            }
            
            SmsScenario.UNKNOWN -> {}
        }
    }
}
