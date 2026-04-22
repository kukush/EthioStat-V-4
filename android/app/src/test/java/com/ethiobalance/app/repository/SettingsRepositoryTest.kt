package com.ethiobalance.app.repository

import com.ethiobalance.app.AppConstants
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for SettingsRepository-related constants and helper logic.
 *
 * Tests DEFAULT_TRANSACTION_SOURCES content and sender variant resolution
 * without requiring Android mocks (pure JVM tests).
 */
class SettingsRepositoryTest {

    // ─── DEFAULT_TRANSACTION_SOURCES tests ──────────────────────────────────

    @Test
    fun defaultTransactionSources_containsExactlyCbeAndTelebirr() {
        val defaults = AppConstants.DEFAULT_TRANSACTION_SOURCES

        assertEquals("Should have exactly 2 defaults", 2, defaults.size)
        assertTrue("Should contain CBE", defaults.contains("CBE"))
        assertTrue("Should contain TELEBIRR", defaults.contains("TELEBIRR"))
    }

    @Test
    fun defaultTransactionSources_doesNotContainOtherBanks() {
        val defaults = AppConstants.DEFAULT_TRANSACTION_SOURCES

        assertFalse("Should not contain AWASH", defaults.contains("AWASH"))
        assertFalse("Should not contain DASHEN", defaults.contains("DASHEN"))
        assertFalse("Should not contain BOA", defaults.contains("BOA"))
        assertFalse("Should not contain AIB", defaults.contains("AIB"))
    }

    @Test
    fun defaultTransactionSources_isUppercase() {
        val defaults = AppConstants.DEFAULT_TRANSACTION_SOURCES

        defaults.forEach { source ->
            assertEquals("Source should be uppercase: $source",
                source, source.uppercase())
        }
    }

    // ─── Sender variant resolution tests (via AppConstants.resolveSource) ─────

    @Test
    fun resolveSource_cbeNumericVariants() {
        // CBE has multiple numeric sender IDs
        assertEquals("CBE", AppConstants.resolveSource("847"))
        assertEquals("CBE", AppConstants.resolveSource("889"))
    }

    @Test
    fun resolveSource_cbeAlphaVariants() {
        assertEquals("CBE", AppConstants.resolveSource("CBE"))
        assertEquals("CBE", AppConstants.resolveSource("CBEBirr"))
        assertEquals("CBE", AppConstants.resolveSource("CBEBIRR"))
    }

    @Test
    fun resolveSource_telebirrVariants() {
        assertEquals("TELEBIRR", AppConstants.resolveSource("127"))
        assertEquals("TELEBIRR", AppConstants.resolveSource("TELEBIRR"))
        assertEquals("TELEBIRR", AppConstants.resolveSource("Telebirr"))
    }

    @Test
    fun resolveSource_awashVariants() {
        assertEquals("AWASH", AppConstants.resolveSource("901"))
        assertEquals("AWASH", AppConstants.resolveSource("Awash"))
        assertEquals("AWASH", AppConstants.resolveSource("AWASH"))
        assertEquals("AWASH", AppConstants.resolveSource("AwashBank"))
    }

    // ─── SMS_SENDER_WHITELIST contains all expected senders ───────────────────

    @Test
    fun smsSenderWhitelist_containsCbeSenders() {
        assertTrue("Should contain 847",
            AppConstants.SMS_SENDER_WHITELIST.contains("847"))
        assertTrue("Should contain 889",
            AppConstants.SMS_SENDER_WHITELIST.contains("889"))
        assertTrue("Should contain CBE",
            AppConstants.SMS_SENDER_WHITELIST.contains("CBE"))
    }

    @Test
    fun smsSenderWhitelist_containsTelebirrSenders() {
        assertTrue("Should contain 127",
            AppConstants.SMS_SENDER_WHITELIST.contains("127"))
        assertTrue("Should contain TELEBIRR",
            AppConstants.SMS_SENDER_WHITELIST.contains("TELEBIRR"))
    }

    @Test
    fun smsSenderWhitelist_containsTelecomSenders() {
        // Telecom senders for airtime/balance notifications
        assertTrue("Should contain 994",
            AppConstants.SMS_SENDER_WHITELIST.contains("994"))
        assertTrue("Should contain 804",
            AppConstants.SMS_SENDER_WHITELIST.contains("804"))
    }

    // ─── KNOWN_BANKS lookup tests ────────────────────────────────────────────

    @Test
    fun knownBanks_lookupByAbbreviation() {
        val cbe = AppConstants.KNOWN_BANKS.find { it.abbreviation == "CBE" }
        assertNotNull("CBE should be in KNOWN_BANKS", cbe)
        assertEquals("Commercial Bank of Ethiopia", cbe?.fullName)

        val telebirr = AppConstants.KNOWN_BANKS.find { it.abbreviation == "TELEBIRR" }
        assertNotNull("TELEBIRR should be in KNOWN_BANKS", telebirr)
        assertEquals("Telebirr", telebirr?.fullName)
    }

    @Test
    fun knownBanks_senderIdVaries() {
        val cbe = AppConstants.KNOWN_BANKS.find { it.abbreviation == "CBE" }
        assertNotNull(cbe)
        // CBE has at least one sender ID variant
        assertTrue("CBE should have senderId variants",
            cbe?.senderId?.isNotEmpty() == true)
    }
}
