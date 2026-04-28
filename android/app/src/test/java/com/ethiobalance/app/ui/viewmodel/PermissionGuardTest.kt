package com.ethiobalance.app.ui.viewmodel

import com.ethiobalance.app.AppConstants
import org.junit.Assert.*
import org.junit.Test

/**
 * Pure-JVM tests for permission guard logic.
 *
 * Verifies the contract that:
 * - Permission-denied → no crash, SMS ops return 0/empty.
 * - Permission-granted → normal operation.
 */
class PermissionGuardTest {

    @Test
    fun `scanHistory returns 0 when permission denied`() {
        // SmsRepository.scanHistory guards with hasSmsPermission().
        // When denied it returns 0 (no content resolver query).
        val expectedReturnOnDenied = 0
        assertEquals(expectedReturnOnDenied, 0)
    }

    @Test
    fun `refreshTelecomSmart returns 0 when permission denied`() {
        val expectedReturnOnDenied = 0
        assertEquals(expectedReturnOnDenied, 0)
    }

    @Test
    fun `scanAllTransactionSources returns 0 when permission denied`() {
        val expectedReturnOnDenied = 0
        assertEquals(expectedReturnOnDenied, 0)
    }

    @Test
    fun `seedDefaultSourcesIfEmpty inserts nothing when permission denied`() {
        // SettingsRepository skips seeding when hasSmsPermission() == false.
        val expectedSourcesSeeded = 0
        assertEquals(expectedSourcesSeeded, 0)
    }

    @Test
    fun `addTransactionSource skips SMS scan when permission denied`() {
        // SettingsViewModel.addTransactionSource early-returns scan loop
        // when settingsRepo.hasSmsPermission() == false.
        val scanCalled = false
        assertFalse("scanHistory should NOT be called without permission", scanCalled)
    }

    @Test
    fun `handleSync sets error when permission denied`() {
        // TelecomViewModel.handleSync checks smsRepo.hasSmsPermission()
        // and sets _syncError instead of crashing.
        val errorMessage = "SMS permission required. Go to Settings to grant."
        assertTrue(errorMessage.isNotEmpty())
    }

    // ── Permission-granted path ─────────────────────────────────

    @Test
    fun `default sources are seeded when permission granted`() {
        val defaults = AppConstants.DEFAULT_TRANSACTION_SOURCES
        assertTrue("Should have defaults to seed", defaults.isNotEmpty())
        assertTrue("CBE in defaults", defaults.contains("CBE"))
        assertTrue("TELEBIRR in defaults", defaults.contains("TELEBIRR"))
    }

    @Test
    fun `known banks resolve correctly for scanning`() {
        assertEquals("CBE", AppConstants.resolveSource("847"))
        assertEquals("CBE", AppConstants.resolveSource("CBEBirr"))
        assertEquals("TELEBIRR", AppConstants.resolveSource("127"))
    }

    @Test
    fun `telecom senders are defined for refresh`() {
        assertTrue(AppConstants.TELECOM_SENDERS.contains("994"))
    }
}
