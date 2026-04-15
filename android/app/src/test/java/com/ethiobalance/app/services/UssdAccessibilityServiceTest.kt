package com.ethiobalance.app.services

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [UssdAccessibilityService] logic that can be exercised without
 * a real Android device or Accessibility framework.
 *
 * Covers:
 *  - looksLikeUssdResponse(): accepts real balance responses, rejects dialpad/progress junk
 *  - Package classification: isPhonePopupPkg / isDialerPkg helpers used in onAccessibilityEvent
 *  - pendingReturnToApp state machine (simulated via the same branching logic)
 *
 * NOTE: CALL_PHONE permission is NOT used anywhere in the service. Dialing is done
 * via ACTION_DIAL — the user manually presses Call.
 */
class UssdAccessibilityServiceTest {

    // ─────────────────────────────────────────────────────────────────────────
    // Replicate private helpers as package-private for testing
    // (mirrors exact logic from UssdAccessibilityService)
    // ─────────────────────────────────────────────────────────────────────────

    private fun looksLikeUssdResponse(text: String): Boolean {
        if (text.isBlank()) return false
        val lower = text.lowercase()
        return (lower.contains("birr") || lower.contains("balance") || lower.contains("dear")
            || lower.contains("customer") || lower.contains("ዋጋ") || lower.contains("ብር"))
            && !lower.contains("keypad") && !lower.contains("voicemail")
    }

    private fun isPhonePopupPkg(pkg: String) =
        pkg == "com.android.phone"
            || pkg == "com.samsung.telephony.phone"
            || pkg == "com.samsung.android.phone"
            || pkg == "com.huawei.phone"

    private fun isDialerPkg(pkg: String) =
        pkg == "com.samsung.android.dialer"
            || pkg == "com.google.android.dialer"

    // ─────────────────────────────────────────────────────────────────────────
    // looksLikeUssdResponse — ACCEPT cases
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `real balance response with Birr is accepted`() {
        val text = "Dear customer, Your account balance is 1.75 Birr . " +
            "With this balance your account will expire on 15/04/2027. ethio telecom"
        assertTrue(looksLikeUssdResponse(text))
    }

    @Test
    fun `response containing only balance keyword is accepted`() {
        assertTrue(looksLikeUssdResponse("Your balance is 100 Birr"))
    }

    @Test
    fun `response with Amharic Birr symbol is accepted`() {
        assertTrue(looksLikeUssdResponse("ቀሪ ሒሳብ 50 ብር"))
    }

    @Test
    fun `response with Amharic price keyword is accepted`() {
        assertTrue(looksLikeUssdResponse("ዋጋ 200 ብር"))
    }

    @Test
    fun `response with Dear Customer keywords is accepted`() {
        assertTrue(looksLikeUssdResponse("Dear Customer, your package expires tomorrow."))
    }

    @Test
    fun `mixed case balance keyword is accepted`() {
        assertTrue(looksLikeUssdResponse("Your BALANCE is 55.00 Birr"))
    }

    // ─────────────────────────────────────────────────────────────────────────
    // looksLikeUssdResponse — REJECT cases
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `blank string is rejected`() {
        assertFalse(looksLikeUssdResponse(""))
    }

    @Test
    fun `whitespace-only string is rejected`() {
        assertFalse(looksLikeUssdResponse("   "))
    }

    @Test
    fun `dialer keypad text is rejected`() {
        val dialpadText = "Search More options 1,Voicemail 2 ,A,B,C 3 ,D,E,F 4 " +
            "Delete last digit Meet video call Keypad"
        assertFalse(looksLikeUssdResponse(dialpadText))
    }

    @Test
    fun `USSD running progress screen is rejected`() {
        assertFalse(looksLikeUssdResponse("USSD code running…"))
    }

    @Test
    fun `generic string without telecom keywords is rejected`() {
        assertFalse(looksLikeUssdResponse("Hello world"))
    }

    @Test
    fun `voicemail label is rejected even if it contains other keywords`() {
        // Pathological: "balance" present but "voicemail" also present → rejected
        assertFalse(looksLikeUssdResponse("Voicemail balance 10 Birr keypad"))
    }

