package com.ethiobalance.app.services

import com.ethiobalance.app.data.*
import com.ethiobalance.app.domain.model.ParsedSmsResult
import com.ethiobalance.app.domain.model.SmsScenario
import com.ethiobalance.app.domain.usecase.ParseSmsUseCase
import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import java.util.*

class ReconciliationEngineTest {

    private lateinit var smsLogDao: SmsLogDao
    private lateinit var transactionDao: TransactionDao
    private lateinit var balancePackageDao: BalancePackageDao
    private lateinit var parseSmsUseCase: ParseSmsUseCase
    private lateinit var reconciliationEngine: ReconciliationEngine

    @Before
    fun setup() {
        smsLogDao = mockk(relaxed = true)
        transactionDao = mockk(relaxed = true)
        balancePackageDao = mockk(relaxed = true)
        parseSmsUseCase = mockk()
        
        reconciliationEngine = ReconciliationEngine(
            smsLogDao,
            transactionDao,
            balancePackageDao,
            parseSmsUseCase
        )
    }

    @Test
    fun `test Telebirr 127 transaction processing`() = runBlocking {
        val sender = "127"
        val body = "You have received 100.00 ETB from 0911223344. Transaction ID: TX123"
        val timestamp = System.currentTimeMillis()
        
        val parsedResult = ParsedSmsResult(
            scenario = SmsScenario.INCOME,
            confidence = 0.95f,
            addedAmount = 100.0,
            reference = "TX123",
            partyName = "0911223344"
        )

        coEvery { parseSmsUseCase(any(), any(), any()) } returns parsedResult
        coEvery { smsLogDao.existsByHash(any(), any(), any()) } returns false
        coEvery { transactionDao.existsById(any()) } returns false

        reconciliationEngine.processSms(sender, body, timestamp)

        coVerify { 
            transactionDao.insert(match { 
                it.amount == 100.0 && it.type == "INCOME" && it.source == "TeleBirr"
            }) 
        }
    }

    @Test
    fun `test low confidence message is skipped`() = runBlocking {
        val sender = "127"
        val body = "Invalid message"
        val timestamp = System.currentTimeMillis()
        
        val parsedResult = ParsedSmsResult(
            scenario = SmsScenario.UNKNOWN,
            confidence = 0.5f
        )

        coEvery { parseSmsUseCase(any(), any(), any()) } returns parsedResult
        coEvery { smsLogDao.existsByHash(any(), any(), any()) } returns false

        reconciliationEngine.processSms(sender, body, timestamp)

        coVerify(exactly = 0) { transactionDao.insert(any()) }
    }
}
