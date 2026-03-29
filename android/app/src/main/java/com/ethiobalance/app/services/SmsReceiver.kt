package com.ethiobalance.app.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.ethiobalance.app.AppConstants

class SmsReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "SmsReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            for (message in messages) {
                val sender = message.displayOriginatingAddress ?: ""
                val body = message.displayMessageBody
                val timestamp = message.timestampMillis

                Log.d(TAG, "SMS Received from: $sender")

                // Filter: Check both system whitelist and user-defined transaction sources
                val prefs = context.getSharedPreferences("ethio_balance_prefs", Context.MODE_PRIVATE)
                val userWhitelist = prefs.getStringSet("sms_whitelist", emptySet()) ?: emptySet()
                
                val isWhitelisted = sender.contains("TELEBIRR", ignoreCase = true) ||
                                  AppConstants.SMS_SENDER_WHITELIST.contains(sender) ||
                                  userWhitelist.contains(sender)

                if (isWhitelisted) {
                    Log.d(TAG, "Sender $sender is whitelisted. Starting processing...")
                    // Start Foreground Service to ensure processing continues if app is in background
                    val serviceIntent = Intent(context, SmsForegroundService::class.java).apply {
                        putExtra("sender", sender)
                        putExtra("body", body)
                        putExtra("timestamp", timestamp)
                    }
                    context.startForegroundService(serviceIntent)
                } else {
                    Log.d(TAG, "Sender $sender is NOT whitelisted. Ignoring message.")
                }
            }
        }
    }
}
