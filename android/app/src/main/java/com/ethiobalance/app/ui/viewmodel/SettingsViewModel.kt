package com.ethiobalance.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ethiobalance.app.AppConstants
import com.ethiobalance.app.data.TransactionSourceEntity
import com.ethiobalance.app.repository.SettingsRepository
import com.ethiobalance.app.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepo: SettingsRepository,
    private val transactionRepo: TransactionRepository
) : ViewModel() {

    val hasSeenOnboarding: StateFlow<Boolean?> = settingsRepo.hasSeenOnboarding
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val language: StateFlow<String> = settingsRepo.language
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "en")

    val theme: StateFlow<String> = settingsRepo.theme
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "light")

    val userName: StateFlow<String> = settingsRepo.userName
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "User")

    val userPhone: StateFlow<String> = settingsRepo.userPhone
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val userAvatar: StateFlow<String> = settingsRepo.userAvatar
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val transactionSources: StateFlow<List<TransactionSourceEntity>> = settingsRepo.getTransactionSources()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isBiometricEnabled: StateFlow<Boolean> = settingsRepo.isBiometricEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
        
    val isPinEnabled: StateFlow<Boolean> = settingsRepo.isPinEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val storedPin: StateFlow<String?> = settingsRepo.storedPin
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val isAutoSyncEnabled: StateFlow<Boolean> = settingsRepo.isAutoSyncEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun setLanguage(lang: String) {
        viewModelScope.launch { settingsRepo.setLanguage(lang) }
    }
    
    fun setBiometricEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepo.setBiometricEnabled(enabled) }
    }
    
    fun setPinEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepo.setPinEnabled(enabled) }
    }

    fun setPin(pin: String) {
        viewModelScope.launch { settingsRepo.setPin(pin) }
    }
    
    fun setAutoSyncEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepo.setAutoSyncEnabled(enabled) }
    }

    fun setTheme(theme: String) {
        viewModelScope.launch { settingsRepo.setTheme(theme) }
    }

    fun setUserProfile(name: String, phone: String, avatar: String) {
        viewModelScope.launch {
            settingsRepo.setUserProfile(name, phone, avatar)
        }
    }

    fun markOnboardingSeen() {
        viewModelScope.launch { settingsRepo.setOnboardingSeen() }
    }

    fun addTransactionSource(source: TransactionSourceEntity) {
        viewModelScope.launch {
            settingsRepo.addTransactionSource(source)
            // Only scan SMS if permission is granted — otherwise just insert the source
            if (!settingsRepo.hasSmsPermission()) return@launch
            // Scan ALL whitelist senders that resolve to the same bank abbreviation.
            // This is critical because many banks use alpha senders (e.g. "CBEBirr", "AwashBank")
            // in addition to numeric short codes (e.g. "847", "901").
            // A single scanHistory(senderId) would miss alpha-sender SMS entirely.
            val targetAbbrev = AppConstants.resolveSource(source.abbreviation).uppercase()
            val sendersToScan = (AppConstants.SMS_SENDER_WHITELIST + source.senderId.split(",").map { it.trim() })
                .filter { AppConstants.resolveSource(it).uppercase() == targetAbbrev }
                .distinct()
            for (sender in sendersToScan) {
                transactionRepo.smsRepo.scanHistory(sender, days = 90, forceReparse = true)
            }
        }
    }

    fun removeTransactionSource(abbreviation: String) {
        viewModelScope.launch { settingsRepo.removeTransactionSource(abbreviation) }
    }

    fun updateTransactionSource(source: TransactionSourceEntity) {
        viewModelScope.launch {
            settingsRepo.addTransactionSource(source)
        }
    }

    fun toggleTransactionSource(abbreviation: String) {
        viewModelScope.launch {
            settingsRepo.toggleTransactionSource(abbreviation)
        }
    }

    fun onPermissionGranted() {
        viewModelScope.launch {
            if (settingsRepo.hasSmsPermission()) {
                settingsRepo.seedDefaultSourcesIfEmpty()
                transactionRepo.smsRepo.refreshTelecomSmart()
                transactionRepo.smsRepo.scanAllTransactionSources(days = 90)
                settingsRepo.pruneEmptyDefaultSources()
            }
        }
    }
}
