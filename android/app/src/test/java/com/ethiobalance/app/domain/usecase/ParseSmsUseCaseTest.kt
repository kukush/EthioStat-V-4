package com.ethiobalance.app.domain.usecase

import com.ethiobalance.app.domain.model.SmsScenario
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
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
}
