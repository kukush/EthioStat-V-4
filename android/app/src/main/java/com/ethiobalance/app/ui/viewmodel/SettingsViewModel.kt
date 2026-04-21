package com.ethiobalance.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ethiobalance.app.AppConstants
import com.ethiobalance.app.data.TransactionSourceEntity
import com.ethiobalance.app.repository.BalanceRepository
import com.ethiobalance.app.repository.SettingsRepository
import com.ethiobalance.app.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepo: SettingsRepository,
    private val transactionRepo: TransactionRepository,
    private val balanceRepo: BalanceRepository
) : ViewModel() {

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

    fun setLanguage(lang: String) {
        viewModelScope.launch { settingsRepo.setLanguage(lang) }
    }

    fun setTheme(theme: String) {
        viewModelScope.launch { settingsRepo.setTheme(theme) }
    }

    fun setUserProfile(name: String, phone: String, avatar: String) {
        viewModelScope.launch {
            settingsRepo.setUserProfile(name, phone, avatar)
        }
    }

    fun addTransactionSource(source: TransactionSourceEntity) {
        viewModelScope.launch {
            settingsRepo.addTransactionSource(source)
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

    fun clearAllData() {
        viewModelScope.launch {
            transactionRepo.deleteAll()
            balanceRepo.deleteAll()
        }
    }
}
