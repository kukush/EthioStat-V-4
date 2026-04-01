package com.ethiobalance.app.services

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.ethiobalance.app.AppConstants
import com.ethiobalance.app.data.AppDatabase
import com.ethiobalance.app.data.UssdEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

class UssdAccessibilityService : AccessibilityService() {

    private val executor = Executors.newSingleThreadExecutor()

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val packageName = event.packageName?.toString() ?: ""
            if (packageName.contains(AppConstants.PHONE_APP_PACKAGE)) {
                harvestUssdText(event.source)
            }
        }
    }

    private fun harvestUssdText(node: AccessibilityNodeInfo?) {
        if (node == null) return

        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) {
                if (child.text != null) {
                    val text = child.text.toString()
                    Log.d(TAG, "Harvested Text: $text")
                    
                    if (text.length > 5) {
                        saveUssdResponse(text)
                        attemptDismiss(node)
                    }
                }
                harvestUssdText(child)
                child.recycle()
            }
        }
    }

    private fun saveUssdResponse(response: String) {
        executor.execute {
            val db = AppDatabase.getDatabase(this)
            
            // Save to DB
            db.ussdDao().insert(UssdEntity(
                AppConstants.USSD_REQUEST_LABEL,
                response,
                System.currentTimeMillis(),
                AppConstants.DEFAULT_SIM_SLOT
            ))

            // Dual-Tracking: Let the engine parse the USSD string as if it's an SMS
            CoroutineScope(Dispatchers.IO).launch {
                ReconciliationEngine.processSms("USSD", response, System.currentTimeMillis(), db)
            }

            // Broadcast locally for UI string presentation if needed
            val intent = Intent(AppConstants.ACTION_USSD_RESPONSE)
            intent.putExtra("ussd_text", response)
            sendBroadcast(intent)
        }
    }

    private fun attemptDismiss(node: AccessibilityNodeInfo) {
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) {
                if ("android.widget.Button" == child.className) {
                    val btnText = child.text?.toString()?.lowercase() ?: ""
                    if (btnText.contains("ok") || btnText.contains("dismiss") || btnText.contains("cancel")) {
                        child.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    }
                }
                attemptDismiss(child)
                child.recycle()
            }
        }
    }

    override fun onInterrupt() {
        Log.e(TAG, "Service Interrupted")
    }

    companion object {
        private const val TAG = "UssdAccessibility"
    }
}
