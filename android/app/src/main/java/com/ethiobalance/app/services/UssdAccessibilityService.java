package com.ethiobalance.app.services;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import com.ethiobalance.app.data.AppDatabase;
import com.ethiobalance.app.data.UssdEntity;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class UssdAccessibilityService extends AccessibilityService {
    private static final String TAG = "UssdAccessibility";
    private final Executor executor = Executors.newSingleThreadExecutor();

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            String packageName = (event.getPackageName() != null) ? event.getPackageName().toString() : "";
            if (packageName.contains("com.android.phone")) {
                harvestUssdText(event.getSource());
            }
        }
    }

    private void harvestUssdText(AccessibilityNodeInfo node) {
        if (node == null) return;

        // Traverse tree for text content
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null) {
                if (child.getText() != null) {
                    String text = child.getText().toString();
                    Log.d(TAG, "Harvested Text: " + text);
                    
                    // Basic validation for USSD content (e.g., contains 'Balance', 'ETB', '*', etc.)
                    if (text.length() > 5) {
                        saveUssdResponse(text);
                        // Auto-dismiss logic: perform click on 'OK' or 'Dismiss' button if found
                        attemptDismiss(node);
                    }
                }
                harvestUssdText(child);
                child.recycle();
            }
        }
    }

    private void saveUssdResponse(String response) {
        executor.execute(() -> {
            AppDatabase db = AppDatabase.getDatabase(this);
            db.ussdDao().insert(new UssdEntity("Last USSD Request", response, System.currentTimeMillis(), 0));
            // Broadcast locally for UI update or plugin listener
            Intent intent = new Intent("com.ethiobalance.app.ACTION_USSD_RESPONSE");
            intent.putExtra("ussd_text", response);
            sendBroadcast(intent);
        });
    }

    private void attemptDismiss(AccessibilityNodeInfo node) {
        // Look for buttons that might dismiss the dialog
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null) {
                if ("android.widget.Button".equals(child.getClassName())) {
                    String btnText = (child.getText() != null) ? child.getText().toString().toLowerCase() : "";
                    if (btnText.contains("ok") || btnText.contains("dismiss") || btnText.contains("cancel")) {
                        child.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                    }
                }
                attemptDismiss(child);
                child.recycle();
            }
        }
    }

    @Override
    public void onInterrupt() {
        Log.e(TAG, "Service Interrupted");
    }
}
