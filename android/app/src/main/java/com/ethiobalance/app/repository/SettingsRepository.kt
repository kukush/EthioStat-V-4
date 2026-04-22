package com.ethiobalance.app.repository

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Telephony
import androidx.core.content.ContextCompat
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

import com.ethiobalance.app.data.TransactionDao
import com.ethiobalance.app.data.TransactionSourceDao
import com.ethiobalance.app.AppConstants
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dataStore: DataStore<Preferences>,
    private val transactionSourceDao: TransactionSourceDao,
    private val transactionDao: TransactionDao
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

    /**
     * Get all sender ID variants for a bank abbreviation.
     * Filters SMS_SENDER_WHITELIST to find all entries that resolve to this abbreviation.
     * Returns comma-separated string (e.g., "889,847,CBE,CBEBirr,CBEBIRR" for CBE).
     */
    fun getAllSenderIdsForBank(abbreviation: String): String {
        val upper = abbreviation.uppercase()
        val variants = mutableSetOf<String>()

        // Add all entries from SMS_SENDER_WHITELIST that resolve to this abbreviation
        AppConstants.SMS_SENDER_WHITELIST.forEach { senderId ->
            if (AppConstants.resolveSource(senderId) == upper) {
                variants.add(senderId)
            }
        }

        // Add the abbreviation itself
        variants.add(upper)

        // Also check TELECOM_SENDERS for Telebirr
        if (upper == "TELEBIRR") {
            AppConstants.TELECOM_SENDERS.forEach { senderId ->
                if (AppConstants.resolveSource(senderId) == upper) {
                    variants.add(senderId)
                }
            }
        }

        return variants.joinToString(",")
    }

    /**
     * Check if SMS read permission is granted.
     */
    private fun hasSmsPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.READ_SMS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true // Permission granted at install time on older Android
        }
    }

    /**
     * Check if there are any SMS messages from any of the given sender IDs in the last N days.
     * Returns true if at least one message is found.
     * Returns false if SMS permission is not granted.
     */
    private fun hasSmsFromSenders(senderIds: List<String>, days: Int = 90): Boolean {
        // Can't query SMS without permission
        if (!hasSmsPermission()) {
            return false
        }

        val contentUri = Telephony.Sms.CONTENT_URI
        val windowStart = System.currentTimeMillis() - (days * AppConstants.MILLISECONDS_PER_DAY)

        if (senderIds.isEmpty()) return false

        // Build selection: (address=? OR address=? ...) AND date>=?
        // Parentheses are required — without them, SQL AND binds tighter than OR
        // and the date filter would only apply to the LAST sender.
        val selectionArgs = mutableListOf<String>()
        val orClause = senderIds.joinToString(" OR ") {
            selectionArgs.add(it)
            "address = ?"
        }
        val selection = "($orClause) AND date >= ?"
        selectionArgs.add(windowStart.toString())

        return try {
            context.contentResolver.query(
                contentUri,
                arrayOf("_id", "address", "date"),
                selection,
                selectionArgs.toTypedArray(),
                "date DESC LIMIT 1"
            )?.use { cursor ->
                val found = cursor.moveToFirst()
                if (found) {
                    val addr = cursor.getString(cursor.getColumnIndexOrThrow("address"))
                    val ts = cursor.getLong(cursor.getColumnIndexOrThrow("date"))
                    android.util.Log.d(
                        "SettingsRepository",
                        "hasSmsFromSenders: match senders=$senderIds addr=$addr ts=$ts"
                    )
                } else {
                    android.util.Log.d(
                        "SettingsRepository",
                        "hasSmsFromSenders: NO match senders=$senderIds windowDays=$days"
                    )
                }
                found
            } ?: false
        } catch (e: SecurityException) {
            android.util.Log.w("SettingsRepository", "hasSmsFromSenders: SecurityException", e)
            false
        }
    }

    /**
     * Seed default transaction sources on first install.
     * Only runs if transaction_sources table is empty.
     *
     * Behavior:
     * - If SMS permission is granted: only add sources that have SMS transactions in last 90 days
     * - If SMS permission NOT granted: add all default sources (CBE, Telebirr) so user sees them
     *   and can use them. SMS scanning will happen after permission is granted via MainActivity.
     */
    suspend fun seedDefaultSourcesIfEmpty() = withContext(Dispatchers.IO) {
        val currentSources = transactionSourceDao.getAllSources().first()
        if (currentSources.isNotEmpty()) {
            android.util.Log.d(
                "SettingsRepository",
                "seedDefaultSourcesIfEmpty: already seeded (${currentSources.size} rows), skip"
            )
            return@withContext
        }

        val hasPermission = hasSmsPermission()
        android.util.Log.d(
            "SettingsRepository",
            "seedDefaultSourcesIfEmpty: starting, hasSmsPermission=$hasPermission"
        )

        val sourcesToAdd = if (hasPermission) {
            // With permission: only add sources that have actual SMS transactions
            AppConstants.DEFAULT_TRANSACTION_SOURCES.mapNotNull { abbrev ->
                val bankInfo = AppConstants.KNOWN_BANKS.find { it.abbreviation == abbrev }
                bankInfo?.let {
                    val allSenderIds = getAllSenderIdsForBank(it.abbreviation)
                        .split(",")
                        .map { id -> id.trim() }
                        .filter { id -> id.isNotEmpty() }

                    // Only add source if there are actual SMS messages
                    if (hasSmsFromSenders(allSenderIds, days = 90)) {
                        TransactionSourceEntity(
                            abbreviation = it.abbreviation,
                            name = it.fullName,
                            ussd = "",
                            senderId = allSenderIds.joinToString(","),
                            isEnabled = true
                        )
                    } else {
                        null // Skip - no transactions found
                    }
                }
            }
        } else {
            // Without permission: add all default sources so user can see them in Settings
            // SMS scanning will happen after user grants permission in MainActivity
            AppConstants.DEFAULT_TRANSACTION_SOURCES.mapNotNull { abbrev ->
                val bankInfo = AppConstants.KNOWN_BANKS.find { it.abbreviation == abbrev }
                bankInfo?.let {
                    TransactionSourceEntity(
                        abbreviation = it.abbreviation,
                        name = it.fullName,
                        ussd = "",
                        senderId = getAllSenderIdsForBank(it.abbreviation),
                        isEnabled = true
                    )
                }
            }
        }

        android.util.Log.d(
            "SettingsRepository",
            "seedDefaultSourcesIfEmpty: inserting ${sourcesToAdd.size} sources: " +
                sourcesToAdd.joinToString { it.abbreviation }
        )
        if (sourcesToAdd.isNotEmpty()) {
            transactionSourceDao.insertAll(sourcesToAdd)
            updateSmsWhitelist()
        }
    }

    /**
     * After the 90-day historical scan, remove any DEFAULT source that ended up
     * with zero parsed transactions. A sender may have SMS in the inbox but none
     * of them are actual transaction messages (e.g. promos, PINs, greetings) —
     * in that case the UI should not advertise it as a configured source.
     *
     * Only prunes abbreviations from [AppConstants.DEFAULT_TRANSACTION_SOURCES]
     * so that user-added sources are never touched.
     */
    suspend fun pruneEmptyDefaultSources() = withContext(Dispatchers.IO) {
        val currentSources = transactionSourceDao.getAllSources().first()
        val defaults = AppConstants.DEFAULT_TRANSACTION_SOURCES.toSet()
        val toRemove = currentSources
            .filter { it.abbreviation in defaults }
            .filter { transactionDao.countBySource(it.abbreviation) == 0 }

        if (toRemove.isEmpty()) {
            android.util.Log.d(
                "SettingsRepository",
                "pruneEmptyDefaultSources: nothing to prune"
            )
            return@withContext
        }

        toRemove.forEach { src ->
            android.util.Log.d(
                "SettingsRepository",
                "pruneEmptyDefaultSources: removing ${src.abbreviation} (0 transactions after scan)"
            )
            transactionSourceDao.deleteByAbbreviation(src.abbreviation)
        }
        updateSmsWhitelist()
    }
}
