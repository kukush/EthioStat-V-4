package com.ethiobalance.app

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for AppConstants — resolveSource(), displaySource(), and SMS_SENDER_WHITELIST.
 *
 * Real sender addresses pulled from device via:
 *   adb shell content query --uri content://sms/inbox --projection "address"
 *
 * Verified senders on device: "Awash", "CBE", "DashenBank", "127", "251994", "DB SuperApp"
 */
class AppConstantsTest {

    // ─── resolveSource: canonical UPPERCASE output ───────────────────────────

    @Test fun resolveSource_numericAwash() =
        assertEquals("AWASH", AppConstants.resolveSource("901"))

    @Test fun resolveSource_alphaAwash_exactInbox() =
        assertEquals("AWASH", AppConstants.resolveSource("Awash"))

    @Test fun resolveSource_alphaAwash_mixed() =
        assertEquals("AWASH", AppConstants.resolveSource("AWASH"))

    @Test fun resolveSource_alphaAwash_lower() =
        assertEquals("AWASH", AppConstants.resolveSource("awash"))

    @Test fun resolveSource_alphaAwashBank() =
        assertEquals("AWASH", AppConstants.resolveSource("AwashBank"))

    @Test fun resolveSource_alphaAwashBankSpaced() =
        assertEquals("AWASH", AppConstants.resolveSource("Awash Bank"))

    @Test fun resolveSource_numericCBE() =
        assertEquals("CBE", AppConstants.resolveSource("847"))

    @Test fun resolveSource_alphaCBE() =
        assertEquals("CBE", AppConstants.resolveSource("CBE"))

    @Test fun resolveSource_alphaCBEBirr() =
        assertEquals("CBE", AppConstants.resolveSource("CBEBirr"))

    @Test fun resolveSource_numericDashen() =
        assertEquals("DASHEN", AppConstants.resolveSource("996"))

    @Test fun resolveSource_alphaDashenBank() =
        assertEquals("DASHEN", AppConstants.resolveSource("DashenBank"))

    @Test fun resolveSource_numericTelebirr() =
        assertEquals("TELEBIRR", AppConstants.resolveSource("127"))

    @Test fun resolveSource_alphaTelebirr() =
        assertEquals("TELEBIRR", AppConstants.resolveSource("Telebirr"))

    @Test fun resolveSource_alphaTelebirrUpper() =
        assertEquals("TELEBIRR", AppConstants.resolveSource("TELEBIRR"))

    @Test fun resolveSource_numericBOA() =
        assertEquals("BOA", AppConstants.resolveSource("815"))

    @Test fun resolveSource_alphaBOA() =
        assertEquals("BOA", AppConstants.resolveSource("BOA"))

    @Test fun resolveSource_numericHibret() =
        assertEquals("HIBRET", AppConstants.resolveSource("811"))

    @Test fun resolveSource_alphaHibret() =
        assertEquals("HIBRET", AppConstants.resolveSource("Hibret"))

    @Test fun resolveSource_numericBerhan() =
        assertEquals("BERHAN", AppConstants.resolveSource("881"))

    @Test fun resolveSource_alphaBerhan() =
        assertEquals("BERHAN", AppConstants.resolveSource("Berhan"))

    @Test fun resolveSource_numericZemen() =
        assertEquals("ZEMEN", AppConstants.resolveSource("844"))

    @Test fun resolveSource_alphaZemen() =
        assertEquals("ZEMEN", AppConstants.resolveSource("Zemen"))

    @Test fun resolveSource_numericCoopbank() =
        assertEquals("COOPBANK", AppConstants.resolveSource("841"))

    @Test fun resolveSource_alphaCoopbank() =
        assertEquals("COOPBANK", AppConstants.resolveSource("Coopbank"))

    @Test fun resolveSource_airtime994() =
        assertEquals("AIRTIME", AppConstants.resolveSource("994"))

    @Test fun resolveSource_airtime251994() =
        assertEquals("AIRTIME", AppConstants.resolveSource("251994"))

    @Test fun resolveSource_fallback_uppercases() {
        val result = AppConstants.resolveSource("UnknownSender")
        assertEquals("UNKNOWNSENDER", result)
    }

    @Test fun resolveSource_trims_whitespace() =
        assertEquals("AWASH", AppConstants.resolveSource("  Awash  "))

    // ─── resolveSource: always returns UPPERCASE ─────────────────────────────

    @Test fun resolveSource_neverReturnsMixedCase() {
        val senders = listOf("Awash", "awash", "AWASH", "901", "CBE", "DashenBank",
            "Coopbank", "Hibret", "Berhan", "Zemen", "BOA", "127", "994")
        senders.forEach { sender ->
            val result = AppConstants.resolveSource(sender)
            assertEquals(
                "resolveSource(\"$sender\") should be uppercase but got \"$result\"",
                result.uppercase(), result
            )
        }
    }

    // ─── displaySource: human-readable title-case ────────────────────────────

    @Test fun displaySource_awash() =
        assertEquals("Awash", AppConstants.displaySource("AWASH"))

    @Test fun displaySource_awash_fromLower() =
        assertEquals("Awash", AppConstants.displaySource("awash"))

    @Test fun displaySource_cbe_staysUppercase() =
        assertEquals("CBE", AppConstants.displaySource("CBE"))

    @Test fun displaySource_boa_staysUppercase() =
        assertEquals("BOA", AppConstants.displaySource("BOA"))

    @Test fun displaySource_nib_staysUppercase() =
        assertEquals("NIB", AppConstants.displaySource("NIB"))

