package com.ethiobalance.app.ui.viewmodel

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import androidx.lifecycle.viewModelScope
import com.ethiobalance.app.AppConstants
import com.ethiobalance.app.data.BalancePackageEntity
import com.ethiobalance.app.domain.usecase.SyncAirtimeUseCase
import com.ethiobalance.app.repository.BalanceRepository
import com.ethiobalance.app.repository.SettingsRepository
import com.ethiobalance.app.repository.SmsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import android.util.Log

@HiltViewModel
class TelecomViewModel @Inject constructor(
    private val balanceRepo: BalanceRepository,
    private val settingsRepo: SettingsRepository,
    private val smsRepo: SmsRepository,
    private val syncAirtimeUseCase: SyncAirtimeUseCase,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val telecomTypes = setOf("airtime", "voice", "internet", "data", "sms", "bonus")

    val packages: StateFlow<List<BalancePackageEntity>> = balanceRepo.getAllPackages()
        .map { list ->
            list.filter { it.type.lowercase() in telecomTypes }
                // Normalize "data" → "internet" so they merge
                .map { if (it.type.equals("data", ignoreCase = true)) it.copy(type = "internet") else it }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val language: StateFlow<String> = settingsRepo.language
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "en")

    val telecomBalance: StateFlow<Double> = packages.map { list ->
        list.filter { it.type.equals("airtime", ignoreCase = true) }
            .sumOf { it.remainingAmount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _syncError = MutableStateFlow<String?>(null)
    val syncError: StateFlow<String?> = _syncError.asStateFlow()

    private val _syncWarning = MutableStateFlow<String?>(null)
    val syncWarning: StateFlow<String?> = _syncWarning.asStateFlow()

    /**
     * Sync balance: opens dialer with *804# pre-filled, waits for USSD response via broadcast.
     * User presses call button, AccessibilityService captures popup, app auto-returns.
     */
    fun handleSync() {
        viewModelScope.launch {
            _isSyncing.value = true
            _syncError.value = null
            _syncWarning.value = null

            val done = CompletableDeferred<Unit>()

            val receiver = object : BroadcastReceiver() {
                override fun onReceive(ctx: Context, intent: Intent) {
                    // USSD response captured — refresh telecom from latest 2 SMS
                    viewModelScope.launch {
                        smsRepo.refreshTelecomFromLatestSms(limit = 2)
                        _isSyncing.value = false
                        done.complete(Unit)
                    }
                }
            }

            ContextCompat.registerReceiver(
                context,
                receiver,
                IntentFilter(AppConstants.ACTION_USSD_RESPONSE),
                ContextCompat.RECEIVER_NOT_EXPORTED
            )

            try {
                // Open dialer with *804#, user presses Call
                smsRepo.dialUssd(AppConstants.USSD_BALANCE_CHECK)

                // Wait up to 120s for user to dial, read popup, and close it
                withTimeoutOrNull(120_000) { done.await() } ?: run {
                    // Timeout — still refresh from latest 2 SMS so data is up-to-date
                    smsRepo.refreshTelecomFromLatestSms(limit = 2)
                    _syncWarning.value = "Sync timed out — showing latest available data"
                    _isSyncing.value = false
                }
            } catch (e: Exception) {
                // On error, still try to refresh from latest SMS
                smsRepo.refreshTelecomFromLatestSms(limit = 2)
                _syncError.value = e.message ?: "Sync failed"
                _isSyncing.value = false
            } finally {
                runCatching { context.unregisterReceiver(receiver) }
            }
        }
    }
    
    fun rechargeViaUssd(voucher: String) {
        syncAirtimeUseCase.recharge(voucher)
    }

    fun transferAirtime(recipient: String, amount: String) {
        syncAirtimeUseCase.transfer(recipient, amount)
    }
}
