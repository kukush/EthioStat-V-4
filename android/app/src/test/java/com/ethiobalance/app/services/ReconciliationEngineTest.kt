package com.ethiobalance.app.services

import com.ethiobalance.app.AppConstants
import com.ethiobalance.app.data.BalancePackageDao
import com.ethiobalance.app.data.BalancePackageEntity
import com.ethiobalance.app.data.SmsLogDao
import com.ethiobalance.app.data.SmsLogEntity
import com.ethiobalance.app.data.TransactionDao
import com.ethiobalance.app.data.TransactionEntity
import com.ethiobalance.app.data.TransactionSourceDao
import com.ethiobalance.app.data.TransactionSourceEntity
import com.ethiobalance.app.domain.model.ParsedSmsResult
import com.ethiobalance.app.domain.model.SmsScenario
import com.ethiobalance.app.domain.usecase.ParseSmsUseCase
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class ReconciliationEngineTest {

    private lateinit var smsLogDao: SmsLogDao
    private lateinit var transactionDao: TransactionDao
    private lateinit var balancePackageDao: BalancePackageDao
    private lateinit var transactionSourceDao: TransactionSourceDao
    private lateinit var parseSmsUseCase: ParseSmsUseCase
    private lateinit var engine: ReconciliationEngine

    @Before
    fun setUp() {
        smsLogDao = mockk(relaxed = true)
        transactionDao = mockk(relaxed = true)
        balancePackageDao = mockk(relaxed = true)
        transactionSourceDao = mockk(relaxed = true)
        parseSmsUseCase = mockk()
        engine = ReconciliationEngine(
            smsLogDao,
            transactionDao,
            balancePackageDao,
            transactionSourceDao,
            parseSmsUseCase
        )
    }

    @Test
    fun normalizeSender_normalizesNumericAndAlphaSenders() {
        assertEquals("127", engine.normalizeSender("+251127"))
        assertEquals("127", engine.normalizeSender("251127"))
        assertEquals("127", engine.normalizeSender("0127"))
        assertEquals("CBE", engine.normalizeSender(" cbe "))
        assertEquals("AWASHBANK", engine.normalizeSender("AwashBank"))
    }

    @Test
    fun processSms_returnsImmediatelyWhenSenderIsNull() = runTest {
        engine.processSms(sender = null, body = "body", timestamp = 1_000L)

        coVerify(exactly = 0) { smsLogDao.insert(any()) }
        coVerify(exactly = 0) { transactionDao.insert(any()) }
    }

    @Test
    fun processSms_skipsAlreadyLoggedMessageBeforeParsing() = runTest {
        val body = "duplicate message"
        val timestamp = 2_000L
        coEvery { smsLogDao.existsByHash("127", timestamp, body.hashCode()) } returns true

        engine.processSms(sender = "+251127", body = body, timestamp = timestamp)

        coVerify(exactly = 0) { transactionDao.insert(any()) }
    }

    @Test
    fun processSms_insertsExpenseWithConfiguredSource() = runTest {
        val body = "Paid ETB 42.00 to merchant. Ref R1"
        val timestamp = 3_000L
        val transactionSlot = slot<TransactionEntity>()
        val source = TransactionSourceEntity(
            abbreviation = "MYCBE",
            name = "My CBE",
            ussd = "*889#",
            senderId = "CBE,847"
        )

        coEvery { smsLogDao.existsByHash("CBE", timestamp, body.hashCode()) } returns false
        every { parseSmsUseCase("CBE", body, timestamp) } returns ParsedSmsResult(
            scenario = SmsScenario.EXPENSE,
            confidence = 0.95f,
            deductedAmount = 42.0,
            transactionCategory = "TRANSFER",
            reference = "R1",
            partyName = "Merchant",
            transactionSubType = "merchant_payment"
        )
        coEvery { transactionDao.existsById(any()) } returns false
        every { transactionSourceDao.getAllSources() } returns flowOf(listOf(source))
        coEvery { transactionDao.existsNearDuplicate("MYCBE", "EXPENSE", 42.0, timestamp, 60_000L) } returns false
        coEvery { smsLogDao.insert(any()) } just Runs
        coEvery { transactionDao.insert(capture(transactionSlot)) } just Runs

        engine.processSms(sender = "CBE", body = body, timestamp = timestamp)

        val inserted = transactionSlot.captured
        assertEquals("EXPENSE", inserted.type)
        assertEquals(42.0, inserted.amount, 0.0)
        assertEquals("TRANSFER", inserted.category)
        assertEquals("MYCBE", inserted.source)
        assertEquals("R1", inserted.reference)
        assertEquals("Merchant", inserted.partyName)
        assertEquals("merchant_payment", inserted.transactionSubType)
    }

    @Test
    fun processSms_forceReparseUpdatesExistingLogAndRefreshesTelecomPackages() = runTest {
        val body = "994 balance"
        val timestamp = 4_000L
        val packageEntity = BalancePackageEntity(
            id = "internet-night",
            simId = "",
            type = "internet",
            subType = "Night",
            totalAmount = 600.0,
            remainingAmount = 512.0,
            unit = "MB",
            expiryDate = 5_000L,
            isActive = true,
            source = "SMS",
            lastUpdated = 6_000L
        )
        val existingLog = SmsLogEntity(
            id = 99L,
            sender = "994",
            message = body,
            parsedType = AppConstants.SMS_LOG_TYPE_PROCESSING,
            confidence = 0.80f,
            processed = true,
            timestamp = timestamp,
            bodyHash = body.hashCode()
        )

        every { parseSmsUseCase("994", body, timestamp) } returns ParsedSmsResult(
            scenario = SmsScenario.BALANCE_UPDATE,
            confidence = 0.90f,
            packages = mutableListOf(packageEntity),
            isMultiSegmentBalance = true
        )
        coEvery { smsLogDao.getAllLogs() } returns listOf(existingLog)
        coEvery { smsLogDao.insert(any()) } just Runs
        every { transactionSourceDao.getAllSources() } returns flowOf(emptyList())
        coEvery { balancePackageDao.deleteTelecomPackages() } just Runs
        coEvery { balancePackageDao.insertOrUpdate(packageEntity) } just Runs

        engine.processSms(sender = "994", body = body, timestamp = timestamp, forceReparse = true)

        coVerify { smsLogDao.insert(match { it.id == 99L && it.confidence == 0.90f }) }
        coVerify { balancePackageDao.deleteTelecomPackages() }
        coVerify { balancePackageDao.insertOrUpdate(packageEntity) }
    }
}
