package com.ethiobalance.app.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.ethiobalance.app.repository.SettingsRepository
import com.ethiobalance.app.repository.SmsRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import com.ethiobalance.app.data.AppDatabase
import com.ethiobalance.app.AppConstants

@AndroidEntryPoint
class SmsForegroundService : Service() {
    companion object {
        const val SYNC_CHANNEL_ID = "SyncNotificationChannel"
        private const val TAG = "SmsForegroundService"
    }

    @Inject
    lateinit var settingsRepository: SettingsRepository

    @Inject
    lateinit var smsRepository: SmsRepository

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand: Starting manual SMS scan")

        val notification = NotificationCompat.Builder(this, AppConstants.NOTIFICATION_CHANNEL_ID)
            .setContentTitle("EthioStat")
            .setContentText("Scanning transaction history...")
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(AppConstants.NOTIFICATION_ID_SMS, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SHORT_SERVICE)
            } else {
                startForeground(AppConstants.NOTIFICATION_ID_SMS, notification)
            }
            Log.d(TAG, "startForeground succeeded")
        } catch (e: Exception) {
            Log.e(TAG, "startForeground failed: ${e.message}")
        }

        scope.launch {
            try {
                Log.d(TAG, "Manual scan started")
                val lastScanned = settingsRepository.lastScannedTimestamp.first()
                val now = System.currentTimeMillis()
                
                val daysSinceLastScan = if (lastScanned == 0L) {
                    90
                } else {
                    ((now - lastScanned) / AppConstants.MILLISECONDS_PER_DAY).toInt() + 1
                }
                
                val messagesScanned = smsRepository.scanAllTransactionSources(days = daysSinceLastScan)
                Log.d(TAG, "Manual scan completed. Scanned $messagesScanned messages.")
                
                settingsRepository.setLastScannedTimestamp(now)
            } catch (e: Exception) {
                Log.e(TAG, "Manual scan failed: ${e.message}")
            } finally {
                stopSelf(startId)
            }
        }

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onTimeout(startId: Int) {
        stopSelf(startId)
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            val serviceChannel = NotificationChannel(
                AppConstants.NOTIFICATION_CHANNEL_ID,
                AppConstants.NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            )
            manager?.createNotificationChannel(serviceChannel)
            val syncChannel = NotificationChannel(
                SYNC_CHANNEL_ID,
                "Sync Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "Notifies when telecom data sync completes" }
            manager?.createNotificationChannel(syncChannel)
        }
    }
}
