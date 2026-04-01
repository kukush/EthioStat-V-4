package com.ethiobalance.app.repository

import android.content.Context
import android.provider.Telephony
import android.telephony.TelephonyManager
import android.os.Handler
import android.os.Looper
import com.ethiobalance.app.AppConstants
import com.ethiobalance.app.data.AppDatabase
import com.ethiobalance.app.services.ReconciliationEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SmsRepository(private val context: Context) {

    private val db = AppDatabase.getDatabase(context)

    /**
     * Scan SMS history for a given sender, starting from the last scanned timestamp
     * to avoid reprocessing old messages.
     */
    suspend fun scanHistory(senderId: String, days: Int = 7): Int = withContext(Dispatchers.IO) {
        val lastTimestamp = db.smsLogDao().getLastTimestampForSender(
            ReconciliationEngine.normalizeSender(senderId)
        )
        val cutoffTime = lastTimestamp
            ?: (System.currentTimeMillis() - (days * 24L * 60L * 60L * 1000L))

        val uri = Telephony.Sms.Inbox.CONTENT_URI
        val projection = arrayOf("address", "body", "date")
        val selection = "address LIKE ? AND date > ?"
        val selectionArgs = arrayOf("%$senderId%", cutoffTime.toString())

        val cursor = context.contentResolver.query(uri, projection, selection, selectionArgs, "date ASC")
        var matchCount = 0

        cursor?.use {
            val addressIndex = it.getColumnIndex("address")
            val bodyIndex = it.getColumnIndex("body")
            val dateIndex = it.getColumnIndex("date")

            while (it.moveToNext()) {
                val sender = it.getString(addressIndex)
                val body = it.getString(bodyIndex)
                val timestamp = it.getLong(dateIndex)
                ReconciliationEngine.processSms(sender, body, timestamp, db)
                matchCount++
            }
        }
        matchCount
    }

    /**
     * Send USSD request using Intent (no CALL_PHONE permission required).
     * Uses ACTION_CALL with USSD code - works on most Android versions without runtime permission.
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
     * Scan SMS for all configured transaction sources.
     */
    suspend fun scanAllTransactionSources(): Int = withContext(Dispatchers.IO) {
        val sources = db.transactionSourceDao().getEnabledSenderIds()
        var totalScanned = 0
        sources.forEach { senderId ->
            totalScanned += scanHistory(senderId, days = 7)
        }
        totalScanned
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
