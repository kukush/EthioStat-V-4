package com.ethiobalance.app.repository

import android.content.Context
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
        val windowStart = System.currentTimeMillis() - (days * 24L * 60L * 60L * 1000L)
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
     * Full historical scan — covers BOTH:
     *  (a) User-configured sources in the transaction_sources table
     *  (b) The built-in SMS_SENDER_WHITELIST in AppConstants
     *
     * Using 90 days so the past 3 months of SMS are always captured on first run.
     * De-duplication inside ReconciliationEngine prevents double-counting.
     */
    suspend fun scanAllTransactionSources(days: Int = 90): Int = withContext(Dispatchers.IO) {
        // Merge user-configured senders with the hardcoded whitelist
        val configuredSenders = transactionSourceDao.getEnabledSenderIds().toSet()
        val allSenders = (configuredSenders + AppConstants.SMS_SENDER_WHITELIST).distinct()

        var totalScanned = 0
        for (senderId in allSenders) {
            totalScanned += scanHistory(senderId, days = days)
        }
        Log.d("SmsRepository", "scanAllTransactionSources done: $totalScanned total")
        totalScanned
    }

    /**
     * Specifically scans for EthioTelecom balance responses.
     * Includes common senders like 804, ETC, and other telecom numbers.
     */
    suspend fun scanTelecomHistory(days: Int = 1): Int = withContext(Dispatchers.IO) {
        val telecomSenders = setOf("804", "ETC", "EthioTelecom", "8181", "888", "808")
        var total = 0
        telecomSenders.forEach { total += scanHistory(it, days) }
        total
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
     * Send USSD automatically using TelephonyManager (Android O+).
     * No user interaction required - captures response via callback.
     */
    suspend fun sendUssdAuto(ussdCode: String): Result<String> = withContext(Dispatchers.Main) {
        try {
            val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            
            // Remove * and # from the code as sendUssdRequest doesn't need them
            val cleanCode = ussdCode.replace("*", "").replace("#", "")
            
            var ussdResult: Result<String>? = null
            val lock = Object()
            
            val callback = object : TelephonyManager.UssdResponseCallback() {
                override fun onReceiveUssdResponse(telephonyManager: TelephonyManager, request: String, response: CharSequence) {
                    synchronized(lock) {
                        ussdResult = Result.success(response.toString())
                        lock.notify()
                    }
                }
                
                override fun onReceiveUssdResponseFailed(telephonyManager: TelephonyManager, request: String, failureCode: Int) {
                    synchronized(lock) {
                        ussdResult = Result.failure(Exception("USSD failed with code: $failureCode"))
                        lock.notify()
                    }
                }
            }
            
            // Send USSD request
            telephonyManager.sendUssdRequest(cleanCode, callback, Handler(Looper.getMainLooper()))
            
            // Wait for response (max 15 seconds)
            synchronized(lock) {
                if (ussdResult == null) {
                    lock.wait(15000)
                }
            }
            
            ussdResult ?: Result.failure(Exception("USSD request timed out. Network may be unavailable."))
        } catch (e: SecurityException) {
            Result.failure(Exception("CALL_PHONE permission required for auto-USSD"))
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
