package com.ethiobalance.app.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.util.Log;

import com.ethiobalance.app.data.AppDatabase;
import com.ethiobalance.app.data.SmsEntity;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class SmsReceiver extends BroadcastReceiver {
    private static final String TAG = "SmsReceiver";
    private final Executor executor = Executors.newSingleThreadExecutor();

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction() != null && intent.getAction().equals("android.provider.Telephony.SMS_RECEIVED")) {
            Bundle bundle = intent.getExtras();
            if (bundle != null) {
                Object[] pdus = (Object[]) bundle.get("pdus");
                if (pdus != null) {
                    for (Object pdu : pdus) {
                        SmsMessage smsMessage = SmsMessage.createFromPdu((byte[]) pdu);
                        String sender = smsMessage.getDisplayOriginatingAddress();
                        String body = smsMessage.getDisplayMessageBody();
                        long timestamp = smsMessage.getTimestampMillis();
                        
                        Log.d(TAG, "SMS Received from: " + sender + ", Body: " + body);

                        // Persist to Room DB in background thread
                        executor.execute(() -> {
                            AppDatabase db = AppDatabase.getDatabase(context);
                            db.smsDao().insert(new SmsEntity(sender, body, timestamp, 0));
                            
                            // Start Foreground Service to ensure processing continues if app is in background
                            Intent serviceIntent = new Intent(context, SmsForegroundService.class);
                            serviceIntent.putExtra("sender", sender);
                            serviceIntent.putExtra("body", body);
                            context.startForegroundService(serviceIntent);
                        });
                    }
                }
            }
        }
    }
}
