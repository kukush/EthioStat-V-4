package com.ethiobalance.app.domain.usecase

import com.ethiobalance.app.domain.model.SmsScenario
import org.junit.Assert.*
import org.junit.Test
import java.util.*

class ParseSmsUseCaseTest {

    private val parseSmsUseCase = ParseSmsUseCase()

    // ───────── Financial SMS tests (unchanged) ─────────

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

    // ═════════════════════════════════════════════════════
    //  Case 1 — Multi-segment balance SMS
    // ═════════════════════════════════════════════════════

    private val case1Sms = "Dear Customer, your remaining amount " +
        "from Monthly Internet Package 12GB from telebirr to be expired after 30 days is 11458.902 MB with expiry date on 2026-05-16 at 11:42:05; " +
        "from 50 minutes + 200 MB Free is 44 minute and 3 second with expiry date on 2026-04-22 at 00:00:00; " +
        "from Monthly Recurring 125 Min and 63Min night package bonus is 52 minute and 1 second with expiry date on 2026-04-30 at 16:02:16; " +
        "from Monthly Recurring 125 Min and 63Min night package bonus is 125 minute and 0 second with expiry date on 2026-04-30 at 16:02:16; " +
        "from Create Your Own Package Monthly is 136 SMS with expiry date on 2026-04-19 at 00:22:19; " +
        "Enjoy 10% additional rewards by downloading telebirr SuperApp https://bit.ly/telebirr_SuperApp.Happy Holiday! Ethio telecom."

    private fun case1Result() = parseSmsUseCase("994", case1Sms, System.currentTimeMillis())

    @Test
    fun testCase1_packageCount() {
        val result = case1Result()
        val telecom = result.packages.filter { it.type in setOf("voice", "internet", "sms") }
        val internetCount = telecom.count { it.type == "internet" }
        val voiceCount = telecom.count { it.type == "voice" }
        val smsCount = telecom.count { it.type == "sms" }
        assertEquals("Expected 2 internet packages", 2, internetCount)
        assertEquals("Expected 3 voice packages", 3, voiceCount)
        assertEquals("Expected 1 SMS package", 1, smsCount)
        assertEquals("Expected 6 total telecom packages", 6, telecom.size)
    }

    @Test
    fun testCase1_monthlyInternet() {
        val result = case1Result()
        val pkg = result.packages.first { it.type == "internet" && it.subType == "Monthly" }
        assertEquals("internet", pkg.type)
        assertEquals("Monthly", pkg.subType)
        assertEquals(11459.0, pkg.remainingAmount, 0.01)
        assertEquals(12288.0, pkg.totalAmount, 0.01) // 12 GB = 12288 MB
        assertEquals("MB", pkg.unit)
    }

    @Test
    fun testCase1_freeInternet() {
        val result = case1Result()
        val pkg = result.packages.first { it.type == "internet" && it.subType == "Free" }
        assertEquals("internet", pkg.type)
        assertEquals("Free", pkg.subType)
        assertEquals(200.0, pkg.remainingAmount, 0.01)
        assertEquals(200.0, pkg.totalAmount, 0.01)
        assertEquals("MB", pkg.unit)
    }

    @Test
    fun testCase1_recurringVoice() {
        val result = case1Result()
        val pkg = result.packages.first { it.type == "voice" && it.subType == "Recurring" }
        assertEquals("voice", pkg.type)
        assertEquals("Recurring", pkg.subType)
        assertEquals(125.0, pkg.remainingAmount, 0.01)
        assertEquals(125.0, pkg.totalAmount, 0.01)
        assertEquals("MIN", pkg.unit)
    }

    @Test
    fun testCase1_nightVoice() {
        val result = case1Result()
        val pkg = result.packages.first { it.type == "voice" && it.subType == "Night" }
        assertEquals("voice", pkg.type)
        assertEquals("Night", pkg.subType)
        assertEquals(52.0, pkg.remainingAmount, 0.01) // 52 min + 1 sec → rounds to 52
        assertEquals(63.0, pkg.totalAmount, 0.01)
        assertEquals("MIN", pkg.unit)
    }

