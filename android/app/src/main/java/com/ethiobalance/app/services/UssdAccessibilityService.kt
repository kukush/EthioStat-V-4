package com.ethiobalance.app.services

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.ethiobalance.app.AppConstants
import com.ethiobalance.app.MainActivity
import com.ethiobalance.app.data.AppDatabase
import com.ethiobalance.app.data.BalancePackageDao
import com.ethiobalance.app.data.BalancePackageEntity
import com.ethiobalance.app.data.UssdDao
import com.ethiobalance.app.data.UssdEntity
import com.ethiobalance.app.repository.SmsRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

@AndroidEntryPoint
class UssdAccessibilityService : AccessibilityService() {

    @Inject
    lateinit var reconciliationEngine: ReconciliationEngine
    
    @Inject
    lateinit var ussdDao: UssdDao

    @Inject
    lateinit var balancePackageDao: BalancePackageDao

    @Inject
    lateinit var smsRepository: SmsRepository

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Set to true after USSD text is captured; returnToApp() fires when popup is dismissed
    @Volatile private var pendingReturnToApp = false

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: ""
        Log.d(TAG, "Window state changed: pkg=$pkg pendingReturn=$pendingReturnToApp")

        // USSD popup is hosted by com.android.phone (or telephony variants) — NOT the dialer app
        val isPhonePopupPkg = pkg == "com.android.phone"
            || pkg == "com.samsung.telephony.phone"
            || pkg == "com.samsung.android.phone"
            || pkg == "com.huawei.phone"

        // Dialer app showing dialpad — signals popup was dismissed and user is back in dialer
        val isDialerPkg = pkg == "com.samsung.android.dialer"
            || pkg == "com.google.android.dialer"

        // If we are waiting for dismissal and dialer comes to foreground → popup was closed
        if (pendingReturnToApp && isDialerPkg) {
            pendingReturnToApp = false
            Log.d(TAG, "Popup dismissed (dialer resumed) — returning to app")
            returnToApp()
            return
        }

        // Only harvest USSD text from the phone popup package
        if (!isPhonePopupPkg) return

        // Try event.text first (fastest path)
        val eventTexts = (0 until event.text.size).mapNotNull { event.text.getOrNull(it)?.toString() }
        val eventText = eventTexts.filter { it.length > 5 }.joinToString(" ").trim()
        if (looksLikeUssdResponse(eventText)) {
            Log.d(TAG, "Captured USSD via event.text: $eventText")
            onUssdCaptured(eventText)
            autoDismissAndReturn(rootInActiveWindow ?: event.source)
            return
        }

        // Fallback: walk the accessibility node tree
        val root = rootInActiveWindow ?: event.source
        if (root != null) {
            val collected = mutableListOf<String>()
            harvestText(root, collected)
            val harvested = collected.filter { it.length > 5 }.joinToString(" ").trim()
            if (looksLikeUssdResponse(harvested)) {
                Log.d(TAG, "Captured USSD via node tree: $harvested")
                onUssdCaptured(harvested)
                autoDismissAndReturn(root)
                return
            }
        }
    }

    /**
     * After capturing USSD text: wait for user to dismiss popup, then return to app.
     * Does NOT auto-click OK/Cancel — user must manually dismiss the dialog.
     * Returns to app when dialer comes to foreground (popup dismissed) or after timeout.
     */
    private fun autoDismissAndReturn(root: AccessibilityNodeInfo?) {
        pendingReturnToApp = true
        // Note: We do NOT call attemptDismiss() here — user must manually click OK/Cancel
        Log.d(TAG, "Waiting for user to dismiss USSD popup...")

        // Fallback: return to app after 30s if user never dismisses (safety net)
        serviceScope.launch {
            delay(30_000)
            if (pendingReturnToApp) {
                pendingReturnToApp = false
                Log.d(TAG, "Fallback timeout (30s): returning to app")
                withContext(Dispatchers.Main) { returnToApp() }
            }
        }
    }

    /** Returns true if the text looks like a real USSD response (not a dialpad / progress screen). */
    private fun looksLikeUssdResponse(text: String): Boolean {
        if (text.isBlank()) return false
        val lower = text.lowercase()
        // Must contain balance/telecom keywords; reject pure "running..." progress screens
        return (lower.contains("birr") || lower.contains("balance") || lower.contains("dear")
            || lower.contains("customer") || lower.contains("ዋጋ") || lower.contains("ብር"))
            && !lower.contains("keypad") && !lower.contains("voicemail")
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

    /** Regex patterns to extract airtime ETB balance from USSD popup text.
     *  MUST match "Balance: X" format specifically — avoid promotional text like "win 20,000 Birr"
     */
    private val airtimeBalancePatterns = listOf(
        // Primary: "Balance: 123.45" or "Balance: 0" — must have Balance prefix
        Regex("""Balance[:\s]+(\d[\d,]*\.?\d*)""", RegexOption.IGNORE_CASE),
        // Secondary: "ቀሪ ሒሳብ: 123" (Amharic)
        Regex("""ቀሪ\s*ሒሳብ[:\s]+(\d[\d,]*\.?\d*)""", RegexOption.IGNORE_CASE),
        // Fallback: line starting with balance info (avoiding promo text)
        Regex("""^[\s]*(?:balance|ብር)[:\s]+(?:ETB\s*)?(\d[\d,]*\.?\d*)""", RegexOption.IGNORE_CASE)
    )

    private fun onUssdCaptured(response: String) {
        serviceScope.launch {
            try {
                val now = System.currentTimeMillis()

                // Persist raw USSD response
                ussdDao.insert(
                    UssdEntity(
                        request = AppConstants.USSD_REQUEST_LABEL,
                        response = response,
                        timestamp = now,
                        simSlot = AppConstants.DEFAULT_SIM_SLOT
                    )
                )

                // Extract AIRTIME balance only — skip full package parsing
                var airtimeBalance: Double? = null
                for (pattern in airtimeBalancePatterns) {
                    val match = pattern.find(response)
                    if (match != null) {
                        airtimeBalance = match.groupValues[1].replace(",", "").toDoubleOrNull()
                        if (airtimeBalance != null && airtimeBalance > 0) break
                    }
                }

                if (airtimeBalance != null && airtimeBalance > 0) {
                    balancePackageDao.insertOrUpdate(
                        BalancePackageEntity(
                            id = "airtime", simId = "", type = "airtime",
                            subType = "", totalAmount = airtimeBalance,
                            remainingAmount = airtimeBalance, unit = "ETB",
                            expiryDate = now + (30 * 24 * 60 * 60 * 1000L),
                            isActive = true, source = "USSD", lastUpdated = now
                        )
                    )
                    Log.d(TAG, "Saved airtime balance: $airtimeBalance ETB")
                } else {
                    Log.w(TAG, "Could not extract airtime balance from USSD response")
                }

                // Refresh telecom packages from the last 2 SMS from 994
                val refreshed = smsRepository.refreshTelecomFromLatestSms(limit = 2)
                Log.d(TAG, "Refreshed $refreshed telecom packages from latest SMS")

                // Broadcast to UI so TelecomViewModel knows sync is done
                val intent = Intent(AppConstants.ACTION_USSD_RESPONSE).apply {
                    putExtra("ussd_text", response)
                    setPackage(packageName)
                }
                sendBroadcast(intent)

                Log.d(TAG, "USSD response persisted and broadcast sent.")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to process USSD response: ${e.message}", e)
            }
        }
    }
    
    private fun returnToApp() {
        try {
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            startActivity(intent)
            Log.d(TAG, "Returned to MainActivity")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to return to app: ${e.message}", e)
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
