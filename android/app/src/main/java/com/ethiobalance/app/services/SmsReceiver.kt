package com.ethiobalance.app.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log

class SmsReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "SmsReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            for (message in messages) {
                val sender = message.displayOriginatingAddress
                val body = message.displayMessageBody
                val timestamp = message.timestampMillis

                Log.d(TAG, "SMS Received from: $sender, Body: $body")

                // Start Foreground Service to ensure processing continues if app is in background
                val serviceIntent = Intent(context, SmsForegroundService::class.java).apply {
                    putExtra("sender", sender)
                    putExtra("body", body)
                    putExtra("timestamp", timestamp)
                }
                context.startForegroundService(serviceIntent)
            }
        }
    }
}
