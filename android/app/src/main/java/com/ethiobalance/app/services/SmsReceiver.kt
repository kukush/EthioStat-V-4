package com.ethiobalance.app.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Telephony
import com.ethiobalance.app.AppConstants
import com.ethiobalance.app.repository.SmsRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SmsReceiver : BroadcastReceiver() {
    @Inject
    lateinit var smsRepository: SmsRepository

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Telephony.Sms.Intents.SMS_RECEIVED_ACTION -> {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)

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

                if (isWhitelisted) {
                    val serviceIntent = Intent(context, SmsForegroundService::class.java).apply {
                        putExtra("sender", sender)
                        putExtra("body", body)
                        putExtra("timestamp", timestamp)
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(serviceIntent)
                    } else {
                        context.startService(serviceIntent)
                    }
                }
            }
            }
            AppConstants.ACTION_TRIGGER_REFRESH -> {
                val scanDepth = intent.getIntExtra("scan_depth", 5)
                if (::smsRepository.isInitialized) {
                    scope.launch { smsRepository.refreshTelecomSmart(scanDepth) }
                }
            }
        }
    }
}
