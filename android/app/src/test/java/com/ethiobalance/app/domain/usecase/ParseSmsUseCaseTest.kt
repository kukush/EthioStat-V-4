package com.ethiobalance.app.domain.usecase

import com.ethiobalance.app.domain.model.SmsScenario
import com.ethiobalance.app.domain.model.TelecomPackageType
import org.junit.Assert.*
import org.junit.Test
import java.util.*

class ParseSmsUseCaseTest {

    private val parseSmsUseCase = ParseSmsUseCase()

    @Test
    fun testCbeTransfer() {
        val body = "Dear xxx, You have transfered ETB 1,300.00 to Lenco Getachew on 18/03/2026 at 13:36:38 from your account 1*********5064. Your account has been debited..."
        val result = parseSmsUseCase("CBE", body, System.currentTimeMillis())
        
        assertEquals(SmsScenario.EXPENSE, result.scenario)
        assertEquals(1300.0, result.deductedAmount!!, 0.01)
        assertEquals("Lenco Getachew", result.partyName)
        assertEquals("Transfer", result.transactionSubType)
    }

    @Test
    fun testBoaCredit() {
        val body = "Dear xx, your account 2*****46 was credited with ETB 5.00 by A/r Tele Birr. Available Balance: ETB 1,114.15."
        val result = parseSmsUseCase("BOA", body, System.currentTimeMillis())
        
        assertEquals(SmsScenario.INCOME, result.scenario)
        assertEquals(5.0, result.addedAmount!!, 0.01)
        assertEquals("A/r Tele Birr", result.partyName)
        assertEquals("Received", result.transactionSubType)
    }

    @Test
    fun testTelebirrSendWithName() {
        val body = "You have sent 500.00 ETB to 0911XXXXXX (Abebe Kebede). Service fee is 2.00 ETB. Your current balance... Transaction ID: 123456789."
        val result = parseSmsUseCase("127", body, System.currentTimeMillis())
        
        assertEquals(SmsScenario.EXPENSE, result.scenario)
        assertEquals(500.0, result.deductedAmount!!, 0.01)
        assertEquals("0911XXXXXX (Abebe Kebede)", result.partyName)
        assertEquals("Transfer", result.transactionSubType)
    }

    @Test
    fun testTelebirrSendPhoneOnly() {
        val body = "You have sent 200.00 ETB to 0922123456. Service fee is 1.00 ETB. Your current balance... Transaction ID: 123."
        val result = parseSmsUseCase("127", body, System.currentTimeMillis())
        
        assertEquals(SmsScenario.EXPENSE, result.scenario)
        assertEquals("0922123456", result.partyName)
    }

    @Test
    fun testTelebirrUtility() {
        val body = "You have paid 350.00 ETB to Ethiopian Electric Utility for Reference No 1000XXXXXX. Your current balance is 800.00 ETB. Transaction ID: 556677889."
        val result = parseSmsUseCase("127", body, System.currentTimeMillis())
        
        assertEquals(SmsScenario.SELF_PURCHASE, result.scenario)
        assertEquals(350.0, result.deductedAmount!!, 0.01)
        assertEquals("Ethiopian Electric Utility", result.partyName)
    }

    @Test
    fun testTelebirrCashIn() {
        val body = "Your account has been credited with 2,000.00 ETB by Agent [Agent Name/ID]. Your current balance is 2,800.00 ETB. Transaction ID: 112233445."
        val result = parseSmsUseCase("127", body, System.currentTimeMillis())
        
        assertEquals(SmsScenario.INCOME, result.scenario)
        assertEquals(2000.0, result.addedAmount!!, 0.01)
        assertEquals("Agent [Agent Name/ID]", result.partyName)
        assertEquals("Received", result.transactionSubType)
    }

    // ───────── Multi-segment telecom SMS parsing ─────────

    private val multiSegmentSms = "Dear Customer, your remaining amount from " +
        "Monthly Recurring 125 Min and 63Min night package bonus is 52 minute and 1 second with expiry date on 2026-04-30 at 16:02:16;   " +
        "from Monthly Recurring 125 Min and 63Min night package bonus is 125 minute and 0 second with expiry date on 2026-04-30 at 16:02:16;    " +
        "from Monthly Internet Package 12GB from telebirr to be expired after 30 days is 2280.410 MB with expiry date on 2026-04-30 at 10:16:09;    " +
        "from Create Your Own Package Monthly is 136 SMS with expiry date on 2026-04-19 at 00:22:19; " +
        "from 50 minutes + 200 MB Free is 12 minute and 12 second with expiry date on 2026-04-15 at 00:00:00;    " +
        "Enjoy 10% additional rewards by downloading telebirr SuperApp https://bit.ly/telebirr_SuperApp.Happy Holiday! Ethio telecom."

    @Test
    fun testMultiSegment_producesCorrectPackageCount() {
        val result = parseSmsUseCase("994", multiSegmentSms, System.currentTimeMillis())
        val telecom = result.packages.filter { it.type in setOf("voice", "internet", "sms") }
        assertEquals("Expected 5 telecom packages (3 voice + 1 internet + 1 sms)", 5, telecom.size)
    }

    @Test
    fun testMultiSegment_nightVoice() {
        val result = parseSmsUseCase("994", multiSegmentSms, System.currentTimeMillis())
        val nightVoice = result.packages.first { it.id == "voice-0" }

        assertEquals(52.0, nightVoice.remainingAmount, 0.01)
        assertEquals(63.0, nightVoice.totalAmount, 0.01)
        assertEquals("MIN", nightVoice.unit)
        assertEquals("Night Voice", nightVoice.simId)
    }

