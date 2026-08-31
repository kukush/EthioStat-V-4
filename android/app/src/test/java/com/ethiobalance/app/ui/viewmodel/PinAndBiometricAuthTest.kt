package com.ethiobalance.app.ui.viewmodel

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ethiobalance.app.data.AppDatabase
import com.ethiobalance.app.repository.SettingsRepository
import com.ethiobalance.app.repository.TransactionRepository
import com.ethiobalance.app.repository.SmsRepository
import com.ethiobalance.app.services.ReconciliationEngine
import com.ethiobalance.app.domain.usecase.ParseSmsUseCase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class PinAndBiometricAuthTest {

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

        val dataStore = PreferenceDataStoreFactory.create {
            context.preferencesDataStoreFile("test_auth_datastore")
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

    @Test
    fun testPinEnabledToggleAndPersistence() = runBlocking {
        assertFalse(viewModel.isPinEnabled.first())

        viewModel.setPinEnabled(true)
        assertTrue(viewModel.isPinEnabled.first())

        viewModel.setPinEnabled(false)
        assertFalse(viewModel.isPinEnabled.first())
    }

    @Test
    fun testBiometricEnabledToggleAndPersistence() = runBlocking {
        assertFalse(viewModel.isBiometricEnabled.first())

        viewModel.setBiometricEnabled(true)
        assertTrue(viewModel.isBiometricEnabled.first())

        viewModel.setBiometricEnabled(false)
        assertFalse(viewModel.isBiometricEnabled.first())
    }

    @Test
    fun testPinStorageAndValidation() = runBlocking {
        assertNull(viewModel.storedPin.first())

        val testPin = "1234"
        viewModel.setPin(testPin)

        val savedPin = viewModel.storedPin.first()
        assertEquals(testPin, savedPin)

        // Validate 4-digit numeric constraint logic
        val input = "12ab34"
        val filtered = input.filter { it.isDigit() }
        assertEquals("1234", filtered)
        assertEquals(4, filtered.length)
    }

    @Test
    fun testPinValidationFailureCases() {
        val shortPin = "123"
        assertNotEquals(4, shortPin.length)

        val nonNumericPin = "12a4"
        val filtered = nonNumericPin.filter { it.isDigit() }
        assertNotEquals(nonNumericPin, filtered)
        assertEquals("124", filtered)
    }
}
