package com.ethiobalance.app.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Telephony
import android.util.Log
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
    companion object {
        private const val TAG = "SmsReceiver"
    }

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

                // A sender is whitelisted if:
                //  (a) resolveSource() maps it to a known bank/service (i.e. doesn't fall back to itself), OR
                //  (b) it is in the user's custom whitelist.
                // This handles all case variants: "Awash", "AWASH", "AwashBank", "901" all pass.
                val resolvedSender = AppConstants.resolveSource(sender)
                val isKnownSource = resolvedSender != sender.trim().uppercase()
                val isWhitelisted = sender.isNotEmpty() && (
                    isKnownSource ||
                    userWhitelist.any { it.equals(sender, ignoreCase = true) }
                )

                Log.d(TAG, "Checking sender: $sender, isWhitelisted: $isWhitelisted, segments: ${messages.count { (it.displayOriginatingAddress ?: "") == sender }}")

                if (isWhitelisted) {
                    Log.d(TAG, "Triggering foreground service for $sender")
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
                } else {
                    Log.d(TAG, "Sender $sender is NOT whitelisted. Ignoring message.")
                }
            }
            }
            AppConstants.ACTION_TRIGGER_REFRESH -> {
                val scanDepth = intent.getIntExtra("scan_depth", 5)
                Log.d(TAG, "Received TRIGGER_REFRESH broadcast, scanDepth=$scanDepth")
                if (::smsRepository.isInitialized) {
                    scope.launch {
                        val count = smsRepository.refreshTelecomSmart(scanDepth)
                        Log.d(TAG, "TRIGGER_REFRESH completed, processed $count SMS")
                    }
                } else {
                    Log.w(TAG, "smsRepository not initialized, cannot refresh")
                }
            }
        }
    }
}
