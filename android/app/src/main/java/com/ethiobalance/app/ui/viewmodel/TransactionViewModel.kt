package com.ethiobalance.app.ui.viewmodel

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ethiobalance.app.AppConstants
import com.ethiobalance.app.data.TransactionEntity
import com.ethiobalance.app.domain.usecase.FormatTransactionUseCase
import com.ethiobalance.app.domain.usecase.GetFinancialSummaryUseCase
import com.ethiobalance.app.repository.SettingsRepository
import com.ethiobalance.app.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class TransactionViewModel @Inject constructor(
    private val transactionRepo: TransactionRepository,
    private val settingsRepo: SettingsRepository,
    private val formatTransactionUseCase: FormatTransactionUseCase,
    private val getFinancialSummaryUseCase: GetFinancialSummaryUseCase
) : ViewModel() {

    val allTransactions: StateFlow<List<TransactionEntity>> = transactionRepo.getAllTransactions()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val language: StateFlow<String> = settingsRepo.language
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "en")

    // Scanning state — shown as spinner on the Refresh button
    private val _isScanningHistory = MutableStateFlow(false)
    val isScanningHistory: StateFlow<Boolean> = _isScanningHistory.asStateFlow()

    // Filter state
    private val _timeFilter = MutableStateFlow("allTime")
    val timeFilter: StateFlow<String> = _timeFilter.asStateFlow()

    private val _sourceFilter = MutableStateFlow<String?>(null)
    val sourceFilter: StateFlow<String?> = _sourceFilter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val filteredTransactions: StateFlow<List<TransactionEntity>> = combine(
        allTransactions, _timeFilter, _sourceFilter, _searchQuery, settingsRepo.getTransactionSources()
    ) { transactions, time, source, query, configuredSources ->
        formatTransactionUseCase(transactions, time, source, query, configuredSources)
    }.distinctUntilChanged()
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Calculate totals using GetFinancialSummaryUseCase
    private val financialSummary: StateFlow<GetFinancialSummaryUseCase.FinancialSummary> = filteredTransactions
        .map { getFinancialSummaryUseCase(it) }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), GetFinancialSummaryUseCase.FinancialSummary(0.0, 0.0))

    val totalIncome: StateFlow<Double> = financialSummary.map { it.totalIncome }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalExpense: StateFlow<Double> = financialSummary.map { it.totalExpense }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val uniqueSources: StateFlow<List<Pair<String, String>>> = settingsRepo.getTransactionSources()
        .map { sources ->
            sources.map { it.abbreviation to AppConstants.resolveSource(it.senderId) }
                .filter { it.second != AppConstants.SOURCE_AIRTIME }
                .distinctBy { it.second }
                .sortedBy { it.second }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setTimeFilter(filter: String) { _timeFilter.value = filter }
    fun setSourceFilter(source: String?) { _sourceFilter.value = source }
    fun setSearchQuery(query: String) { _searchQuery.value = query }

    /**
     * Trigger a full 90-day historical SMS scan across all known senders
     * (AppConstants whitelist + user-configured sources).
     * Shows a loading state while running.
     */
    fun scanSmsHistory() {
        if (_isScanningHistory.value) return
        viewModelScope.launch {
            _isScanningHistory.value = true
            try {
                transactionRepo.smsRepo.scanAllTransactionSources(days = 90)
            } finally {
                _isScanningHistory.value = false
            }
        }
    }

    fun exportToCsv(context: Context) {
        val transactions = filteredTransactions.value
        if (transactions.isEmpty()) return

        val dateFormat = SimpleDateFormat("yyyy-MM-dd_HHmmss", Locale.US)
        val fileName = "ethiobalance_export_${dateFormat.format(Date())}.csv"
        val file = File(context.cacheDir, fileName)

        file.bufferedWriter().use { writer ->
            writer.write("ID,Type,Amount,Category,Source,Timestamp\n")
            transactions.forEach { t ->
                writer.write("${t.id},${t.type},${t.amount},${t.category},${t.source},${t.timestamp}\n")
            }
        }

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Export Transactions").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }
}
