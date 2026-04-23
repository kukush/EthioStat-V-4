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
import com.ethiobalance.app.AppConstants
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

@AndroidEntryPoint
class SmsForegroundService : Service() {
    companion object {
        private const val TAG = "SmsForegroundService"
        const val SYNC_CHANNEL_ID = "SyncNotificationChannel"
    }

    @Inject
    lateinit var reconciliationEngine: ReconciliationEngine

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val sender = intent?.getStringExtra("sender")
        val body = intent?.getStringExtra("body")
        val timestamp = intent?.getLongExtra("timestamp", System.currentTimeMillis()) ?: System.currentTimeMillis()

        if (sender != null && body != null) {
            Log.d(TAG, "Processing SMS from $sender in foreground service")

            val notification = NotificationCompat.Builder(this, AppConstants.NOTIFICATION_CHANNEL_ID)
                .setContentTitle("EthioStat")
                .setContentText("Processing message from $sender...")
                .setSmallIcon(android.R.drawable.stat_notify_chat)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build()

            try {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    startForeground(AppConstants.NOTIFICATION_ID_SMS, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
                } else {
                    startForeground(AppConstants.NOTIFICATION_ID_SMS, notification)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start foreground service: ${e.message}", e)
            }

            // Process with Dual Tracking Engine, then stop the service when done
            scope.launch {
                try {
                    reconciliationEngine.processSms(sender, body, timestamp)
                    Log.d(TAG, "SMS from $sender processed successfully")

                    // If this was a telecom sender (994, 804, etc.), notify the app
                    // so it can auto-return from the dialer after a sync
                    val isTelecom = AppConstants.TELECOM_SENDERS.any {
                        it.equals(sender, ignoreCase = true)
                    }
                    if (isTelecom) {
                        Log.d(TAG, "Telecom SMS processed — notifying app")
                        sendBroadcast(Intent(AppConstants.ACTION_TELECOM_SMS_ARRIVED).setPackage(packageName))

                        // Show heads-up notification so user can tap to return
                        val bringBack = Intent(this@SmsForegroundService, com.ethiobalance.app.MainActivity::class.java)
                        bringBack.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or
                                Intent.FLAG_ACTIVITY_SINGLE_TOP or
                                Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                        val pendingIntent = android.app.PendingIntent.getActivity(
                            this@SmsForegroundService, 0, bringBack,
                            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
                        )
                        val headsUp = NotificationCompat.Builder(this@SmsForegroundService, SYNC_CHANNEL_ID)
                            .setContentTitle("EthioStat")
                            .setContentText("Telecom data updated — tap to return")
                            .setSmallIcon(android.R.drawable.stat_notify_chat)
                            .setPriority(NotificationCompat.PRIORITY_HIGH)
                            .setContentIntent(pendingIntent)
                            .setAutoCancel(true)
                            .build()
                        val nm = getSystemService(NotificationManager::class.java)
                        nm?.notify(AppConstants.NOTIFICATION_ID_SMS + 1, headsUp)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing SMS from $sender: ${e.message}", e)
                } finally {
                    stopSelf(startId)
                }
            }
        } else {
            stopSelf(startId)
        }

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
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
