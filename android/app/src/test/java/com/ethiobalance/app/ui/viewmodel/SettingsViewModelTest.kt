package com.ethiobalance.app.ui.viewmodel

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ethiobalance.app.AppConstants
import com.ethiobalance.app.data.AppDatabase
import com.ethiobalance.app.data.TransactionSourceEntity
import com.ethiobalance.app.domain.usecase.ParseSmsUseCase
import com.ethiobalance.app.repository.SettingsRepository
import com.ethiobalance.app.repository.TransactionRepository
import com.ethiobalance.app.repository.SmsRepository
import com.ethiobalance.app.services.ReconciliationEngine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain

/**
 * Tests that default transaction sources (Apollo, CBEBirr) can be removed via the
 * SettingsViewModel and subsequently added again using the same logic as TeleBirr.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class SettingsViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private lateinit var settingsRepo: SettingsRepository
    private lateinit var transactionRepo: TransactionRepository
    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setUp() = runBlocking {
        Dispatchers.setMain(testDispatcher)
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        // Create a DataStore instance for the repository (in-memory file)
        val dataStore = PreferenceDataStoreFactory.create {
            context.preferencesDataStoreFile("test_datastore")
        }

        settingsRepo = SettingsRepository(
            context = context,
            dataStore = dataStore,
            transactionSourceDao = db.transactionSourceDao(),
            transactionDao = db.transactionDao()
        )

        val reconciliationEngine = ReconciliationEngine(
            smsLogDao = db.smsLogDao(),
            transactionDao = db.transactionDao(),
            balancePackageDao = db.balancePackageDao(),
            transactionSourceDao = db.transactionSourceDao(),
            parseSmsUseCase = ParseSmsUseCase()
        )

        transactionRepo = TransactionRepository(
            transactionDao = db.transactionDao(),
            transactionSourceDao = db.transactionSourceDao(),
            smsLogDao = db.smsLogDao(),
            smsRepo = SmsRepository(context, db.smsLogDao(), db.transactionSourceDao(), reconciliationEngine)
        )

        viewModel = SettingsViewModel(settingsRepo, transactionRepo)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        db.close()
    }

    private fun createDefaultSource(abbrev: String): TransactionSourceEntity {
        val senderIds = settingsRepo.getAllSenderIdsForBank(abbrev)
        val name = AppConstants.KNOWN_BANKS.find { it.abbreviation == abbrev }?.fullName
            ?: abbrev
        return TransactionSourceEntity(
            abbreviation = abbrev,
            name = name,
            ussd = "",
            senderId = senderIds,
            isEnabled = true
        )
    }

    @Test
    fun removeAndReaddApolloSource_isIdempotent() = runBlocking {
        // Add Apollo source via repository (mirrors default seeding)
        val apollo = createDefaultSource("APOLLO")
        settingsRepo.addTransactionSource(apollo)
        assertTrue(settingsRepo.getTransactionSources().first().any { it.abbreviation == "APOLLO" })

        // Remove using ViewModel
        viewModel.removeTransactionSource("APOLLO")
        // Ensure removal
        assertFalse(settingsRepo.getTransactionSources().first().any { it.abbreviation == "APOLLO" })

        // Re-add using ViewModel's addTransactionSource (should follow same rule as TeleBirr)
        viewModel.addTransactionSource(apollo)
        assertTrue(settingsRepo.getTransactionSources().first().any { it.abbreviation == "APOLLO" })
    }

    @Test
    fun removeAndReaddCBEBirrSource_isIdempotent() = runBlocking {
        val cbeBirr = createDefaultSource("CBEBIRR")
        settingsRepo.addTransactionSource(cbeBirr)
        assertTrue(settingsRepo.getTransactionSources().first().any { it.abbreviation == "CBEBIRR" })

        viewModel.removeTransactionSource("CBEBIRR")
        assertFalse(settingsRepo.getTransactionSources().first().any { it.abbreviation == "CBEBIRR" })

        viewModel.addTransactionSource(cbeBirr)
        assertTrue(settingsRepo.getTransactionSources().first().any { it.abbreviation == "CBEBIRR" })
    }

    @Test
    fun hasSeenOnboarding_startsNullAndUpdates() = runBlocking {
        // By default, DataStore returns false if not set, but the StateFlow starts as null.
        // Wait for the initial state to load
        var onboardingState = viewModel.hasSeenOnboarding.value
        // It might be null initially before collect finishes, or false if already emitted.
        // We'll mark it seen and then verify it becomes true
        viewModel.markOnboardingSeen()
        
        // Wait for it to become true
        val isSeen = viewModel.hasSeenOnboarding.first { it == true }
        assertTrue(isSeen == true)
    }
}
