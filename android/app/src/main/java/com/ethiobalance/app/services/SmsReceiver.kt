package com.ethiobalance.app.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Telephony
import android.util.Log
import com.ethiobalance.app.AppConstants
import com.ethiobalance.app.services.SmsForegroundService // Keeping for manual scan reference if needed, else remove
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.app.NotificationManager
import android.app.PendingIntent
import androidx.core.app.NotificationCompat

@AndroidEntryPoint
class SmsReceiver : BroadcastReceiver() {
    @Inject
    lateinit var reconciliationEngine: ReconciliationEngine

    init {
        Log.w(TAG, "SmsReceiver instantiated")
    }
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onReceive(context: Context, intent: Intent) {
        Log.w(TAG, "onReceive: action=${intent.action}")
        when (intent.action) {
            Telephony.Sms.Intents.SMS_RECEIVED_ACTION -> {
                val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
                Log.w(TAG, "SMS_RECEIVED: ${messages.size} message parts")

                // Concatenate multi-part SMS segments from the same sender to avoid
                // processing each segment as a separate transaction.
                val grouped = mutableMapOf<String, Pair<StringBuilder, Long>>()
                for (message in messages) {
                    val sender = message.displayOriginatingAddress ?: ""
                    val body = message.displayMessageBody ?: ""
                    val timestamp = message.timestampMillis
                    val existing = grouped[sender]
                    if (existing != null) {
                        existing.first.append(body)
                    } else {
                        grouped[sender] = Pair(StringBuilder(body), timestamp)
                    }
                }

                val prefs = context.getSharedPreferences(AppConstants.PREFS_NAME, Context.MODE_PRIVATE)
                val userWhitelist = prefs.getStringSet(AppConstants.PREF_KEY_SMS_WHITELIST, emptySet()) ?: emptySet()

                for ((sender, pair) in grouped) {
                    val body = pair.first.toString()
                    val timestamp = pair.second

                    // A sender is whitelisted ONLY if:
                    //  (a) it is in the DB-backed user whitelist (which includes all variants for configured sources), OR
                    //  (b) it is a telecom sender (994, 804, etc.)
                    // NOTE: The broad resolveSource() gate has been removed per project standards.
                    // Only configured transaction sources + telecom are accepted.
                    val upperSender = sender.trim().uppercase()
                    val isTelecomSender = AppConstants.TELECOM_SENDERS.any {
                        it.equals(sender, ignoreCase = true) || it == upperSender
                    }
                    val isInUserWhitelist = userWhitelist.any { it.equals(sender, ignoreCase = true) }
                    val isWhitelisted = sender.isNotEmpty() && (isInUserWhitelist || isTelecomSender)

                    Log.w(TAG, "sender=$sender isTelecom=$isTelecomSender inWhitelist=$isInUserWhitelist whitelisted=$isWhitelisted")

                    if (isWhitelisted) {
                        val pendingResult = goAsync()
                        scope.launch {
                            try {
                                Log.w(TAG, "processSms start: sender=$sender")
                                reconciliationEngine.processSms(sender, body, timestamp)
                                Log.w(TAG, "processSms completed for $sender")

                                if (isTelecomSender) {
                                    Log.w(TAG, "Sending ACTION_TELECOM_SMS_ARRIVED for $sender")
                                    context.sendBroadcast(Intent(AppConstants.ACTION_TELECOM_SMS_ARRIVED).setPackage(context.packageName))

                                    // Show heads-up notification so user can tap to return
                                    val bringBack = Intent(context, Class.forName("com.ethiobalance.app.MainActivity"))
                                    bringBack.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or
                                            Intent.FLAG_ACTIVITY_SINGLE_TOP or
                                            Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                                    val pendingIntent = PendingIntent.getActivity(
                                        context, 0, bringBack,
                                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                                    )
                                    val headsUp = NotificationCompat.Builder(context, SmsForegroundService.SYNC_CHANNEL_ID)
                                        .setContentTitle("EthioStat")
                                        .setContentText("Telecom data updated — tap to return")
                                        .setSmallIcon(android.R.drawable.stat_notify_chat)
                                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                                        .setContentIntent(pendingIntent)
                                        .setAutoCancel(true)
                                        .build()
                                    val nm = context.getSystemService(NotificationManager::class.java)
                                    nm?.notify(AppConstants.NOTIFICATION_ID_SMS + 1, headsUp)
                                    Log.w(TAG, "Heads-up notification posted")
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "processSms failed: ${e.message}")
                            } finally {
                                pendingResult.finish()
                            }
                        }
                    }
                }
            }
        }
    }

    companion object {
        private const val TAG = "SmsReceiver"
    }
}