    @Test
    fun testCase1_freeVoice() {
        val result = case1Result()
        val pkg = result.packages.first { it.type == "voice" && it.subType == "Free" }
        assertEquals("voice", pkg.type)
        assertEquals("Free", pkg.subType)
        assertEquals(44.0, pkg.remainingAmount, 0.01) // 44 min + 3 sec → rounds to 44
        assertEquals(50.0, pkg.totalAmount, 0.01)
        assertEquals("MIN", pkg.unit)
    }

    @Test
    fun testCase1_customSms() {
        val result = case1Result()
        val pkg = result.packages.first { it.type == "sms" }
        assertEquals("sms", pkg.type)
        assertEquals("Custom", pkg.subType)
        assertEquals(136.0, pkg.remainingAmount, 0.01)
        assertEquals("SMS", pkg.unit)
    }

    @Test
    fun testCase1_compositeIds() {
        val result = case1Result()
        val telecom = result.packages.filter { it.type in setOf("voice", "internet", "sms") }
        telecom.forEach { pkg ->
            // ID format: {type}-{subType}-{YYYYMMDD}
            val parts = pkg.id.split("-")
            assertTrue("ID '${pkg.id}' should have 3 parts (type-subType-date)", parts.size == 3)
            assertTrue("ID '${pkg.id}' date part should be 8 digits", parts[2].length == 8 && parts[2].all { it.isDigit() })
        }
    }

    @Test
    fun testCase1_noDuplicateIds() {
        val result = case1Result()
        val telecom = result.packages.filter { it.type in setOf("voice", "internet", "sms") }
        val ids = telecom.map { it.id }
        assertEquals("All package IDs should be unique", ids.size, ids.toSet().size)
    }

    @Test
    fun testCase1_scenarioIsBalanceUpdate() {
        val result = case1Result()
        assertEquals(SmsScenario.BALANCE_UPDATE, result.scenario)
    }

    @Test
    fun testCase1_expiryDatesAreParsed() {
        val result = case1Result()
        result.packages.filter { it.type in setOf("voice", "internet", "sms") }.forEach { pkg ->
            assertTrue("Package '${pkg.id}' should have a future expiry date", pkg.expiryDate > 0)
        }
    }

    // ═════════════════════════════════════════════════════
    //  Case 2 — Single-segment Night Internet package
    // ═════════════════════════════════════════════════════

    private val case2Sms = "Dear customer You have received Night Internet package 600MB from telebirr " +
        "expire after 24 hr from 0. The package Will be expired on 17-04-2026 06:59:59."

    private fun case2Result() = parseSmsUseCase("994", case2Sms, System.currentTimeMillis())

    @Test
    fun testCase2_nightInternetCreated() {
        val result = case2Result()
        val internetPkgs = result.packages.filter { it.type == "internet" }
        assertEquals("Expected 1 internet package", 1, internetPkgs.size)
        val pkg = internetPkgs.first()
        assertEquals("Night", pkg.subType)
        assertEquals(600.0, pkg.remainingAmount, 0.01)
        assertEquals(600.0, pkg.totalAmount, 0.01)
        assertEquals("MB", pkg.unit)
    }