    @Test fun displaySource_telebirr() =
        assertEquals("Telebirr", AppConstants.displaySource("TELEBIRR"))

    @Test fun displaySource_dashen() =
        assertEquals("Dashen", AppConstants.displaySource("DASHEN"))

    @Test fun displaySource_coopbank() =
        assertEquals("Coopbank", AppConstants.displaySource("COOPBANK"))

    @Test fun displaySource_hibret() =
        assertEquals("Hibret", AppConstants.displaySource("HIBRET"))

    @Test fun displaySource_berhan() =
        assertEquals("Berhan", AppConstants.displaySource("BERHAN"))

    @Test fun displaySource_zemen() =
        assertEquals("Zemen", AppConstants.displaySource("ZEMEN"))

    @Test fun displaySource_zamzam() =
        assertEquals("ZamZam", AppConstants.displaySource("ZAMZAM"))

    @Test fun displaySource_unknown_titleCase() =
        assertEquals("Newbank", AppConstants.displaySource("NEWBANK"))

    // ─── SMS_SENDER_WHITELIST: contains real device sender addresses ──────────

    @Test fun whitelist_containsAwash_shortName() =
        assertTrue("\"Awash\" must be in whitelist",
            AppConstants.SMS_SENDER_WHITELIST.any { it.equals("Awash", ignoreCase = true) })

    @Test fun whitelist_containsCBE() =
        assertTrue(AppConstants.SMS_SENDER_WHITELIST.contains("CBE"))

    @Test fun whitelist_containsDashenBank() =
        assertTrue(AppConstants.SMS_SENDER_WHITELIST.any { it.equals("DashenBank", ignoreCase = true) })

    @Test fun whitelist_containsTelebirr_shortName() =
        assertTrue(AppConstants.SMS_SENDER_WHITELIST.any { it.equals("Telebirr", ignoreCase = true) })

    @Test fun whitelist_containsCoopbank_shortName() =
        assertTrue(AppConstants.SMS_SENDER_WHITELIST.any { it.equals("Coopbank", ignoreCase = true) })

    @Test fun whitelist_containsHibret_shortName() =
        assertTrue(AppConstants.SMS_SENDER_WHITELIST.any { it.equals("Hibret", ignoreCase = true) })

    @Test fun whitelist_containsBerhan_shortName() =
        assertTrue(AppConstants.SMS_SENDER_WHITELIST.any { it.equals("Berhan", ignoreCase = true) })

    @Test fun whitelist_containsZemen_shortName() =
        assertTrue(AppConstants.SMS_SENDER_WHITELIST.any { it.equals("Zemen", ignoreCase = true) })

    @Test fun whitelist_containsNumericSenders() {
        listOf("847", "901", "996", "815", "811", "881", "844", "127", "994").forEach { n ->
            assertTrue("numeric sender \"$n\" must be in whitelist",
                AppConstants.SMS_SENDER_WHITELIST.contains(n))
        }
    }

    // ─── SmsReceiver whitelist logic: sender resolves to known canonical key ──
    //
    // SmsReceiver accepts a sender if resolveSource(sender) is in SOURCE_DISPLAY_NAMES.
    // This mirrors the SmsReceiver logic: any sender whose resolved key is a configured
    // bank/telecom is whitelisted.

    private val knownKeys: Set<String> by lazy {
        AppConstants.KNOWN_BANKS.map { it.abbreviation }.toSet() +
        setOf(AppConstants.SOURCE_TELEBIRR, AppConstants.SOURCE_AIRTIME)
    }

    private fun isKnownSender(sender: String): Boolean =
        AppConstants.resolveSource(sender) in knownKeys

    @Test fun smsReceiverLogic_awashIsKnown() =
        assertTrue("\"Awash\" sender should be detected as a known source", isKnownSender("Awash"))

    @Test fun smsReceiverLogic_awashBankIsKnown() =
        assertTrue("\"Awash Bank\" sender should be detected as a known source", isKnownSender("Awash Bank"))

    @Test fun smsReceiverLogic_dashenBankIsKnown() =
        assertTrue("\"DashenBank\" sender should be detected as a known source", isKnownSender("DashenBank"))

    @Test fun smsReceiverLogic_cbeIsKnown() =
        assertTrue("\"CBE\" sender should be detected as a known source", isKnownSender("CBE"))

    @Test fun smsReceiverLogic_127IsKnown() =
        assertTrue("\"127\" sender should be detected as a known source", isKnownSender("127"))

    @Test fun smsReceiverLogic_unknownSenderIsNotKnown() =
        assertFalse("\"RandomSpam\" sender should NOT be detected as known", isKnownSender("RandomSpam"))

    // ─── KNOWN_BANKS consistency: abbreviation matches resolveSource ─────────

    @Test fun knownBanks_abbreviationsAreUppercase() {
        AppConstants.KNOWN_BANKS.forEach { bank ->
            assertEquals(
                "KNOWN_BANKS abbreviation for ${bank.fullName} must be uppercase",
                bank.abbreviation.uppercase(), bank.abbreviation
            )
        }
    }

    @Test fun knownBanks_senderIdResolvesToAbbreviation() {
        AppConstants.KNOWN_BANKS.forEach { bank ->
            if (bank.senderId.isNotEmpty()) {
                val resolved = AppConstants.resolveSource(bank.senderId)
                assertEquals(
                    "resolveSource(\"${bank.senderId}\") should return \"${bank.abbreviation}\" for ${bank.fullName}",
                    bank.abbreviation, resolved
                )
            }
        }
    }
}
