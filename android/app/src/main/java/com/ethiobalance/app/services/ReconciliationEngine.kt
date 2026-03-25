package com.ethiobalance.app.services

import com.ethiobalance.app.data.AppDatabase
import com.ethiobalance.app.data.SmsLogEntity
import com.ethiobalance.app.data.TransactionEntity
import java.util.UUID

object ReconciliationEngine {
    
    suspend fun processSms(sender: String?, body: String, timestamp: Long, db: AppDatabase) {
        if (sender == null) return
        
        // 1. Log Raw SMS
        val logDao = db.smsLogDao()
        logDao.insert(SmsLogEntity(sender = sender, message = body, parsedType = "PROCESSING", confidence = 0f, processed = false, timestamp = timestamp))

        // 2. Parse SMS
        val parsedResult = SmsParser.parse(sender, body)
        if (parsedResult.confidence < 0.7f) {
            // Update log and return
            // logDao.update(...) would go here
            return
        }

        // 3. Apply Dual Tracking Rules based on Parsed Output
        when (parsedResult.scenario) {
            SmsScenario.SELF_PURCHASE -> {
                db.transactionDao().insert(TransactionEntity(
                    id = UUID.randomUUID().toString(),
                    type = "EXPENSE",
                    amount = parsedResult.deductedAmount ?: 0.0,
                    category = "PURCHASE",
                    source = if (sender == "830") "TELEBIRR" else "AIRTIME",
                    timestamp = timestamp,
                    reference = null
                ))
                parsedResult.packages.forEach { pkg ->
                    db.balancePackageDao().insertOrUpdate(pkg)
                }
            }

            SmsScenario.GIFT_SENT -> {
                db.transactionDao().insert(TransactionEntity(
                    id = UUID.randomUUID().toString(),
                    type = "EXPENSE",
                    amount = parsedResult.deductedAmount ?: 0.0,
                    category = "TRANSFER_SENT",
                    source = "AIRTIME",
                    timestamp = timestamp,
                    reference = null
                ))
            }

            SmsScenario.RECHARGE_OR_GIFT_RECEIVED -> {
                if (parsedResult.isRecharge) {
                    db.transactionDao().insert(TransactionEntity(
                        id = UUID.randomUUID().toString(),
                        type = "INCOME",
                        amount = parsedResult.addedAmount ?: 0.0,
                        category = "RECHARGE",
                        source = "AIRTIME",
                        timestamp = timestamp,
                        reference = null
                    ))
                } else {
                    parsedResult.packages.forEach { pkg ->
                        db.balancePackageDao().insertOrUpdate(pkg)
                    }
                }
            }

            SmsScenario.LOAN_TAKEN -> {
                db.transactionDao().insert(TransactionEntity(
                    id = UUID.randomUUID().toString(),
                    type = "INCOME",
                    amount = parsedResult.addedAmount ?: 0.0,
                    category = "LOAN",
                    source = "AIRTIME",
                    timestamp = timestamp,
                    reference = null
                ))
            }
            
            SmsScenario.UNKNOWN -> {}
        }
    }
}
