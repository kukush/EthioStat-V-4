package com.ethiobalance.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.ethiobalance.app.repository.SettingsRepository
import com.ethiobalance.app.services.SmsForegroundService
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Debug-only BroadcastReceivers for screenshot automation.
 * Compiled only in debug builds (src/debug). Registered via src/debug/AndroidManifest.xml.
 */

@EntryPoint
@InstallIn(SingletonComponent::class)
interface DebugEntryPoint {
    fun settingsRepository(): SettingsRepository
}

private fun settingsRepo(context: Context): SettingsRepository =
    EntryPointAccessors.fromApplication(context.applicationContext, DebugEntryPoint::class.java)
        .settingsRepository()

class DebugSmsSeedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val source = intent.getStringExtra("source") ?: "CBE"
        val body = intent.getStringExtra("body")
            ?: "Dear Customer, your Account has been Credited with ETB 5000.00. Balance: ETB 15000.00."

        // Forward to SmsForegroundService which handles parsing + DB insert
        val svcIntent = Intent(context, SmsForegroundService::class.java).apply {
            putExtra("sender", getSenderForSource(source))
            putExtra("body", body)
            putExtra("timestamp", System.currentTimeMillis())
        }
        context.startForegroundService(svcIntent)
    }

    private fun getSenderForSource(source: String): String = when (source.uppercase()) {
        "CBE" -> "847"
        "TELEBIRR" -> "127"
        "AWASH" -> "Awash"
        "DASHEN" -> "DashenBank"
        "BOA" -> "BOA"
        else -> "847"
    }
}

class DebugSetLanguageReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val lang = intent.getStringExtra("lang") ?: return
        CoroutineScope(Dispatchers.IO).launch {
            settingsRepo(context).setLanguage(lang)
        }
    }
}
