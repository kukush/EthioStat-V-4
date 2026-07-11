package com.ethiobalance.app.ui.viewmodel

import com.ethiobalance.app.AppConstants
import com.ethiobalance.app.domain.usecase.GetFinancialSummaryUseCase
import com.ethiobalance.app.repository.BalanceRepository
import com.ethiobalance.app.repository.SettingsRepository
import com.ethiobalance.app.repository.SmsRepository
import com.ethiobalance.app.repository.TransactionRepository
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for HomeViewModel sync logic.
 *
 * Covers:
 * - triggerManualSync sets isSyncing = true
 * - triggerManualSync computes daysSinceLastScan correctly (90 if 0, else based on time diff)
 * - triggerManualSync calls smsRepo.scanAllTransactionSources
 * - triggerManualSync updates lastScannedTimestamp
 * - triggerManualSync emits success/failure messages to syncEvent
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var transactionRepo: TransactionRepository
    private lateinit var balanceRepo: BalanceRepository
    private lateinit var settingsRepo: SettingsRepository
    private lateinit var smsRepo: SmsRepository
    private lateinit var getFinancialSummaryUseCase: GetFinancialSummaryUseCase

    private lateinit var viewModel: HomeViewModel

    private val lastScannedFlow = MutableStateFlow(0L)

    @Before
    fun setup() {
        mockkStatic(android.util.Log::class)
        every { android.util.Log.d(any(), any()) } returns 0
        every { android.util.Log.e(any(), any(), any()) } returns 0
        Dispatchers.setMain(testDispatcher)

        transactionRepo = mockk(relaxed = true) {
            every { getAllTransactions() } returns flowOf(emptyList())
        }
        balanceRepo = mockk(relaxed = true) {
            every { getAllPackages() } returns flowOf(emptyList())
        }
        settingsRepo = mockk(relaxed = true) {
            every { getTransactionSources() } returns flowOf(emptyList())
            every { userName } returns flowOf("User")
            every { userPhone } returns flowOf("")
            every { language } returns flowOf("en")
            every { theme } returns flowOf("light")
            every { lastScannedTimestamp } returns lastScannedFlow
            coEvery { setLastScannedTimestamp(any()) } returns Unit
        }
        smsRepo = mockk(relaxed = true)
        getFinancialSummaryUseCase = mockk(relaxed = true)

        viewModel = HomeViewModel(
            transactionRepo,
            balanceRepo,
            settingsRepo,
            smsRepo,
            getFinancialSummaryUseCase
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun triggerManualSync_initialScan_scans90Days() = runTest {
        // Arrange
        lastScannedFlow.value = 0L
        coEvery { smsRepo.scanAllTransactionSources(any()) } returns 10

        val syncEvents = mutableListOf<String>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.syncEvent.collect { syncEvents.add(it) }
        }

        // Act
        viewModel.triggerManualSync()

        // Assert
        coVerify { smsRepo.scanAllTransactionSources(90) }
        coVerify { settingsRepo.setLastScannedTimestamp(any()) }
        
        assertEquals(1, syncEvents.size)
        assertTrue(syncEvents.first().contains("Synced 10 transactions"))

        job.cancel()
    }

    @Test
    fun triggerManualSync_recentScan_computesCorrectDays() = runTest {
        // Arrange
        val now = System.currentTimeMillis()
        val twoDaysAgo = now - (2 * AppConstants.MILLISECONDS_PER_DAY)
        lastScannedFlow.value = twoDaysAgo
        coEvery { smsRepo.scanAllTransactionSources(any()) } returns 5

        // Act
        viewModel.triggerManualSync()

        // Assert
        // Expecting 2 + 1 = 3 days
        coVerify { smsRepo.scanAllTransactionSources(3) }
        coVerify { settingsRepo.setLastScannedTimestamp(any()) }
    }

    @Test
    fun triggerManualSync_onFailure_emitsErrorEvent() = runTest {
        // Arrange
        lastScannedFlow.value = 0L
        coEvery { smsRepo.scanAllTransactionSources(any()) } throws RuntimeException("Database error")

        val syncEvents = mutableListOf<String>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.syncEvent.collect { syncEvents.add(it) }
        }

        // Act
        viewModel.triggerManualSync()

        // Assert
        assertEquals(1, syncEvents.size)
        assertTrue(syncEvents.first().contains("Sync failed: Database error"))
        assertFalse(viewModel.isSyncing.value)

        job.cancel()
    }
}
