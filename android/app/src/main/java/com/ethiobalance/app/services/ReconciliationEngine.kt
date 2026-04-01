package com.ethiobalance.app.services

import com.ethiobalance.app.AppConstants
import com.ethiobalance.app.data.AppDatabase
import com.ethiobalance.app.data.SmsLogEntity
import com.ethiobalance.app.data.TransactionEntity
import java.util.UUID

object ReconciliationEngine {
    
    suspend fun processSms(sender: String?, body: String, timestamp: Long, db: AppDatabase) {
        if (sender == null) return
        
        // 1. Log Raw SMS
        val logDao = db.smsLogDao()
        logDao.insert(SmsLogEntity(sender = sender, message = body, parsedType = AppConstants.SMS_LOG_TYPE_PROCESSING, confidence = 0f, processed = false, timestamp = timestamp))

        // 2. Parse SMS
        val parsedResult = SmsParser.parse(sender, body, timestamp)
        if (parsedResult.confidence < 0.7f) {
            return
        }

        // 3. Apply Dual Tracking Rules based on Parsed Output
        val uniqueStr = "$sender-$timestamp-${body.hashCode()}"
        val transactionId = UUID.nameUUIDFromBytes(uniqueStr.toByteArray()).toString()

        when (parsedResult.scenario) {
            SmsScenario.SELF_PURCHASE -> {
                db.transactionDao().insert(TransactionEntity(
                    id = transactionId,
                    type = "EXPENSE",
                    amount = parsedResult.deductedAmount ?: 0.0,
                    category = "PURCHASE",
                    source = AppConstants.resolveSource(sender),
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
                    source = AppConstants.resolveSource(sender),
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
                    source = AppConstants.resolveSource(sender),
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
                    source = AppConstants.resolveSource(sender),
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
