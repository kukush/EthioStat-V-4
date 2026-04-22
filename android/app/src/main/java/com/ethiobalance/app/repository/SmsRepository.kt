package com.ethiobalance.app.repository

import android.content.Context
import android.os.Build
import android.provider.Telephony
import android.telephony.TelephonyManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.ethiobalance.app.AppConstants
import com.ethiobalance.app.data.AppDatabase
import com.ethiobalance.app.services.ReconciliationEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import com.ethiobalance.app.data.SmsLogDao
import com.ethiobalance.app.data.TransactionSourceDao
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class SmsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val smsLogDao: SmsLogDao,
    private val transactionSourceDao: TransactionSourceDao,
    private val reconciliationEngine: ReconciliationEngine
) {

    /**
     * Scan the SMS Inbox for a given sender address.
     *
     * Strategy:
     * - First run (no prior log): look back [days] days.
     * - Subsequent runs: look from [days] ago OR from the last processed timestamp,
     *   whichever is OLDER — this guarantees we never miss messages when the
     *   lookback window is widened after an update.
     *
     * The address LIKE clause uses both the raw sender and the normalized form
     * so that "+251127", "251127", "0127", and "127" are all matched.
     */
    suspend fun scanHistory(senderId: String, days: Int = 90, forceReparse: Boolean = false): Int = withContext(Dispatchers.IO) {
        val normalized = reconciliationEngine.normalizeSender(senderId)
        val lastTimestamp = smsLogDao.getLastTimestampForSender(normalized)

        // Ensure we scan at least the last 90 days on initial run.
        // On subsequent runs, overlap slightly or start from the last processed timestamp.
        val windowStart = System.currentTimeMillis() - (days * AppConstants.MILLISECONDS_PER_DAY)
        val cutoffTime = if (lastTimestamp == null || forceReparse) {
            windowStart 
        } else {
            // Take the EARLIER of (last known message) or (90 days ago).
            // This ensures if the window is widened to 90 days, we don't skip old messages.
            minOf(lastTimestamp, windowStart)
        }

        Log.d("SmsRepository", "Scanning sender=$senderId (normalized=$normalized) cutoff=${java.util.Date(cutoffTime)}")

        val uri = Telephony.Sms.Inbox.CONTENT_URI
        val projection = arrayOf("address", "body", "date")

        // Match exact short-codes AND country-code prefixed variants:
        //   "127" → matches "127", "+251127", "251127", "0127"
        //   "CBEBirr" → matches itself directly
        val isNumeric = senderId.all { it.isDigit() }
        val selectionArgs: Array<String>
        val selection: String
        if (isNumeric) {
            selection = "(address = ? OR address = ? OR address = ? OR address = ?) AND date > ?"
            selectionArgs = arrayOf(
                senderId,
                "+251$senderId",
                "251$senderId",
                "0$senderId",
                cutoffTime.toString()
            )
        } else {
            // Alpha sender — case-insensitive match.
            // Android SMS content provider stores the original-case address (e.g. "AwashBank"),
            // but normalizeSender() uppercases it ("AWASHBANK"). We must match both cases.
            // LIKE is safe here because we are matching the full sender string, not a prefix.
            selection = "(address = ? OR upper(address) = upper(?)) AND date > ?"
            selectionArgs = arrayOf(senderId, senderId, cutoffTime.toString())
        }

        val cursor = context.contentResolver.query(
            uri, projection, selection, selectionArgs, "date ASC"
        )

        var matchCount = 0
        cursor?.use {
            val addressIdx = it.getColumnIndex("address")
            val bodyIdx    = it.getColumnIndex("body")
            val dateIdx    = it.getColumnIndex("date")

            while (it.moveToNext()) {
                val sender    = it.getString(addressIdx) ?: continue
                val body      = it.getString(bodyIdx)    ?: continue
                val timestamp = it.getLong(dateIdx)
                try {
                    reconciliationEngine.processSms(sender, body, timestamp, forceReparse)
                    matchCount++
                } catch (e: Exception) {
                    Log.e("SmsRepository", "Failed to process SMS from $sender: ${e.message}")
                }
            }
        }

        Log.d("SmsRepository", "Scanned sender=$senderId (cutoff=$cutoffTime): $matchCount messages")
        matchCount
    }

    /**
     * Full historical scan — scans ONLY user-configured sources in transaction_sources table.
     * Does NOT merge with SMS_SENDER_WHITELIST (per project standards).
     *
     * Using 90 days so the past 3 months of SMS are captured on first run.
     * De-duplication inside ReconciliationEngine prevents double-counting.
     */
    suspend fun scanAllTransactionSources(days: Int = 90): Int = withContext(Dispatchers.IO) {
        // Scan only configured sources from DB — no hardcoded whitelist union
        // Exclude telecom senders — packages are handled by refreshTelecomSmart()
        val telecomExclude = AppConstants.TELECOM_SENDERS
        val configuredSenders = transactionSourceDao.getEnabledSenderIdsFlattened().toSet()
        val allSenders = configuredSenders.filter { it !in telecomExclude }

        var totalScanned = 0
        for (senderId in allSenders) {
            totalScanned += scanHistory(senderId, days = days)
        }
        Log.d("SmsRepository", "scanAllTransactionSources done: $totalScanned total (sources: ${allSenders.size})")
        totalScanned
    }

    /**
     * Specifically scans for EthioTelecom balance responses.
     * Includes common senders like 804, ETC, and other telecom numbers.
     */
    suspend fun scanTelecomHistory(days: Int = 1): Int = withContext(Dispatchers.IO) {
        var total = 0
        AppConstants.TELECOM_SENDERS.take(2).forEach { total += scanHistory(it, days) }
        total
    }

    /**
     * Kept for backwards compatibility — delegates to [refreshTelecomSmart].
     */
    suspend fun refreshTelecomFromLatestSms(limit: Int = 2): Int =
        refreshTelecomSmart(scanDepth = limit.coerceAtLeast(2))

    companion object {
        /** True if [body] is the multi-segment 994 balance format (has ";" + "from " + "expiry date on"). */
        fun isMultiSegmentBalance(body: String): Boolean =
            Regex(";\\s+from ", RegexOption.IGNORE_CASE).containsMatchIn(body) &&
                body.contains("expiry date on", ignoreCase = true)

        data class SmsRow(val sender: String, val body: String, val ts: Long)

        /**
         * Pure decision logic for [refreshTelecomSmart]. Given a DESC-ordered list of 994 SMS rows,
         * returns the subset to process in order:
         *  - latest multi-segment       → [latest]
         *  - latest single + prior multi → [priorMulti, latest]
         *  - latest single + no prior    → [latest]
         *  - empty                       → []
         */
        fun pickRefreshTargets(rowsNewestFirst: List<SmsRow>): List<SmsRow> {
            if (rowsNewestFirst.isEmpty()) return emptyList()
            val latest = rowsNewestFirst.first()
            if (isMultiSegmentBalance(latest.body)) return listOf(latest)
            val priorMulti = rowsNewestFirst.drop(1).firstOrNull { isMultiSegmentBalance(it.body) }
            return if (priorMulti != null) listOf(priorMulti, latest) else listOf(latest)
        }
    }

    /**
     * Smart telecom refresh:
     *   - Latest 994 SMS is multi-segment       → process it (purge + insert).
     *   - Latest is single-segment (e.g. "received Night Internet 600MB")
     *       → find the most recent prior multi-segment SMS within [scanDepth] entries;
     *         process the multi-segment first (purge + insert), then the latest single
     *         (additive merge).
     *   - No 994 SMS at all                     → no-op.
     */
    suspend fun refreshTelecomSmart(scanDepth: Int = 5): Int = withContext(Dispatchers.IO) {
        val uri = Telephony.Sms.Inbox.CONTENT_URI
        val projection = arrayOf("address", "body", "date")
        val selection = AppConstants.TELECOM_SENDERS.joinToString(" OR ") { "address = ?" }.let { "($it)" }
        val selectionArgs = AppConstants.TELECOM_SENDERS.toTypedArray()

        val rows = mutableListOf<SmsRow>()
        context.contentResolver.query(
            uri, projection, selection, selectionArgs, "date DESC LIMIT $scanDepth"
        )?.use { c ->
            val ai = c.getColumnIndex("address")
            val bi = c.getColumnIndex("body")
            val di = c.getColumnIndex("date")
            while (c.moveToNext()) {
                val a = c.getString(ai) ?: continue
                val b = c.getString(bi) ?: continue
                rows += SmsRow(a, b, c.getLong(di))
            }
        }

        val targets = pickRefreshTargets(rows)
        if (targets.isEmpty()) {
            Log.d("SmsRepository", "refreshTelecomSmart: no 994 SMS to process")
            return@withContext 0
        }
        targets.forEach { r ->
            reconciliationEngine.processSms(r.sender, r.body, r.ts, forceReparse = true)
            Log.d("SmsRepository", "refreshTelecomSmart: processed SMS ts=${r.ts} multi=${isMultiSegmentBalance(r.body)}")
        }
        targets.size
    }

    /**
     * Dial a USSD code using ACTION_DIAL (no CALL_PHONE permission needed).
     */
    fun dialUssd(code: String) {
        val encodedCode = code.replace("#", "%23")
        val uri = android.net.Uri.parse("tel:$encodedCode")
        val intent = android.content.Intent(android.content.Intent.ACTION_DIAL, uri).apply {
            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }
}