    @Test
    fun testMultiSegment_recurringVoice() {
        val result = parseSmsUseCase("994", multiSegmentSms, System.currentTimeMillis())
        val recurring = result.packages.first { it.id == "voice-1" }

        assertEquals(125.0, recurring.remainingAmount, 0.01)
        assertEquals(125.0, recurring.totalAmount, 0.01)
        assertEquals("MIN", recurring.unit)
        assertEquals("Recurring Voice", recurring.simId)
    }

    @Test
    fun testMultiSegment_bonusVoice() {
        val result = parseSmsUseCase("994", multiSegmentSms, System.currentTimeMillis())
        val bonus = result.packages.first { it.id == "voice-2" }

        assertEquals(12.0, bonus.remainingAmount, 0.01)
        assertEquals("MIN", bonus.unit)
        assertEquals("Bonus Voice", bonus.simId)
    }

    @Test
    fun testMultiSegment_internetPackage() {
        val result = parseSmsUseCase("994", multiSegmentSms, System.currentTimeMillis())
        val internet = result.packages.first { it.type == "internet" }

        assertEquals(2280.41, internet.remainingAmount, 0.01)
        assertEquals(12288.0, internet.totalAmount, 0.01)  // 12 GB = 12288 MB
        assertEquals("MB", internet.unit)
        assertEquals("Monthly Internet Package 12GB", internet.simId)
    }

    @Test
    fun testMultiSegment_smsPackage() {
        val result = parseSmsUseCase("994", multiSegmentSms, System.currentTimeMillis())
        val sms = result.packages.first { it.type == "sms" }

        assertEquals(136.0, sms.remainingAmount, 0.01)
        assertEquals("SMS", sms.unit)
    }

    @Test
    fun testMultiSegment_noSimOneSuffix() {
        val result = parseSmsUseCase("994", multiSegmentSms, System.currentTimeMillis())
        result.packages.forEach { pkg ->
            assertFalse("Package ID '${pkg.id}' should not contain -sim1", pkg.id.contains("-sim1"))
        }
    }

    @Test
    fun testMultiSegment_scenarioIsBalanceUpdate() {
        val result = parseSmsUseCase("994", multiSegmentSms, System.currentTimeMillis())
        assertEquals(SmsScenario.BALANCE_UPDATE, result.scenario)
    }

    @Test
    fun testMultiSegment_expiryDatesAreParsed() {
        val result = parseSmsUseCase("994", multiSegmentSms, System.currentTimeMillis())
        result.packages.filter { it.type in setOf("voice", "internet", "sms") }.forEach { pkg ->
            assertTrue("Package '${pkg.id}' should have a future expiry date", pkg.expiryDate > 0)
        }
    }

    @Test
    fun testMultiSegment_voiceTotalSum() {
        val result = parseSmsUseCase("994", multiSegmentSms, System.currentTimeMillis())
        val voiceTotal = result.packages.filter { it.type == "voice" }.sumOf { it.remainingAmount }
        assertEquals("Sum of voice remaining: 52 + 125 + 12 = 189", 189.0, voiceTotal, 0.01)
    }

    @Test
    fun testMultiSegment_parsePackageDetailsDoesNotOverwrite() {
        val result = parseSmsUseCase("994", multiSegmentSms, System.currentTimeMillis())
        // Should not have generic "voice" or "internet" entries from parsePackageDetails fallback
        val hasGenericVoice = result.packages.any { it.id == "voice" }
        assertFalse("parsePackageDetails should not create a generic 'voice' entry", hasGenericVoice)
    }

    // ───────── TelecomPackageType enum ─────────

    @Test
    fun testTelecomPackageType_classifySms() {
        assertEquals(TelecomPackageType.SMS, TelecomPackageType.classify("anything", "sms"))
    }

    @Test
    fun testTelecomPackageType_classifyBonus() {
        assertEquals(TelecomPackageType.BONUS_FUND, TelecomPackageType.classify("anything", "bonus"))
    }

    @Test
    fun testTelecomPackageType_classifyInternet() {
        assertEquals(TelecomPackageType.DATA, TelecomPackageType.classify("Monthly Internet 12GB", "internet"))
    }

    @Test
    fun testTelecomPackageType_classifyNightVoice() {
        assertEquals(
            TelecomPackageType.NIGHT_VOICE,
            TelecomPackageType.classify("Monthly Recurring 125 Min and 63Min night package bonus", "voice")
        )
    }

    @Test
    fun testTelecomPackageType_classifyRecurringVoice() {
        assertEquals(
            TelecomPackageType.RECURRING_VOICE,
            TelecomPackageType.classify("Monthly Recurring plan", "voice")
        )
    }

    @Test
    fun testTelecomPackageType_classifyFreeVoice() {
        assertEquals(
            TelecomPackageType.BONUS_VOICE,
            TelecomPackageType.classify("50 minutes + 200 MB Free", "voice")
        )
    }

    @Test
    fun testTelecomPackageType_classifyVoicePair_night() {
        assertEquals(TelecomPackageType.NIGHT_VOICE, TelecomPackageType.classifyVoicePair(52.0, 63.0))
    }

    @Test
    fun testTelecomPackageType_classifyVoicePair_recurring() {
        assertEquals(TelecomPackageType.RECURRING_VOICE, TelecomPackageType.classifyVoicePair(125.0, 63.0))
    }

    @Test
    fun testTelecomPackageType_classifyVoicePair_edgeAtQuota() {
        assertEquals(TelecomPackageType.NIGHT_VOICE, TelecomPackageType.classifyVoicePair(63.0, 63.0))
    }

    @Test
    fun testTelecomPackageType_unknownFallback() {
        assertEquals(TelecomPackageType.UNKNOWN, TelecomPackageType.classify("random text", "other"))
    }
}
