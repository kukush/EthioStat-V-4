package com.ethiobalance.app.ui.viewmodel

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ethiobalance.app.AppConstants
import com.ethiobalance.app.data.TransactionEntity
import com.ethiobalance.app.data.TransactionSourceEntity
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

    private val _typeFilter = MutableStateFlow("ALL")
    val typeFilter: StateFlow<String> = _typeFilter.asStateFlow()

    private val _categoryFilter = MutableStateFlow("ALL")
    val categoryFilter: StateFlow<String> = _categoryFilter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _customStartMs = MutableStateFlow<Long?>(null)
    val customStartMs: StateFlow<Long?> = _customStartMs.asStateFlow()

    private val _customEndMs = MutableStateFlow<Long?>(null)
    val customEndMs: StateFlow<Long?> = _customEndMs.asStateFlow()

    private val _customRangeTrigger: StateFlow<Pair<Long?, Long?>> = combine(
        _customStartMs, _customEndMs
    ) { start, end -> start to end }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null to null)

    private data class FilterParams(
        val transactions: List<TransactionEntity>,
        val time: String,
        val source: String?,
        val type: String,
        val category: String,
        val query: String,
        val configuredSources: List<TransactionSourceEntity>
    )

    val filteredTransactions: StateFlow<List<TransactionEntity>> = combine(
        allTransactions,
        _timeFilter,
        _sourceFilter,
        _typeFilter,
        _categoryFilter,
        _searchQuery,
        settingsRepo.getTransactionSources()
    ) { arr ->
        @Suppress("UNCHECKED_CAST")
        FilterParams(
            transactions = arr[0] as List<TransactionEntity>,
            time = arr[1] as String,
            source = arr[2] as String?,
            type = arr[3] as String,
            category = arr[4] as String,
            query = arr[5] as String,
            configuredSources = arr[6] as List<TransactionSourceEntity>
        )
    }.combine(_customRangeTrigger) { params, range ->
        formatTransactionUseCase(
            transactions = params.transactions,
            timeFilter = params.time,
            sourceFilter = params.source,
            typeFilter = params.type,
            categoryFilter = params.category,
            searchQuery = params.query,
            configuredSources = params.configuredSources,
            customStartMs = range.first,
            customEndMs = range.second
        )
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
            sources.filter { it.isEnabled }.map { it.abbreviation to AppConstants.displaySource(it.abbreviation) }
                .filter { it.first != AppConstants.SOURCE_AIRTIME }
                .distinctBy { it.first }
                .sortedBy { it.second }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setTimeFilter(filter: String) { _timeFilter.value = filter }
    fun setCustomRange(start: Long?, end: Long?) {
        _customStartMs.value = start
        _customEndMs.value = end
        _timeFilter.value = "custom"
    }
    fun setSourceFilter(source: String?) { _sourceFilter.value = source }
    fun setTypeFilter(type: String) { _typeFilter.value = type }
    fun setCategoryFilter(category: String) { _categoryFilter.value = category }
    fun setSearchQuery(query: String) { _searchQuery.value = query }

    fun addManualTransaction(
        type: String,
        source: String,
        amount: Double,
        category: String,
        partyName: String,
        reference: String
    ) {
        viewModelScope.launch {
            val tx = TransactionEntity(
                id = "tx-manual-${System.currentTimeMillis()}",
                type = type.uppercase(),
                amount = amount,
                category = category.uppercase(),
                source = source.uppercase(),
                timestamp = System.currentTimeMillis(),
                reference = reference.trim().ifBlank { "REF-${(100000..999999).random()}" },
                partyName = partyName.trim().ifBlank { source },
                transactionSubType = "Manual"
            )
            transactionRepo.insert(tx)
        }
    }

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
        viewModelScope.launch {
            val transactions = filteredTransactions.value
            if (transactions.isEmpty()) return@launch

            val userName = settingsRepo.userName.first()
            val userPhone = settingsRepo.userPhone.first()
            val totalIncome = totalIncome.value
            val totalExpense = totalExpense.value
            val netBalance = totalIncome - totalExpense

            val dateFormat = SimpleDateFormat("yyyy-MM-dd_HHmmss", Locale.US)
            val fileName = "ethiostat_export_${dateFormat.format(Date())}.csv"
            val file = File(context.cacheDir, fileName)

            file.bufferedWriter().use { writer ->
                writer.write("EthioBalance Financial Report,,,,,,\n")
                writer.write("Account Holder,$userName,,,,,\n")
                writer.write("Phone Number,$userPhone,,,,,\n")
                writer.write("Summary,Income,$totalIncome,Expense,$totalExpense,Net,$netBalance\n")
                writer.write("\n")
                writer.write("Name,Date,Amount,Timestamp,Category,Type,Source Transaction\n")
                transactions.forEach { t ->
                    val date = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(t.timestamp))
                    writer.write("${t.partyName ?: "N/A"},$date,${t.amount},${t.timestamp},${t.category},${t.type},${t.source}\n")
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
}
