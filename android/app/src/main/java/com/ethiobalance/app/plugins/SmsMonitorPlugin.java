package com.ethiobalance.app.plugins;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

@CapacitorPlugin(name = "SmsMonitor")
public class SmsMonitorPlugin extends Plugin {

    private BroadcastReceiver ussdReceiver;

    @Override
    public void load() {
        super.load();
        // Listen for internal USSD broadcasts from our AccessibilityService
        ussdReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if ("com.ethiobalance.app.ACTION_USSD_RESPONSE".equals(intent.getAction())) {
                    String text = intent.getStringExtra("ussd_text");
                    JSObject ret = new JSObject();
                    ret.put("text", text);
                    notifyListeners("ussdReceived", ret);
                }
            }
        };
        getContext().registerReceiver(ussdReceiver, new IntentFilter("com.ethiobalance.app.ACTION_USSD_RESPONSE"));
    }

    @PluginMethod
    public void startMonitoring(PluginCall call) {
        call.resolve();
    }

    @PluginMethod
    public void scanHistory(PluginCall call) {
        String senderId = call.getString("senderId");
        Integer days = call.getInt("days", 7);
        
        if (senderId == null) {
            call.reject("senderId is required");
            return;
        }

        long cutoff = System.currentTimeMillis() - (days * 24 * 60 * 60 * 1000L);
        
        Context context = getContext();
        android.database.Cursor cursor = context.getContentResolver().query(
                android.net.Uri.parse("content://sms/inbox"),
                new String[]{"_id", "address", "body", "date"},
                "address = ? AND date > ?",
                new String[]{senderId, String.valueOf(cutoff)},
                "date DESC"
        );

        if (cursor != null) {
            while (cursor.moveToNext()) {
                String address = cursor.getString(1);
                String body = cursor.getString(2);
                long date = cursor.getLong(3);
                
                JSObject ret = new JSObject();
                ret.put("sender", address);
                ret.put("body", body);
                ret.put("timestamp", date);
                
                notifyListeners("smsFound", ret);
            }
            cursor.close();
        }
        call.resolve();
    }

    @Override
    protected void handleOnDestroy() {
        super.handleOnDestroy();
        if (ussdReceiver != null) {
            getContext().unregisterReceiver(ussdReceiver);
        }
    }
}
