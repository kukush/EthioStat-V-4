package com.ethiobalance.app.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ethiobalance.app.AppConstants
import com.ethiobalance.app.data.BalancePackageEntity
import com.ethiobalance.app.data.TransactionEntity
import com.ethiobalance.app.domain.usecase.GetFinancialSummaryUseCase
import com.ethiobalance.app.repository.BalanceRepository
import com.ethiobalance.app.repository.SettingsRepository
import com.ethiobalance.app.repository.SmsRepository
import com.ethiobalance.app.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Home screen.
 *
 * Responsibilities:
 * - Exposes filtered transactions, financial summaries, telecom packages,
 *   and bank balances for display.
 * - Provides [triggerManualSync] to scan SMS history for all user-configured
 *   transaction sources, resuming from the last scanned timestamp to avoid
 *   re-processing already-seen messages.
 *
 * The manual sync is the primary fallback when [SmsReceiver]'s real-time
 * `goAsync()` processing misses SMS due to power-saving or Doze mode.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val transactionRepo: TransactionRepository,
    private val balanceRepo: BalanceRepository,
    private val settingsRepo: SettingsRepository,
    private val smsRepo: SmsRepository,
    private val getFinancialSummaryUseCase: GetFinancialSummaryUseCase,
) : ViewModel() {

    // ─── Manual Sync State ──────────────────────────────────────────────────

    private val _isSyncing = MutableStateFlow(false)
    /** True while a manual SMS history scan is in progress. */
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _syncEvent = MutableSharedFlow<String>(extraBufferCapacity = 1)
    /** One-shot event emitted after a sync completes (message for Snackbar). */
    val syncEvent: SharedFlow<String> = _syncEvent.asSharedFlow()

    /**
     * Triggers a manual scan of SMS history for all user-configured transaction
     * sources.  Scans only messages newer than [SettingsRepository.lastScannedTimestamp]
     * to avoid re-processing.
     *
     * Safe to call multiple times — concurrent invocations are guarded by
     * [_isSyncing].
     */
    fun triggerManualSync() {
        if (_isSyncing.value) return
        viewModelScope.launch {
            _isSyncing.value = true
            try {
                val lastScanned = settingsRepo.lastScannedTimestamp.first()
                val now = System.currentTimeMillis()

                val daysSinceLastScan = if (lastScanned == 0L) {
                    90
                } else {
                    ((now - lastScanned) / AppConstants.MILLISECONDS_PER_DAY).toInt() + 1
                }

                val scanned = smsRepo.scanAllTransactionSources(days = daysSinceLastScan)
                settingsRepo.setLastScannedTimestamp(now)

                _syncEvent.tryEmit("Synced $scanned transactions")
                Log.d(TAG, "Manual sync completed: $scanned messages, window=$daysSinceLastScan days")
            } catch (e: Exception) {
                _syncEvent.tryEmit("Sync failed: ${e.message}")
                Log.e(TAG, "Manual sync failed", e)
            } finally {
                _isSyncing.value = false
            }
        }
    }

    private val allTransactions: StateFlow<List<TransactionEntity>> = transactionRepo.getAllTransactions()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val transactions: StateFlow<List<TransactionEntity>> = combine(
        allTransactions, settingsRepo.getTransactionSources()
    ) { txList, configuredSources ->
        val enabledResolved = configuredSources.flatMap { source ->
            source.senderId.split(",").map { id ->
                AppConstants.resolveSource(id.trim()).lowercase()
            }
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

    val bankBalances: StateFlow<Map<String, Double>> = combine(
        balanceRepo.getAllPackages(),
        settingsRepo.getTransactionSources()
    ) { packages, configuredSources ->
        val enabledResolved = configuredSources.flatMap { source ->
            source.senderId.split(",").map { id ->
                AppConstants.resolveSource(id.trim()).lowercase()
            }
        }.toSet()
        packages.filter { it.type.equals("bank_balance", ignoreCase = true) }
            .filter { enabledResolved.contains(AppConstants.resolveSource(it.simId).lowercase()) }
            .groupBy { AppConstants.resolveSource(it.simId).uppercase() }
            .mapValues { entry -> entry.value.sumOf { it.remainingAmount } }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    companion object {
        private const val TAG = "HomeViewModel"
    }
}
