package com.ethiobalance.app.services

import android.util.Log
import com.ethiobalance.app.AppConstants
import com.ethiobalance.app.data.*
import com.ethiobalance.app.domain.model.SmsScenario
import com.ethiobalance.app.domain.usecase.ParseSmsUseCase
import kotlinx.coroutines.flow.first
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReconciliationEngine @Inject constructor(
    private val smsLogDao: SmsLogDao,
    private val transactionDao: TransactionDao,
    private val balancePackageDao: BalancePackageDao,
    private val transactionSourceDao: TransactionSourceDao,
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
        if (sender == null) {
            Log.w("ReconciliationEngine", "Aborting: Sender is null")
            return
        }

        val normalizedSender = normalizeSender(sender)
        Log.d("ReconciliationEngine", "Processing SMS from $sender (normalized: $normalizedSender), force: $forceReparse")
        
        // 1. Dedup check
        val bodyHash = body.hashCode()
        
        if (!forceReparse && smsLogDao.existsByHash(normalizedSender, timestamp, bodyHash)) {
            Log.d("ReconciliationEngine", "Ignoring duplicate SMS from $normalizedSender")
            return
        }
        
        // Check if we've already processed this exact transaction ID before
        val parsedResult = parseSmsUseCase(normalizedSender, body, timestamp)
        Log.d("ReconciliationEngine", "Parsed result confidence: ${parsedResult.confidence}, scenario: ${parsedResult.scenario}")

        if (parsedResult.confidence < 0.7f) {
            Log.w("ReconciliationEngine", "Confidence too low (${parsedResult.confidence}) for $normalizedSender. Body snippet: ${body.take(20)}...")
            return
        }
        
        // Generate transaction ID
        val transactionId = if (!parsedResult.reference.isNullOrBlank()) {
            UUID.nameUUIDFromBytes("${normalizedSender}-${parsedResult.reference}".toByteArray()).toString()
        } else if (parsedResult.addedAmount != null && parsedResult.addedAmount > 0) {
            UUID.nameUUIDFromBytes("${normalizedSender}-INCOME-${parsedResult.addedAmount}-${timestamp}".toByteArray()).toString()
        } else if (parsedResult.deductedAmount != null && parsedResult.deductedAmount > 0) {
            UUID.nameUUIDFromBytes("${normalizedSender}-EXPENSE-${parsedResult.deductedAmount}-${timestamp}".toByteArray()).toString()
        } else {
            val uniqueStr = "$normalizedSender-$timestamp-${body.hashCode()}"
            UUID.nameUUIDFromBytes(uniqueStr.toByteArray()).toString()
        }
        
        // Early duplicate check (skip for forceReparse — allows package refresh)
        if (!forceReparse && transactionDao.existsById(transactionId)) {
            Log.d("ReconciliationEngine", "Transaction $transactionId already exists")
            return
        }

        // Time-window dedup: reject if same (source, type, amount) exists within 60s
        if (!forceReparse) {
            val allSources = transactionSourceDao.getAllSources().first()
            val configuredSource = allSources.find { src ->
                src.senderId.split(",").map { it.trim() }.any { it.equals(normalizedSender, ignoreCase = true) }
            }
            val resolvedSrcForDedup = configuredSource?.abbreviation ?: AppConstants.resolveSource(normalizedSender)
            val txType = if ((parsedResult.addedAmount ?: 0.0) > 0) "INCOME" else "EXPENSE"
            val txAmount = parsedResult.addedAmount ?: parsedResult.deductedAmount ?: 0.0
            if (txAmount > 0 && transactionDao.existsNearDuplicate(resolvedSrcForDedup, txType, txAmount, timestamp, 60_000L)) {
                Log.d("ReconciliationEngine", "Near-duplicate detected: $resolvedSrcForDedup $txType $txAmount within 60s window. Skipping.")
                return
            }
        }
        
        Log.d("ReconciliationEngine", "Saving new transaction $transactionId from $normalizedSender")
        
        // 2. Log Raw SMS
        val logEntity = SmsLogEntity(
            sender = normalizedSender, 
            message = body, 
            parsedType = AppConstants.SMS_LOG_TYPE_PROCESSING, 
            confidence = parsedResult.confidence, 
            processed = true, 
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

        // 3. Save Transaction
        // Try to resolve the source to a user-configured abbreviation first
        val allSources = transactionSourceDao.getAllSources().first()
        val configuredSource = allSources.find { src ->
            src.senderId.split(",").map { it.trim() }.any { it.equals(normalizedSender, ignoreCase = true) }
        }
        val resolvedSrc = configuredSource?.abbreviation ?: AppConstants.resolveSource(normalizedSender)
        Log.d("ReconciliationEngine", "Resolved source: $resolvedSrc for $normalizedSender")

        when (parsedResult.scenario) {
            SmsScenario.SELF_PURCHASE -> {
                val entity = TransactionEntity(
                    id = transactionId,
                    type = "EXPENSE",
                    amount = parsedResult.deductedAmount ?: 0.0,
                    category = "PURCHASE",
                    source = resolvedSrc,
                    timestamp = timestamp,
                    reference = parsedResult.reference,
                    partyName = parsedResult.partyName,
                    transactionSubType = parsedResult.transactionSubType
                )
                transactionDao.insert(entity)
                Log.d("ReconciliationEngine", "✅ SAVED: SELF_PURCHASE transaction ${entity.id} - ${entity.amount} ETB")
                parsedResult.packages.forEach { 
                    balancePackageDao.insertOrUpdate(it) 
                    Log.d("ReconciliationEngine", "✅ SAVED: Balance package ${it.type} - ${it.remainingAmount} ${it.unit}")
                }
            }

            SmsScenario.EXPENSE -> {
                val entity = TransactionEntity(
                    id = transactionId,
                    type = "EXPENSE",
                    amount = parsedResult.deductedAmount ?: 0.0,
                    category = parsedResult.transactionCategory ?: "EXPENSE",
                    source = resolvedSrc,
                    timestamp = timestamp,
                    reference = parsedResult.reference,
                    partyName = parsedResult.partyName,
                    transactionSubType = parsedResult.transactionSubType
                )
                transactionDao.insert(entity)
                Log.d("ReconciliationEngine", "✅ SAVED: EXPENSE transaction ${entity.id} - ${entity.amount} ETB (${entity.category})")
                parsedResult.packages.forEach { 
                    balancePackageDao.insertOrUpdate(it) 
                    Log.d("ReconciliationEngine", "✅ SAVED: Balance package ${it.type} - ${it.remainingAmount} ${it.unit}")
                }
            }

            SmsScenario.GIFT_SENT -> {
                val entity = TransactionEntity(
                    id = transactionId,
                    type = "EXPENSE",
                    amount = parsedResult.deductedAmount ?: 0.0,
                    category = parsedResult.transactionCategory ?: "GIFT",
                    source = resolvedSrc,
                    timestamp = timestamp,
                    reference = parsedResult.reference,
                    partyName = parsedResult.partyName,
                    transactionSubType = parsedResult.transactionSubType
                )
                transactionDao.insert(entity)
                Log.d("ReconciliationEngine", "✅ SAVED: GIFT_SENT transaction ${entity.id} - ${entity.amount} ETB")
            }

            SmsScenario.RECHARGE_OR_GIFT_RECEIVED -> {
                val src = AppConstants.resolveSource(normalizedSender)
                if (src != AppConstants.SOURCE_AIRTIME) {
                    val entity = TransactionEntity(
                        id = transactionId,
                        type = "INCOME",
                        amount = parsedResult.addedAmount ?: 0.0,
                        category = "RECHARGE",
                        source = src,
                        timestamp = timestamp,
                        reference = parsedResult.reference,
                        partyName = parsedResult.partyName,
                        transactionSubType = parsedResult.transactionSubType
                    )
                    transactionDao.insert(entity)
                    Log.d("ReconciliationEngine", "✅ SAVED: RECHARGE transaction ${entity.id} - ${entity.amount} ETB")
                }
                parsedResult.packages.forEach { 
                    balancePackageDao.insertOrUpdate(it) 
                    Log.d("ReconciliationEngine", "✅ SAVED: Balance package ${it.type} - ${it.remainingAmount} ${it.unit}")
                }
            }

            SmsScenario.INCOME -> {
                val entity = TransactionEntity(
                    id = transactionId,
                    type = "INCOME",
                    amount = parsedResult.addedAmount ?: 0.0,
                    category = "CREDIT",
                    source = AppConstants.resolveSource(normalizedSender),
                    timestamp = timestamp,
                    reference = parsedResult.reference,
                    partyName = parsedResult.partyName,
                    transactionSubType = parsedResult.transactionSubType
                )
                transactionDao.insert(entity)
                Log.d("ReconciliationEngine", "✅ SAVED: INCOME transaction ${entity.id} - ${entity.amount} ETB")
            }

            SmsScenario.BALANCE_UPDATE, SmsScenario.BALANCE_QUERY -> {
                // Multi-segment 994 balance SMS → purge stale SMS-sourced telecom rows first.
                // Single-segment package SMS (e.g. "received Night Internet 600MB") is additive: no purge.
                if (parsedResult.isMultiSegmentBalance) {
                    balancePackageDao.deleteTelecomPackages()
                    Log.d("ReconciliationEngine", "🧹 PURGED stale telecom packages (multi-segment balance SMS)")
                }
                parsedResult.packages.forEach {
                    balancePackageDao.insertOrUpdate(it)
                    Log.d("ReconciliationEngine", "✅ SAVED: Balance package ${it.type}/${it.subType} - ${it.remainingAmount} ${it.unit}")
                }
            }

            else -> {
                Log.w("ReconciliationEngine", "❌ NO SAVE: Unknown scenario for $normalizedSender - no matching transaction type")
            }
        }
    }
}
