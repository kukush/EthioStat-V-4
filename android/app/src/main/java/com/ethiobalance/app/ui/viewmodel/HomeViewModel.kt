package com.ethiobalance.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ethiobalance.app.AppConstants
import com.ethiobalance.app.data.BalancePackageEntity
import com.ethiobalance.app.data.TransactionEntity
import com.ethiobalance.app.domain.usecase.GetFinancialSummaryUseCase
import com.ethiobalance.app.repository.BalanceRepository
import com.ethiobalance.app.repository.SettingsRepository
import com.ethiobalance.app.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val transactionRepo: TransactionRepository,
    private val balanceRepo: BalanceRepository,
    private val settingsRepo: SettingsRepository,
    private val getFinancialSummaryUseCase: GetFinancialSummaryUseCase
) : ViewModel() {

    private val allTransactions: StateFlow<List<TransactionEntity>> = transactionRepo.getAllTransactions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val transactions: StateFlow<List<TransactionEntity>> = combine(
        allTransactions, settingsRepo.getTransactionSources()
    ) { txList, configuredSources ->
        val enabledResolved = configuredSources.map {
            AppConstants.resolveSource(it.senderId).lowercase()
        }.toSet()
        txList.filter {
            val resolved = AppConstants.resolveSource(it.source).lowercase()
            resolved != AppConstants.SOURCE_AIRTIME.lowercase() && enabledResolved.contains(resolved)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val telecomTypes = setOf("airtime", "voice", "internet", "data", "sms", "bonus")

    val packages: StateFlow<List<BalancePackageEntity>> = balanceRepo.getAllPackages()
        .map { list ->
            list.filter { it.type.lowercase() in telecomTypes }
                // Normalize "data" → "internet" so they merge
                .map { if (it.type.equals("data", ignoreCase = true)) it.copy(type = "internet") else it }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userName: StateFlow<String> = settingsRepo.userName
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "User")

    val userPhone: StateFlow<String> = settingsRepo.userPhone
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val language: StateFlow<String> = settingsRepo.language
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "en")

    val theme: StateFlow<String> = settingsRepo.theme
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "light")

    private val financialSummary: StateFlow<GetFinancialSummaryUseCase.FinancialSummary> = transactions.map { list ->
        getFinancialSummaryUseCase(list)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), GetFinancialSummaryUseCase.FinancialSummary(0.0, 0.0))

    val totalIncome: StateFlow<Double> = financialSummary.map { it.totalIncome }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalExpense: StateFlow<Double> = financialSummary.map { it.totalExpense }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val telecomBalance: StateFlow<Double> = packages.map { list ->
        list.filter { it.type.equals("airtime", ignoreCase = true) }
            .sumOf { it.remainingAmount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Bank/wallet balances keyed by resolved source name (e.g. "TeleBirr" → 7.96)
    // Only shows balances for sources configured in Settings
    val bankBalances: StateFlow<Map<String, Double>> = combine(
        balanceRepo.getAllPackages(),
        settingsRepo.getTransactionSources()
    ) { packages, configuredSources ->
        val enabledResolved = configuredSources.map {
            AppConstants.resolveSource(it.senderId).lowercase()
        }.toSet()
        packages.filter { it.type.equals("bank_balance", ignoreCase = true) }
            .filter { enabledResolved.contains(it.simId.lowercase()) }
            .associate { it.simId to it.remainingAmount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())
}
