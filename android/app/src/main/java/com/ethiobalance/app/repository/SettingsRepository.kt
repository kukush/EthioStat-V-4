package com.ethiobalance.app.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.ethiobalance.app.data.AppDatabase
import com.ethiobalance.app.data.TransactionSourceEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

import com.ethiobalance.app.data.TransactionSourceDao
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dataStore: DataStore<Preferences>,
    private val transactionSourceDao: TransactionSourceDao
) {

    // DataStore keys
    companion object {
        val LANGUAGE_KEY = stringPreferencesKey("language")
        val THEME_KEY = stringPreferencesKey("theme")
        val USER_NAME_KEY = stringPreferencesKey("user_name")
        val USER_PHONE_KEY = stringPreferencesKey("user_phone")
        val USER_AVATAR_KEY = stringPreferencesKey("user_avatar")
    }

    // Language
    val language: Flow<String> = dataStore.data.map { it[LANGUAGE_KEY] ?: "en" }
    suspend fun setLanguage(lang: String) {
        dataStore.edit { it[LANGUAGE_KEY] = lang }
    }

    // Theme
    val theme: Flow<String> = dataStore.data.map { it[THEME_KEY] ?: "light" }
    suspend fun setTheme(theme: String) {
        dataStore.edit { it[THEME_KEY] = theme }
    }

    // User profile
    val userName: Flow<String> = dataStore.data.map { it[USER_NAME_KEY] ?: "User" }
    val userPhone: Flow<String> = dataStore.data.map { it[USER_PHONE_KEY] ?: "" }
    val userAvatar: Flow<String> = dataStore.data.map { it[USER_AVATAR_KEY] ?: "" }

    suspend fun setUserProfile(name: String, phone: String, avatar: String) {
        dataStore.edit {
            it[USER_NAME_KEY] = name
            it[USER_PHONE_KEY] = phone
            it[USER_AVATAR_KEY] = avatar
        }
    }

    // Transaction Sources
    fun getTransactionSources(): Flow<List<TransactionSourceEntity>> =
        transactionSourceDao.getAllSources()

    suspend fun addTransactionSource(source: TransactionSourceEntity) = withContext(Dispatchers.IO) {
        transactionSourceDao.insertOrUpdate(source)
        updateSmsWhitelist()
    }

    suspend fun removeTransactionSource(abbreviation: String) = withContext(Dispatchers.IO) {
        transactionSourceDao.deleteByAbbreviation(abbreviation)
        updateSmsWhitelist()
    }

    private suspend fun updateSmsWhitelist() {
        val enabledSenders = transactionSourceDao.getEnabledSenderIdsFlattened()
        val prefs = context.getSharedPreferences("ethio_balance_prefs", Context.MODE_PRIVATE)
        prefs.edit().putStringSet("sms_whitelist", enabledSenders.toSet()).apply()
    }
}
