package com.ethiobalance.app.services

import com.ethiobalance.app.AppConstants
import com.ethiobalance.app.data.*
import com.ethiobalance.app.domain.model.SmsScenario
import com.ethiobalance.app.domain.usecase.ParseSmsUseCase
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReconciliationEngine @Inject constructor(
    private val smsLogDao: SmsLogDao,
    private val transactionDao: TransactionDao,
    private val balancePackageDao: BalancePackageDao,
    private val parseSmsUseCase: ParseSmsUseCase
) {

    fun normalizeSender(raw: String): String {
        var s = raw.trim()
        if (s.any { it.isLetter() }) return s.uppercase()
        if (s.startsWith("+")) s = s.substring(1)
        if (s.startsWith("251") && s.length > 3) {
            val remainder = s.substring(3)
            if (remainder.length <= 6) s = remainder
        }
        if (s.startsWith("0") && s.length > 1) s = s.substring(1)
        return s
    }
    
    suspend fun processSms(sender: String?, body: String, timestamp: Long, forceReparse: Boolean = false) {
        if (sender == null) return

        val normalizedSender = normalizeSender(sender)
        
        // 1. Dedup check
        val bodyHash = body.hashCode()
        
        if (!forceReparse && smsLogDao.existsByHash(normalizedSender, timestamp, bodyHash)) {
            return
        }
        
        // Check if we've already processed this exact transaction ID before
        // This is a safety net for when forceReparse=true
        val parsedResult = parseSmsUseCase(normalizedSender, body, timestamp)
        if (parsedResult.confidence < 0.7f) {
            return
        }
        
        // Generate potential transaction ID to check for duplicates early
        val potentialTransactionId = if (!parsedResult.reference.isNullOrBlank()) {
            UUID.nameUUIDFromBytes("${normalizedSender}-${parsedResult.reference}".toByteArray()).toString()
        } else if (parsedResult.addedAmount != null && parsedResult.addedAmount > 0) {
            UUID.nameUUIDFromBytes("${normalizedSender}-INCOME-${parsedResult.addedAmount}-${timestamp}".toByteArray()).toString()
        } else if (parsedResult.deductedAmount != null && parsedResult.deductedAmount > 0) {
            UUID.nameUUIDFromBytes("${normalizedSender}-EXPENSE-${parsedResult.deductedAmount}-${timestamp}".toByteArray()).toString()
        } else {
            val uniqueStr = "$normalizedSender-$timestamp-${body.hashCode()}"
            UUID.nameUUIDFromBytes(uniqueStr.toByteArray()).toString()
        }
        
        // Early duplicate check even for forceReparse
        if (transactionDao.existsById(potentialTransactionId)) {
            return
        }
        
        // 2. Log Raw SMS
        val logEntity = SmsLogEntity(
            sender = normalizedSender, 
            message = body, 
            parsedType = AppConstants.SMS_LOG_TYPE_PROCESSING, 
            confidence = 0f, 
            processed = false, 
            timestamp = timestamp, 
            bodyHash = bodyHash
        )
        
        if (forceReparse) {
            val existing = smsLogDao.getAllLogs().find { it.sender == normalizedSender && it.timestamp == timestamp && it.bodyHash == bodyHash }
            if (existing != null) {
                smsLogDao.insert(logEntity.copy(id = existing.id))
            } else {
                smsLogDao.insert(logEntity)
            }
        } else {
            smsLogDao.insert(logEntity)
        }

        // 3. Parse SMS via Use Case (already done above)
        // 4. Apply Dual Tracking Rules (already done above)
        val transactionId = potentialTransactionId
        val resolvedSrc = AppConstants.resolveSource(normalizedSender)

        when (parsedResult.scenario) {
            SmsScenario.SELF_PURCHASE -> {
                transactionDao.insert(TransactionEntity(
                    id = transactionId,
                    type = "EXPENSE",
                    amount = parsedResult.deductedAmount ?: 0.0,
                    category = "PURCHASE",
                    source = resolvedSrc,
                    timestamp = timestamp,
                    reference = parsedResult.reference,
                    partyName = parsedResult.partyName,
                    transactionSubType = parsedResult.transactionSubType
                ))
                parsedResult.packages.forEach { balancePackageDao.insertOrUpdate(it) }
            }

            SmsScenario.EXPENSE -> {
                transactionDao.insert(TransactionEntity(
                    id = transactionId,
                    type = "EXPENSE",
                    amount = parsedResult.deductedAmount ?: 0.0,
                    category = parsedResult.transactionCategory ?: "EXPENSE",
                    source = resolvedSrc,
                    timestamp = timestamp,
                    reference = parsedResult.reference,
                    partyName = parsedResult.partyName,
                    transactionSubType = parsedResult.transactionSubType
                ))
                parsedResult.packages.forEach { balancePackageDao.insertOrUpdate(it) }
            }

            SmsScenario.GIFT_SENT -> {
                transactionDao.insert(TransactionEntity(
                    id = transactionId,
                    type = "EXPENSE",
                    amount = parsedResult.deductedAmount ?: 0.0,
                    category = parsedResult.transactionCategory ?: "GIFT",
                    source = resolvedSrc,
                    timestamp = timestamp,
                    reference = parsedResult.reference,
                    partyName = parsedResult.partyName,
                    transactionSubType = parsedResult.transactionSubType
                ))
            }

            SmsScenario.RECHARGE_OR_GIFT_RECEIVED -> {
                val src = AppConstants.resolveSource(normalizedSender)
                if (src != AppConstants.SOURCE_AIRTIME) {
                    transactionDao.insert(TransactionEntity(
                        id = transactionId,
                        type = "INCOME",
                        amount = parsedResult.addedAmount ?: 0.0,
                        category = "RECHARGE",
                        source = src,
                        timestamp = timestamp,
                        reference = parsedResult.reference,
                        partyName = parsedResult.partyName,
                    transactionSubType = parsedResult.transactionSubType
                    ))
                }
                parsedResult.packages.forEach { balancePackageDao.insertOrUpdate(it) }
            }

            SmsScenario.INCOME -> {
                transactionDao.insert(TransactionEntity(
                    id = transactionId,
                    type = "INCOME",
                    amount = parsedResult.addedAmount ?: 0.0,
                    category = "CREDIT",
                    source = AppConstants.resolveSource(normalizedSender),
                    timestamp = timestamp,
                    reference = parsedResult.reference,
                    partyName = parsedResult.partyName,
                    transactionSubType = parsedResult.transactionSubType
                ))
            }

            SmsScenario.BALANCE_UPDATE, SmsScenario.BALANCE_QUERY -> {
                parsedResult.packages.forEach { balancePackageDao.insertOrUpdate(it) }
            }

            else -> {}
        }
    }
}
