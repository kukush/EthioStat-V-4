package com.ethiobalance.app.services

import org.junit.Assert.*
import org.junit.Test

class SmsParserTest {

    private fun assertPackage(
        packages: List<com.ethiobalance.app.data.BalancePackageEntity>,
        expectedType: String,
        expectedAmount: Double
    ) {
        val pkg = packages.find { it.type == expectedType }
        assertNotNull("Expected to find package of type $expectedType", pkg)
        assertEquals("Amount mismatch for $expectedType", expectedAmount, pkg!!.remainingAmount, 0.01)
    }

    @Test
    fun testTelebirrInternetPurchase() {
        val sender = "127"
        val body = "You have successfully paid 100.00 ETB for 5GB Monthly Internet package via Telebirr. Your new balance is 2400.00 ETB."
        val result = SmsParser.parse(sender, body, System.currentTimeMillis())

        assertEquals(SmsScenario.SELF_PURCHASE, result.scenario)
        assertEquals(100.0, result.deductedAmount)
        assertEquals("PURCHASE", result.transactionCategory)
        
        // 5GB * 1024 = 5120 MB
        assertPackage(result.packages, "internet", 5120.0)
        assertPackage(result.packages, "airtime", 2400.0)
    }

    @Test
    fun testTelebirrVoicePurchase() {
        val sender = "127"
        val body = "You have successfully paid 50.00 ETB for 100 Minutes Weekly Voice package. New Telebirr balance: 2350.00 ETB."
        val result = SmsParser.parse(sender, body, System.currentTimeMillis())

        assertEquals(SmsScenario.SELF_PURCHASE, result.scenario)
        assertEquals(50.0, result.deductedAmount)
        
        assertPackage(result.packages, "voice", 100.0)
        assertPackage(result.packages, "airtime", 2350.0)
    }

    @Test
    fun testTelebirrSmsPurchase() {
        val sender = "127"
        val body = "You have successfully paid 20.00 ETB for 200 Weekly SMS package. Transaction Ref: SMS998."
        val result = SmsParser.parse(sender, body, System.currentTimeMillis())

        assertEquals(SmsScenario.SELF_PURCHASE, result.scenario)
        assertEquals(20.0, result.deductedAmount)
        
        assertPackage(result.packages, "sms", 200.0)
    }

    @Test
    fun testMerchantPayment() {
        val sender = "TELEBIRR"
        val body = "You have paid 450.50 ETB to Merchant ABC. New balance 1899.50 ETB."
        val result = SmsParser.parse(sender, body, System.currentTimeMillis())

        assertEquals(SmsScenario.SELF_PURCHASE, result.scenario)
        assertEquals(450.50, result.deductedAmount)
        assertEquals(1899.50, result.airtimeBalance)
    }

    @Test
    fun testCbeSalaryDeposit() {
        val sender = "CBEBirr"
        // Needs proper initialization of AppConstants.SMS_SENDER_WHITELIST, but AppConstants handles that natively
        val body = "Your account has been credited with 15,500.00 ETB. Reference: SALARY-MAR-2026."
        val result = SmsParser.parse(sender, body, System.currentTimeMillis())

        assertEquals(SmsScenario.INCOME, result.scenario)
        assertEquals(15500.0, result.addedAmount)
    }

    @Test
    fun testEthioTelecomDirectGiftReceived() {
        val sender = "251994"
        val body = "Dear customer, you have received a gift of 500MB Daily Internet package from 0911XXXXXX. Valid until tomorrow."
        val result = SmsParser.parse(sender, body, System.currentTimeMillis())

        assertEquals(SmsScenario.RECHARGE_OR_GIFT_RECEIVED, result.scenario)
        assertPackage(result.packages, "internet", 500.0)
    }

    @Test
    fun testAirtimeRecharge() {
        val sender = "805"
        val body = "Your account has been recharged with 50.00 ETB."
        val result = SmsParser.parse(sender, body, System.currentTimeMillis())

        assertEquals(SmsScenario.RECHARGE_OR_GIFT_RECEIVED, result.scenario)
        assertTrue(result.isRecharge)
        assertEquals(50.0, result.addedAmount)
    }

    @Test
    fun testLoanTaken() {
        val sender = "810"
        val body = "You have taken a loan of 15.00 ETB."
        val result = SmsParser.parse(sender, body, System.currentTimeMillis())

        assertEquals(SmsScenario.LOAN_TAKEN, result.scenario)
        assertEquals(15.0, result.addedAmount)
    }

    @Test
    fun testLoanRepayment() {
        val sender = "810"
        val body = "Repayment of 16.50 ETB has been deducted from your account."
        val result = SmsParser.parse(sender, body, System.currentTimeMillis())

        assertEquals(SmsScenario.EXPENSE, result.scenario)
        assertEquals(16.50, result.deductedAmount)
        assertEquals("REPAYMENT", result.transactionCategory)
    }

    @Test
    fun testAirtimeBalanceQuery() {
        val sender = "804"
        val body = "Your balance is 145.50 ETB. Thank you."
        val result = SmsParser.parse(sender, body, System.currentTimeMillis())

        assertEquals(SmsScenario.BALANCE_QUERY, result.scenario)
        assertEquals(145.50, result.airtimeBalance)
        assertPackage(result.packages, "airtime", 145.50)
    }

