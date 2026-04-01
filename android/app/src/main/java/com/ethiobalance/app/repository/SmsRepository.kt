package com.ethiobalance.app.repository

import android.content.Context
import android.provider.Telephony
import android.util.Log
import com.ethiobalance.app.AppConstants
import com.ethiobalance.app.data.AppDatabase
import com.ethiobalance.app.services.ReconciliationEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SmsRepository(private val context: Context) {

    private val db = AppDatabase.getDatabase(context)

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
    suspend fun scanHistory(senderId: String, days: Int = 90): Int = withContext(Dispatchers.IO) {
        val normalized = ReconciliationEngine.normalizeSender(senderId)
        val lastTimestamp = db.smsLogDao().getLastTimestampForSender(normalized)

        // Use the OLDER of: (now - days) vs last known scan timestamp.
        // This ensures a wider first-run or a re-widened window is honoured.
        val windowStart = System.currentTimeMillis() - (days * 24L * 60L * 60L * 1000L)
        val cutoffTime = if (lastTimestamp != null) minOf(lastTimestamp, windowStart) else windowStart

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
            // Alpha sender — exact match (LIKE would be too broad e.g. "CBE" matching "CBEBirr")
            selection = "address = ? AND date > ?"
            selectionArgs = arrayOf(senderId, cutoffTime.toString())
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
                    ReconciliationEngine.processSms(sender, body, timestamp, db)
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
     * Full historical scan — covers BOTH:
     *  (a) User-configured sources in the transaction_sources table
     *  (b) The built-in SMS_SENDER_WHITELIST in AppConstants
     *
     * Using 90 days so the past 3 months of SMS are always captured on first run.
     * De-duplication inside ReconciliationEngine prevents double-counting.
     */
    suspend fun scanAllTransactionSources(days: Int = 90): Int = withContext(Dispatchers.IO) {
        // Merge user-configured senders with the hardcoded whitelist
        val configuredSenders = db.transactionSourceDao().getEnabledSenderIds().toSet()
        val allSenders = (configuredSenders + AppConstants.SMS_SENDER_WHITELIST).distinct()

        var totalScanned = 0
        allSenders.forEach { senderId ->
            totalScanned += scanHistory(senderId, days = days)
        }
        Log.d("SmsRepository", "scanAllTransactionSources done: $totalScanned total")
        totalScanned
    }

    /**
     * Send USSD request using ACTION_CALL Intent.
     */
    fun sendUssdIntent(ussdCode: String): Result<String> {
        return try {
            val encodedCode = ussdCode.replace("#", "%23")
            val uri = android.net.Uri.parse("tel:$encodedCode")
            val intent = android.content.Intent(android.content.Intent.ACTION_CALL, uri).apply {
                flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            Result.success("USSD initiated successfully")
        } catch (e: Exception) {
            Result.failure(e)
        }
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