    @Test
    fun testCase2_expiryParsed() {
        val result = case2Result()
        val pkg = result.packages.first { it.type == "internet" }
        val cal = Calendar.getInstance().apply { timeInMillis = pkg.expiryDate }
        assertEquals("Expiry year", 2026, cal.get(Calendar.YEAR))
        assertEquals("Expiry month (0-indexed)", 3, cal.get(Calendar.MONTH)) // April = 3
        assertEquals("Expiry day", 17, cal.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun testCase2_compositeId() {
        val result = case2Result()
        val pkg = result.packages.first { it.type == "internet" }
        assertEquals("internet-Night-20260417", pkg.id)
    }

    // ═════════════════════════════════════════════════════
    //  Case 1 + Case 2 combined — no ID collision
    // ═════════════════════════════════════════════════════

    @Test
    fun testCase1And2_noIdCollision() {
        val r1 = case1Result()
        val r2 = case2Result()
        val ids1 = r1.packages.filter { it.type in setOf("voice", "internet", "sms") }.map { it.id }.toSet()
        val ids2 = r2.packages.filter { it.type == "internet" }.map { it.id }.toSet()
        val overlap = ids1.intersect(ids2)
        assertTrue("Case 1 and Case 2 IDs should not overlap, but got: $overlap", overlap.isEmpty())
    }

    // ═════════════════════════════════════════════════════
    //  Real-device SMS formats (from adb content query)
    //  Verbatim messages captured from actual device inbox
    // ═════════════════════════════════════════════════════

    // ── Awash: "paid X BIRR" school fee (actual inbox sender = "Awash") ──────

    @Test
    fun testAwash_paidBirr_schoolFee_amount() {
        val body = "You have paid 2,574 BIRR School Fee for YN/566/18 - Hermon  Faris in YENEGEW FRE.\n" +
                "Please Click the below link to download your receipt  https://eschool.awashbank.com/-5O18B\n" +
                "For any complaint or enquiry, please call 8980. Thank You. Awash Bank."
        val result = parseSmsUseCase("Awash", body, System.currentTimeMillis())

        assertEquals(SmsScenario.SELF_PURCHASE, result.scenario)
        assertEquals(2574.0, result.deductedAmount!!, 0.01)
        assertEquals(0.95f, result.confidence, 0.01f)
    }

    @Test
    fun testAwash_paidBirr_schoolFee_partyName() {
        val body = "You have paid 2,574 BIRR School Fee for YN/566/18 - Hermon  Faris in YENEGEW FRE.\n" +
                "Please Click the below link to download your receipt  https://eschool.awashbank.com/-5O18B\n" +
                "For any complaint or enquiry, please call 8980. Thank You. Awash Bank."
        val result = parseSmsUseCase("Awash", body, System.currentTimeMillis())

        assertNotNull("partyName should be extracted from Awash school fee SMS", result.partyName)
        assertTrue(
            "partyName should contain student name, got: '${result.partyName}'",
            result.partyName!!.contains("Faris", ignoreCase = true) ||
            result.partyName!!.contains("Hermon", ignoreCase = true)
        )
    }

    @Test
    fun testAwash_paidBirr_secondPayment() {
        val body = "You have paid 2,610 BIRR School Fee for YF2017/127 - Leul Faris in YENEGEW FRE.\n" +
                "Please Click the below link to download your receipt  https://eschool.awashbank.com/-5G6S8\n" +
                "For any complaint or enquiry, please call 8980. Thank You. Awash Bank."
        val result = parseSmsUseCase("Awash", body, System.currentTimeMillis())

        assertEquals(SmsScenario.SELF_PURCHASE, result.scenario)
        assertEquals(2610.0, result.deductedAmount!!, 0.01)
    }

    // ── Awash: ETB credit (existing format, verify still works) ──────────────

    @Test
    fun testAwash_creditEtb_fromParty() {
        val body = "Dear Customer, your Account 01320xxxxx1400 has been Credited with ETB 1300.00 on 2026-03-31 11:32:14 by ABEBECH WOLDE. Your balance now is ETB 17535.60. For any complaint or enquiry, please call 8980. Thank You. Awash Bank."
        val result = parseSmsUseCase("Awash", body, System.currentTimeMillis())

        assertEquals(SmsScenario.INCOME, result.scenario)
        assertEquals(1300.0, result.addedAmount!!, 0.01)
    }

    @Test
    fun testAwash_creditEtb_fromTransfer() {
        val body = "Dear Customer, ETB 50 has been credited to your account from SAMUEL MITIKU GUDINA on : 2026-01-28 20:12:21  with Txn ID: 260128201223255 . Your available balance is now ETB 50.00. Receipt  Link: https://awashpay.awashbank.com:8225/-2K7H8UP3KN-3JLL2T. Contact center  8980."
        val result = parseSmsUseCase("Awash", body, System.currentTimeMillis())

        assertEquals(SmsScenario.INCOME, result.scenario)
        assertEquals(50.0, result.addedAmount!!, 0.01)
    }

    // ── Dashen: debit (actual inbox sender = "DashenBank") ───────────────────

    @Test
    fun testDashen_debit_simple() {
        val body = "Dear Customer, your account '5128******011' is debited with ETB 2,000.00 on 17/04/2026 at 07:26:07 PM. Your current balance is ETB 73,108.33.\nDashen Bank - Always one step ahead!"
        val result = parseSmsUseCase("DashenBank", body, System.currentTimeMillis())

        assertEquals(SmsScenario.EXPENSE, result.scenario)
        assertEquals(2000.0, result.deductedAmount!!, 0.01)
        assertTrue(result.confidence >= 0.85f)
    }

    @Test
    fun testDashen_debit_withServiceCharge() {
        val body = "Dear Customer, your account '5128******011' is debited with ETB 804.80.Including Service charge of ETB 4.00 ,VAT(15%) of ETB .60 and DRRF fee of ETB .20 on 09/04/2026 at 08:47:47 AM. Your current balance is ETB 75,114.33.\nDashen Bank - Always one step ahead!."
        val result = parseSmsUseCase("DashenBank", body, System.currentTimeMillis())

        assertEquals(SmsScenario.EXPENSE, result.scenario)
        assertEquals(804.8, result.deductedAmount!!, 0.01)
    }

    @Test
    fun testDashen_credit() {
        val body = "Dear Customer, your account '5128******011' is credited with ETB 2,011.50 on 06/04/2026 at 09:09:47 AM. Your current balance is ETB 75,919.13.\nDashen Bank - Always one step ahead!"
        val result = parseSmsUseCase("DashenBank", body, System.currentTimeMillis())

        assertEquals(SmsScenario.INCOME, result.scenario)
        assertEquals(2011.5, result.addedAmount!!, 0.01)
    }

    @Test
    fun testDashen_resolveSource() {
        val resolved = com.ethiobalance.app.AppConstants.resolveSource("DashenBank")
        assertEquals("DASHEN", resolved)
    }

    // ── CBE: transfer with party name ─────────────────────────────────────────

    @Test
    fun testCbe_transfer_partyNameExtracted() {
        val body = "Dear Mrs, You have transfered ETB 500.00 to Alemtsehay Kifle on 18/04/2026 at 12:17:49 from your account 1*********3627. Your account has been debited with a S.charge of ETB 0.50 and VAT(15%) of ETB0.08 and Disaster Fund (5%) of ETB0.03, with a total of ETB 500.61. Your Current Balance is ETB 93,438.40. Thank you for Banking with CBE!"
        val result = parseSmsUseCase("CBE", body, System.currentTimeMillis())

        assertEquals(SmsScenario.EXPENSE, result.scenario)
        assertEquals(500.0, result.deductedAmount!!, 0.01)
        assertNotNull("partyName should be extracted from CBE transfer", result.partyName)
        assertTrue(
            "partyName should be 'Alemtsehay Kifle', got: '${result.partyName}'",
            result.partyName!!.contains("Alemtsehay", ignoreCase = true)
        )
    }

    @Test
    fun testCbe_debit_noPartyName() {
        val body = "Dear Mrs your Account 1*********3627 has been debited with ETB3,000.00. Service charge of  ETB 10.00 and VAT(15%) of ETB1.50 and Disaster Fund (5%) of ETB0.50 with a total of ETB 3012.00. Your Current Balance is ETB 90,426.40. Thank you for Banking with CBE!"
        val result = parseSmsUseCase("CBE", body, System.currentTimeMillis())

        assertEquals(SmsScenario.EXPENSE, result.scenario)
        assertEquals(3000.0, result.deductedAmount!!, 0.01)
    }

    // ── Telebirr: received ETB from CBE ──────────────────────────────────────

    @Test
    fun testTelebirr_receivedFromCBE() {
        val body = "Dear SELAMAWIT,\nYou have received  ETB 3,000.00 by transaction number DDK32XAG75 on 2026-04-20 19:52:02 from Commercial Bank of Ethiopia to your telebirr Account 251910960146 - SELAMAWIT ALEMU GETAHUN. Your current balance is ETB 3,222.65."
        val result = parseSmsUseCase("127", body, System.currentTimeMillis())

        assertEquals(SmsScenario.INCOME, result.scenario)
        assertEquals(3000.0, result.addedAmount!!, 0.01)
    }

    // ── Currency normalisation: BIRR and ETB both parse correctly ────────────

    @Test
    fun testCurrencyBirr_parsedSameAsEtb() {
        val birrBody = "You have paid 1,000 BIRR for services."
        val etbBody  = "You have paid 1,000 ETB for services."
        val birrResult = parseSmsUseCase("Awash", birrBody, System.currentTimeMillis())
        val etbResult  = parseSmsUseCase("CBE",   etbBody,  System.currentTimeMillis())

        assertEquals(birrResult.deductedAmount!!, etbResult.deductedAmount!!, 0.01)
    }
}