    // ── Internet / Data parsing ────────────────────────────────────────────────────

    @Test
    fun testInternetPurchaseTelebirrFormat() {
        // "5GB Monthly Internet package" - number+unit BEFORE keyword
        val result = SmsParser.parse("127",
            "You have successfully paid 100.00 ETB for 5GB Monthly Internet package via Telebirr. Your new balance is 2400.00 ETB.",
            System.currentTimeMillis())
        assertEquals(SmsScenario.SELF_PURCHASE, result.scenario)
        assertEquals(100.0, result.deductedAmount)
        assertPackage(result.packages, "internet", 5 * 1024.0) // 5GB → 5120 MB
    }

    @Test
    fun testInternetKeywordBeforeUnit() {
        // "Internet: 2.5GB" - keyword then amount+unit
        val result = SmsParser.parse("251994",
            "Your active package summary: Internet: 2.5GB with expiry date 2026-12-31.",
            System.currentTimeMillis())
        assertNotNull(result.packages.find { it.type == "internet" })
        val pkg = result.packages.first { it.type == "internet" }
        assertEquals(2.5 * 1024.0, pkg.remainingAmount, 1.0)
    }

    @Test
    fun testInternetRemainingBalanceFormat() {
        // "You have 450MB data remaining"
        val result = SmsParser.parse("251994",
            "You have 450MB data remaining until 2026-06-30.",
            System.currentTimeMillis())
        assertNotNull(result.packages.find { it.type == "internet" })
        assertPackage(result.packages, "internet", 450.0)
    }

    @Test
    fun testInternetAmharicFormat() {
        // Amharic: "ኢንተርኔት 500MB"
        val result = SmsParser.parse("251994",
            "ኢንተርኔት 500MB የኙር ግዐት 2026-08-01",
            System.currentTimeMillis())
        assertNotNull(result.packages.find { it.type == "internet" })
        assertPackage(result.packages, "internet", 500.0)
    }

    @Test
    fun testInternetOromoFormat() {
        // Afaan Oromo: "Intarneetii 1GB"
        val result = SmsParser.parse("251994",
            "Intarneetii 1GB kennamee jira.",
            System.currentTimeMillis())
        assertNotNull(result.packages.find { it.type == "internet" })
        assertPackage(result.packages, "internet", 1024.0) // 1GB → 1024 MB
    }

    @Test
    fun testInternetMBStoredInMB() {
        // MB package should be stored as-is (not converted)
        val result = SmsParser.parse("127",
            "You have successfully paid 30.00 ETB for 200MB Daily Internet package. Your Telebirr balance: 100.00 ETB.",
            System.currentTimeMillis())
        assertPackage(result.packages, "internet", 200.0)
    }


    @Test
    fun testServiceFeeDeduction() {
        val sender = "127"
        val body = "Service fee of 5.50 ETB deducted."
        val result = SmsParser.parse(sender, body, System.currentTimeMillis())

        assertEquals(SmsScenario.EXPENSE, result.scenario)
        assertEquals(5.50, result.deductedAmount)
        assertEquals("FEE", result.transactionCategory)
    }

    @Test
    fun testStandaloneBonusAward() {
        val sender = "251994"
        val body = "You have been awarded an ETB 7.50 bonus for recharging your prepaid account."
        val result = SmsParser.parse(sender, body, System.currentTimeMillis())

        // Because it lacks financial flags, it defaults to BALANCE_UPDATE when pure asset is modified
        assertEquals(SmsScenario.BALANCE_UPDATE, result.scenario)
        assertPackage(result.packages, "bonus", 7.50)
    }

    @Test
    fun testTransferSent() {
        val sender = "127"
        val body = "You have transferred 220.00 ETB to Worku Mengistu."
        val result = SmsParser.parse(sender, body, System.currentTimeMillis())

        assertEquals(SmsScenario.EXPENSE, result.scenario)
        assertEquals(220.0, result.deductedAmount)
        assertEquals("GIFT", result.transactionCategory)
    }

    @Test
    fun testMultiSegmentTelebirrParsing() {
        val sender = "251994"
        val body = "Voice is 100 Min;  from Internet is 500 MB;  from Bonus Fund is 7.50 Birr with expiry date on 2026-10-30."
        val result = SmsParser.parse(sender, body, System.currentTimeMillis())

        // Ensure multi-segment processing activates cleanly
        assertPackage(result.packages, "voice", 100.0)
        assertPackage(result.packages, "internet", 500.0)
        assertPackage(result.packages, "bonus", 7.50)
    }
    
    @Test
    fun testLanguageDetectionAmharic() {
        val sender = "251994"
        val body = "ቀሪ ሒሳብ 50.00 ብር"
        val result = SmsParser.parse(sender, body, System.currentTimeMillis())
        
        assertEquals(SmsScenario.BALANCE_QUERY, result.scenario)
        assertEquals(50.0, result.airtimeBalance)
    }
}
