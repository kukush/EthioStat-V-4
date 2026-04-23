package com.ethiobalance.app

import org.junit.Assert.*
import org.junit.Test

/**
 * Tests verifying the sync-related changes to AppConstants:
 * - Removed constants: ACTION_USSD_RESPONSE, PHONE_APP_PACKAGE, USSD_REQUEST_LABEL
 * - Added constant: ACTION_TELECOM_SMS_ARRIVED
 * - TELECOM_SENDERS coverage for 994 variants
 */
class SyncConstantsTest {

    // ─── ACTION_TELECOM_SMS_ARRIVED exists ─────────────────────────────────

    @Test
    fun action_telecomSmsArrived_exists() {
        assertEquals(
            "com.ethiobalance.app.ACTION_TELECOM_SMS_ARRIVED",
            AppConstants.ACTION_TELECOM_SMS_ARRIVED
        )
    }

    @Test
    fun action_triggerRefresh_stillExists() {
        assertEquals(
            "com.ethiobalance.app.ACTION_TRIGGER_REFRESH",
            AppConstants.ACTION_TRIGGER_REFRESH
        )
    }

    // ─── Removed constants no longer exist (compile-time check) ────────────
    // These are verified by the fact that this file compiles without referencing:
    //   AppConstants.ACTION_USSD_RESPONSE
    //   AppConstants.PHONE_APP_PACKAGE
    //   AppConstants.USSD_REQUEST_LABEL
    // If any were re-introduced, the tests below would need updating.

    @Test
    fun removedConstants_notInClassFields() {
        val fields = AppConstants::class.java.declaredFields.map { it.name }
        assertFalse(
            "ACTION_USSD_RESPONSE should be removed",
            fields.contains("ACTION_USSD_RESPONSE")
        )
        assertFalse(
            "PHONE_APP_PACKAGE should be removed",
            fields.contains("PHONE_APP_PACKAGE")
        )
        assertFalse(
            "USSD_REQUEST_LABEL should be removed",
            fields.contains("USSD_REQUEST_LABEL")
        )
    }

    // ─── TELECOM_SENDERS ───────────────────────────────────────────────────

    @Test
    fun telecomSenders_contains994() {
        assertTrue(AppConstants.TELECOM_SENDERS.contains("994"))
    }

    @Test
    fun telecomSenders_contains251994() {
        assertTrue(AppConstants.TELECOM_SENDERS.contains("251994"))
    }

    @Test
    fun telecomSenders_containsPlus251994() {
        assertTrue(AppConstants.TELECOM_SENDERS.contains("+251994"))
    }

    @Test
    fun telecomSenders_contains0994() {
        assertTrue(AppConstants.TELECOM_SENDERS.contains("0994"))
    }

    @Test
    fun telecomSenders_matchesCaseInsensitive() {
        // Verify the pattern used in SmsForegroundService for sender matching
        val sender = "251994"
        val isTelecom = AppConstants.TELECOM_SENDERS.any {
            it.equals(sender, ignoreCase = true)
        }
        assertTrue("251994 should match as telecom sender", isTelecom)
    }

    @Test
    fun telecomSenders_unknownSenderDoesNotMatch() {
        val sender = "CBE"
        val isTelecom = AppConstants.TELECOM_SENDERS.any {
            it.equals(sender, ignoreCase = true)
        }
        assertFalse("CBE should NOT match as telecom sender", isTelecom)
    }
}
