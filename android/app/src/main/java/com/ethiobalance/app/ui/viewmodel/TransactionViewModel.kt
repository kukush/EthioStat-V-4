package com.ethiobalance.app.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ethiobalance.app.AppConstants
import com.ethiobalance.app.data.TransactionEntity
import com.ethiobalance.app.repository.SettingsRepository
import com.ethiobalance.app.repository.TransactionRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class TransactionViewModel(application: Application) : AndroidViewModel(application) {

    val transactionRepo = TransactionRepository(application)
    private val settingsRepo = SettingsRepository(application)

    val allTransactions: StateFlow<List<TransactionEntity>> = transactionRepo.getAllTransactions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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
        // Start by removing AIRTIME and filtering by configured sources (Case-Insensitive)
        val enabledNormalised = configuredSources.map { it.name.lowercase() }.toSet()
        var filtered = transactions.filter {
            val resolved = AppConstants.resolveSource(it.source).lowercase()
            resolved != AppConstants.SOURCE_AIRTIME.lowercase() && enabledNormalised.contains(resolved)
        }

        // Time filter
        val now = System.currentTimeMillis()
        filtered = when (time) {
            "today" -> filtered.filter { now - it.timestamp < 24L * 60 * 60 * 1000 }
            "thisWeek" -> filtered.filter { now - it.timestamp < 7L * 24 * 60 * 60 * 1000 }
            "thisMonth" -> filtered.filter { now - it.timestamp < 30L * 24 * 60 * 60 * 1000 }
            else -> filtered
        }

        // Source filter
        if (source != null) {
            filtered = filtered.filter {
                AppConstants.resolveSource(it.source).equals(source, ignoreCase = true)
            }
        }

        // Search query
        if (query.isNotBlank()) {
            val q = query.lowercase()
            filtered = filtered.filter {
                it.source.lowercase().contains(q) ||
                it.category.lowercase().contains(q) ||
                it.id.lowercase().contains(q)
            }
        }

        filtered
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalIncome: StateFlow<Double> = filteredTransactions.map { list ->
        list.filter { it.type == "INCOME" }.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalExpense: StateFlow<Double> = filteredTransactions.map { list ->
        list.filter { it.type == "EXPENSE" }.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val uniqueSources: StateFlow<List<String>> = settingsRepo.getTransactionSources()
        .map { sources ->
            sources.map { it.name }
                .filter { it != AppConstants.SOURCE_AIRTIME }
                .distinct()
                .sorted()
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
