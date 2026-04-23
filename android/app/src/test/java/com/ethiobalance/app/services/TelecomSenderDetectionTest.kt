package com.ethiobalance.app.services

import com.ethiobalance.app.AppConstants
import org.junit.Assert.*
import org.junit.Test

/**
 * Tests for telecom sender detection logic used in SmsForegroundService
 * to decide whether to send ACTION_TELECOM_SMS_ARRIVED broadcast and
 * show the heads-up notification after processing a 994 SMS.
 *
 * The logic: AppConstants.TELECOM_SENDERS.any { it.equals(sender, ignoreCase = true) }
 */
class TelecomSenderDetectionTest {

    private fun isTelecomSender(sender: String): Boolean =
        AppConstants.TELECOM_SENDERS.any { it.equals(sender, ignoreCase = true) }

    // ─── Positive cases: should trigger broadcast + notification ────────────

    @Test fun sender_994_isTelecom() =
        assertTrue(isTelecomSender("994"))

    @Test fun sender_251994_isTelecom() =
        assertTrue(isTelecomSender("251994"))

    @Test fun sender_plus251994_isTelecom() =
        assertTrue(isTelecomSender("+251994"))

    @Test fun sender_0994_isTelecom() =
        assertTrue(isTelecomSender("0994"))

    // ─── Negative cases: should NOT trigger ─────────────────────────────────

    @Test fun sender_CBE_isNotTelecom() =
        assertFalse(isTelecomSender("CBE"))

    @Test fun sender_Awash_isNotTelecom() =
        assertFalse(isTelecomSender("Awash"))

    @Test fun sender_127_isNotTelecom() =
        assertFalse(isTelecomSender("127"))

    @Test fun sender_847_isNotTelecom() =
        assertFalse(isTelecomSender("847"))

    @Test fun sender_empty_isNotTelecom() =
        assertFalse(isTelecomSender(""))

    @Test fun sender_randomSpam_isNotTelecom() =
        assertFalse(isTelecomSender("RandomSpam"))

    // ─── Sync notification channel ──────────────────────────────────────────

    @Test fun syncChannelId_isDefinedInService() {
        assertEquals("SyncNotificationChannel", SmsForegroundService.SYNC_CHANNEL_ID)
    }
}
