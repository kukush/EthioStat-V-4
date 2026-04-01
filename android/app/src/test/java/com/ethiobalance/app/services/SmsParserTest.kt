package com.ethiobalance.app.services

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SmsParserTest {

    @Test
    fun testSelfPurchaseDataPackage() {
        val body = "Dear customer, you have successfully purchased 1GB Daily Internet package. Your remaining balance is 40.00 ETB. Valid until tomorrow."
        val result = SmsParser.parse("251994", body)

        assertEquals(SmsScenario.SELF_PURCHASE, result.scenario)
        assertTrue(result.confidence > 0.7f)
        assertEquals(1, result.packages.size)
        assertEquals("DATA_AIRTIME", result.packages[0].type)
        assertEquals(1.0, result.packages[0].totalAmount, 0.0)
        assertEquals("GB", result.packages[0].unit)
        assertEquals(null, result.addedAmount) // Assets gained, but no cash gained
    }

    @Test
    fun testTelebirrPackagePurchaseExpense() {
        val body = "You have successfully paid 19.00 ETB for 1GB Daily Data package."
        val result = SmsParser.parse("830", body)

        assertEquals(SmsScenario.SELF_PURCHASE, result.scenario)
        assertEquals(19.0, result.deductedAmount)
    }

    @Test
    fun testGiftSentTransfer() {
        val body = "You have transferred 25.00 ETB to 0911223344. Your new balance is 15.00 ETB."
        val result = SmsParser.parse("806", body)

        assertEquals(SmsScenario.GIFT_SENT, result.scenario)
        assertEquals(25.0, result.deductedAmount)
        assertEquals(0, result.packages.size) // No asset gained
    }

    @Test
    fun testLoanTakenIncome() {
        val body = "Dear customer, you have taken a loan of 10.00 ETB. The total amount to be repaid is 11.50 ETB."
        val result = SmsParser.parse("810", body)

        assertEquals(SmsScenario.LOAN_TAKEN, result.scenario)
        assertEquals(10.0, result.addedAmount)
    }

    @Test
    fun testRechargeIncome() {
        val body = "Your account has been recharged with 50.00 ETB."
        val result = SmsParser.parse("805", body)

        assertEquals(SmsScenario.RECHARGE_OR_GIFT_RECEIVED, result.scenario)
        assertEquals(50.0, result.addedAmount)
    }

    @Test
    fun testGiftReceivedAssetGain() {
        val body = "Dear customer, you have received a gift of 500MB Daily Internet package from 0911XXXXXX. Valid until tomorrow."
        val result = SmsParser.parse("251994", body)

        assertEquals(SmsScenario.RECHARGE_OR_GIFT_RECEIVED, result.scenario)
        assertEquals(1, result.packages.size)
        assertEquals("DATA_AIRTIME", result.packages[0].type)
        assertEquals(500.0, result.packages[0].totalAmount, 0.0)
    }
}
