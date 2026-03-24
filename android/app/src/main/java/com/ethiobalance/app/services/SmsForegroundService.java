package com.ethiobalance.app.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

public class SmsForegroundService extends Service {
    private static final String TAG = "SmsForegroundService";
    private static final String CHANNEL_ID = "SmsMonitorChannel";

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String sender = intent.getStringExtra("sender");
        String body = intent.getStringExtra("body");

        Log.d(TAG, "Processing SMS from " + sender + " in foreground service");

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("EthioStat Monitoring")
                .setContentText("Processing message from " + sender)
                .setSmallIcon(android.R.drawable.stat_notify_chat)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();

        startForeground(1, notification);

        // Logic for further processing or notifying Capacitor listeners would go here
        
        // Use START_STICKY so the service is restarted if killed by the OS
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createNotificationChannel() {
        NotificationChannel serviceChannel = new NotificationChannel(
                CHANNEL_ID,
                "SMS Monitor Service Channel",
                NotificationManager.IMPORTANCE_LOW
        );
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.createNotificationChannel(serviceChannel);
        }
    }
}
