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

/**
 * Reads USSD popup dialog text after the user triggers *804#.
 *
 * Setup required for user:
 *   Settings → Accessibility → EthioStat → Enable.
 *
 * Scope: limited to "com.android.phone" package (declared in ussd_service_config.xml)
 * so we never read popups from other apps.
 *
 * Compatible with Android 5.0+ (API 21+).
 * The deprecated `recycle()` call is intentionally removed — the framework
 * automatically recycles nodes obtained via `AccessibilityEvent.source` and
 * `AccessibilityNodeInfo.getChild()` on Android 9+ without explicit recycling.
 */
class UssdAccessibilityService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        // Only act on the phone dialer package (scoped in XML config too, belt-and-suspenders)
        if (!pkg.contains(AppConstants.PHONE_APP_PACKAGE, ignoreCase = true)) return

        // Try harvesting from event text first (fastest path, works on most devices)
        val eventTexts = (0 until event.text.size).mapNotNull { event.text.getOrNull(it)?.toString() }
        val eventText = eventTexts.filter { it.length > 5 }.joinToString(" ").trim()
        if (eventText.isNotEmpty()) {
            Log.d(TAG, "Captured USSD via event.text: $eventText")
            onUssdCaptured(eventText)
            return
        }

        // Fallback: walk the accessibility node tree
        val root = rootInActiveWindow ?: event.source
        if (root != null) {
            val collected = mutableListOf<String>()
            harvestText(root, collected)
            val harvested = collected.filter { it.length > 5 }.joinToString(" ").trim()
            if (harvested.isNotEmpty()) {
                Log.d(TAG, "Captured USSD via node tree: $harvested")
                onUssdCaptured(harvested)
                attemptDismiss(root)
            }
        }
    }

    /**
     * Recursively walk the view tree collecting any non-trivial text.
     * Does NOT call recycle() — safe on all modern Android versions.
     */
    private fun harvestText(node: AccessibilityNodeInfo, out: MutableList<String>) {
        val text = node.text?.toString()
        if (!text.isNullOrBlank() && text.length > 3) {
            out.add(text)
        }
        val desc = node.contentDescription?.toString()
        if (!desc.isNullOrBlank() && desc.length > 3 && desc != text) {
            out.add(desc)
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            harvestText(child, out)
        }
    }

    /**
     * Auto-dismiss the USSD dialog by clicking the OK / Cancel button.
     */
    private fun attemptDismiss(node: AccessibilityNodeInfo) {
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val btnText = child.text?.toString()?.lowercase() ?: ""
            if (child.className?.contains("Button") == true &&
                (btnText.contains("ok") || btnText.contains("cancel") || btnText.contains("dismiss"))) {
                child.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            }
            attemptDismiss(child)
        }
    }

    private fun onUssdCaptured(response: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getDatabase(this@UssdAccessibilityService)

                // Persist raw USSD response
                db.ussdDao().insert(
                    UssdEntity(
                        AppConstants.USSD_REQUEST_LABEL,
                        response,
                        System.currentTimeMillis(),
                        AppConstants.DEFAULT_SIM_SLOT
                    )
                )

                // Run through the dual-tracking reconciliation pipeline
                ReconciliationEngine.processSms("804", response, System.currentTimeMillis(), db)

                // Broadcast to UI for real-time display
                val intent = Intent(AppConstants.ACTION_USSD_RESPONSE).apply {
                    putExtra("ussd_text", response)
                    setPackage(packageName) // explicit package to avoid implicit broadcast warning
                }
                sendBroadcast(intent)

                Log.d(TAG, "USSD response persisted and broadcast sent.")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to process USSD response: ${e.message}", e)
            }
        }
    }

    override fun onInterrupt() {
        Log.w(TAG, "AccessibilityService interrupted")
    }

    companion object {
        private const val TAG = "UssdAccessibility"
        
        // Instance reference for external calls
        @Volatile
        private var instance: UssdAccessibilityService? = null
        
        fun getInstance(): UssdAccessibilityService? = instance

        /** Call this from SettingsScreen to open the system accessibility settings page. */
        fun buildSettingsIntent(): Intent =
            Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
    }
    
    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.d(TAG, "AccessibilityService connected")
    }
    
    override fun onDestroy() {
        super.onDestroy()
        instance = null
        Log.d(TAG, "AccessibilityService destroyed")
    }
    
    /**
     * Close the current dialog/dialer by performing a back gesture
     */
    fun closeDialer() {
        try {
            // Perform back action
            performGlobalAction(GLOBAL_ACTION_BACK)
            Log.d(TAG, "Performed global back action to close dialer")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to close dialer: ${e.message}", e)
        }
    }
}
