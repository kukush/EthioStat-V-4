package com.ethiobalance.app.repository

import android.Manifest
import android.app.Application
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ethiobalance.app.AppConstants
import com.ethiobalance.app.data.AppDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf

@RunWith(AndroidJUnit4::class)
class SettingsRepositoryPermissionTest {

    private lateinit var app: Application
    private lateinit var db: AppDatabase
    private lateinit var settingsRepo: SettingsRepository

    @Before
    fun setUp() {
        app = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(app, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        val dataStore = PreferenceDataStoreFactory.create {
            app.preferencesDataStoreFile("settings_repository_permission_${System.nanoTime()}")
        }

        settingsRepo = SettingsRepository(
            context = app,
            dataStore = dataStore,
            transactionSourceDao = db.transactionSourceDao(),
            transactionDao = db.transactionDao()
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun seedDefaultSourcesIfEmpty_doesNotPopulateDefaultsWithoutSmsReadPermission() = runBlocking {
        shadowOf(app).denyPermissions(Manifest.permission.READ_SMS)

        settingsRepo.seedDefaultSourcesIfEmpty()

        assertEquals(emptyList<String>(), db.transactionSourceDao().getAllSources().first().map { it.abbreviation })
    }

    @Test
    fun seedDefaultSourcesIfEmpty_populatesDefaultsWithSmsReadPermission() = runBlocking {
        shadowOf(app).grantPermissions(Manifest.permission.READ_SMS)

        settingsRepo.seedDefaultSourcesIfEmpty()

        val seeded = db.transactionSourceDao().getAllSources().first().map { it.abbreviation }.toSet()
        assertTrue(seeded.containsAll(AppConstants.DEFAULT_TRANSACTION_SOURCES))
    }
}
