package com.ethiobalance.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ethiobalance.app.data.SimCardEntity
import com.ethiobalance.app.data.TransactionSourceEntity
import com.ethiobalance.app.repository.BalanceRepository
import com.ethiobalance.app.repository.SettingsRepository
import com.ethiobalance.app.repository.SimRepository
import com.ethiobalance.app.repository.TransactionRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsRepo = SettingsRepository(application)
    private val simRepo = SimRepository(application)
    private val transactionRepo = TransactionRepository(application)
    private val balanceRepo = BalanceRepository(application)

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

    val simCards: StateFlow<List<SimCardEntity>> = simRepo.getAllSimCards()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val transactionSources: StateFlow<List<TransactionSourceEntity>> = settingsRepo.getTransactionSources()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setLanguage(lang: String) {
        viewModelScope.launch { settingsRepo.setLanguage(lang) }
    }

    fun setTheme(theme: String) {
        viewModelScope.launch { settingsRepo.setTheme(theme) }
    }

    fun setUserProfile(name: String, phone: String, avatar: String) {
        viewModelScope.launch { settingsRepo.setUserProfile(name, phone, avatar) }
    }

    fun detectSimCards() {
        viewModelScope.launch { simRepo.detectSimCards() }
    }

    fun addSimCard(sim: SimCardEntity) {
        viewModelScope.launch { simRepo.insertOrUpdate(sim) }
    }

    fun deleteSimCard(simId: String) {
        viewModelScope.launch { simRepo.delete(simId) }
    }

    fun setPrimarySim(simId: String) {
        viewModelScope.launch { simRepo.setPrimary(simId) }
    }

    fun addTransactionSource(source: TransactionSourceEntity) {
        viewModelScope.launch { settingsRepo.addTransactionSource(source) }
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