    @Test
    fun `keypad label is rejected even if balance keyword present`() {
        assertFalse(looksLikeUssdResponse("balance keypad Birr"))
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Package classification
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `com_android_phone is phone popup package`() {
        assertTrue(isPhonePopupPkg("com.android.phone"))
    }

    @Test
    fun `com_samsung_telephony_phone is phone popup package`() {
        assertTrue(isPhonePopupPkg("com.samsung.telephony.phone"))
    }

    @Test
    fun `com_samsung_android_phone is phone popup package`() {
        assertTrue(isPhonePopupPkg("com.samsung.android.phone"))
    }

    @Test
    fun `com_huawei_phone is phone popup package`() {
        assertTrue(isPhonePopupPkg("com.huawei.phone"))
    }

    @Test
    fun `dialer is NOT a phone popup package`() {
        assertFalse(isPhonePopupPkg("com.samsung.android.dialer"))
        assertFalse(isPhonePopupPkg("com.google.android.dialer"))
    }

    @Test
    fun `com_samsung_android_dialer is dialer package`() {
        assertTrue(isDialerPkg("com.samsung.android.dialer"))
    }

    @Test
    fun `com_google_android_dialer is dialer package`() {
        assertTrue(isDialerPkg("com.google.android.dialer"))
    }

    @Test
    fun `phone popup packages are NOT dialer packages`() {
        assertFalse(isDialerPkg("com.android.phone"))
        assertFalse(isDialerPkg("com.samsung.telephony.phone"))
    }

    @Test
    fun `ethiobalance app package is neither phone nor dialer`() {
        assertFalse(isPhonePopupPkg("com.ethiobalance.app"))
        assertFalse(isDialerPkg("com.ethiobalance.app"))
    }

    // ─────────────────────────────────────────────────────────────────────────
    // pendingReturnToApp state machine
    // Simulates the onAccessibilityEvent branching without a real service instance
    // ─────────────────────────────────────────────────────────────────────────

    private data class EventResult(val returnToAppCalled: Boolean, val pendingAfter: Boolean)

    private fun simulateEvent(pkg: String, text: String, pendingBefore: Boolean): EventResult {
        var pending = pendingBefore
        var returnCalled = false

        val isPhone = isPhonePopupPkg(pkg)
        val isDialer = isDialerPkg(pkg)

        if (pending && isDialer) {
            pending = false
            returnCalled = true
            return EventResult(returnCalled, pending)
        }
        if (!isPhone) return EventResult(false, pending)

        if (looksLikeUssdResponse(text)) {
            pending = true
        }
        return EventResult(returnCalled, pending)
    }

    @Test
    fun `USSD text from phone sets pendingReturnToApp true`() {
        val result = simulateEvent(
            pkg = "com.android.phone",
            text = "Dear customer, Your account balance is 1.75 Birr",
            pendingBefore = false
        )
        assertFalse(result.returnToAppCalled)
        assertTrue(result.pendingAfter)
    }

    @Test
    fun `dialer event after capture triggers returnToApp and clears flag`() {
        val result = simulateEvent(
            pkg = "com.samsung.android.dialer",
            text = "",
            pendingBefore = true
        )
        assertTrue(result.returnToAppCalled)
        assertFalse(result.pendingAfter)
    }

    @Test
    fun `dialer event without prior capture does NOT trigger returnToApp`() {
        val result = simulateEvent(
            pkg = "com.samsung.android.dialer",
            text = "",
            pendingBefore = false
        )
        assertFalse(result.returnToAppCalled)
        assertFalse(result.pendingAfter)
    }

    @Test
    fun `USSD running progress from phone does NOT set pendingReturnToApp`() {
        val result = simulateEvent(
            pkg = "com.android.phone",
            text = "USSD code running…",
            pendingBefore = false
        )
        assertFalse(result.returnToAppCalled)
        assertFalse(result.pendingAfter)
    }

    @Test
    fun `dialer keypad event from dialer pkg does NOT set pendingReturnToApp`() {
        val result = simulateEvent(
            pkg = "com.samsung.android.dialer",
            text = "Search More options 1,Voicemail 2 Keypad",
            pendingBefore = false
        )
        assertFalse(result.returnToAppCalled)
        assertFalse(result.pendingAfter)
    }

    @Test
    fun `unrelated app events are ignored entirely`() {
        val result = simulateEvent(
            pkg = "com.whatsapp",
            text = "Dear customer balance 50 Birr",
            pendingBefore = false
        )
        assertFalse(result.returnToAppCalled)
        assertFalse(result.pendingAfter)
    }

    @Test
    fun `full happy path - phone popup then dialer dismissal`() {
        // Step 1: dialer opens (keypad shown) — nothing happens
        var state = simulateEvent("com.samsung.android.dialer", "", false)
        assertFalse(state.returnToAppCalled)
        assertFalse(state.pendingAfter)

        // Step 2: USSD running progress — nothing captured
        state = simulateEvent("com.android.phone", "USSD code running…", state.pendingAfter)
        assertFalse(state.returnToAppCalled)
        assertFalse(state.pendingAfter)

        // Step 3: USSD balance response appears — captured, pending set
        state = simulateEvent(
            "com.android.phone",
            "Dear customer, Your account balance is 1.75 Birr . ethio telecom",
            state.pendingAfter
        )
        assertFalse(state.returnToAppCalled)
        assertTrue(state.pendingAfter)

        // Step 4: User clicks OK → dialer comes back → returnToApp fires
        state = simulateEvent("com.samsung.android.dialer", "", state.pendingAfter)
        assertTrue(state.returnToAppCalled)
        assertFalse(state.pendingAfter)
    }
}
