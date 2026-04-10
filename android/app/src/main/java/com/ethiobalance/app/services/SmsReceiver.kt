package com.ethiobalance.app.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
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



                // Filter: Check both system whitelist and user-defined transaction sources
                val prefs = context.getSharedPreferences("ethio_balance_prefs", Context.MODE_PRIVATE)
                val userWhitelist = prefs.getStringSet("sms_whitelist", emptySet()) ?: emptySet()
                
                val isWhitelisted = sender.isNotEmpty() && (
                                  sender.contains("TELEBIRR", ignoreCase = true) ||
                                  sender.contains("CBE", ignoreCase = true) ||
                                  sender.contains("AWASH", ignoreCase = true) ||
                                  sender.contains("DASHEN", ignoreCase = true) ||
                                  AppConstants.SMS_SENDER_WHITELIST.any { it.equals(sender, ignoreCase = true) } ||
                                  userWhitelist.any { it.equals(sender, ignoreCase = true) }
                )

                Log.d(TAG, "Checking sender: $sender, isWhitelisted: $isWhitelisted")

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
    }
}
