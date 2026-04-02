package com.ethiobalance.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ethiobalance.app.AppConstants
import com.ethiobalance.app.data.BalancePackageEntity
import com.ethiobalance.app.repository.BalanceRepository
import com.ethiobalance.app.repository.SettingsRepository
import com.ethiobalance.app.repository.SmsRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class TelecomViewModel(application: Application) : AndroidViewModel(application) {

    private val balanceRepo = BalanceRepository(application)
    private val smsRepo = SmsRepository(application)
    private val settingsRepo = SettingsRepository(application)

    val packages: StateFlow<List<BalancePackageEntity>> = balanceRepo.getAllPackages()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val language: StateFlow<String> = settingsRepo.language
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "en")

    val telecomBalance: StateFlow<Double> = packages.map { list ->
        list.filter { it.type == "DATA_AIRTIME" || it.type == "AIRTIME" }
            .sumOf { it.remainingAmount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _syncError = MutableStateFlow<String?>(null)
    val syncError: StateFlow<String?> = _syncError.asStateFlow()

    /**
     * Sync balance: sends USSD *804# directly and then scans for the response SMS.
     * The response from 804 is caught by SmsReceiver → ReconciliationEngine → DB → Flow auto-updates UI.
     */
    fun handleSync() {
        viewModelScope.launch {
            _isSyncing.value = true
            _syncError.value = null
            try {
                // Send USSD using Intent (no runtime permission required)
                val result = smsRepo.sendUssdIntent(AppConstants.USSD_BALANCE_CHECK)
                if (result.isFailure) {
                    val error = result.exceptionOrNull()?.message
                    _syncError.value = error ?: "Failed to initiate USSD call."
                    return@launch
                }
                // After a short delay, scan for any new SMS from telecom senders
                kotlinx.coroutines.delay(9000)
                val scanned = smsRepo.scanTelecomHistory(days = 1)
                if (scanned == 0) {
                    _syncError.value = "Balance capture pending. Please wait for SMS from EthioTelecom."
                }
            } catch (e: Exception) {
                _syncError.value = e.message ?: "Sync failed"
            } finally {
                _isSyncing.value = false
            }
        }
    }

    fun rechargeViaUssd(voucher: String) {
        val code = "${AppConstants.USSD_RECHARGE_SELF}$voucher#"
        smsRepo.dialUssd(code)
    }

    fun transferAirtime(recipient: String, amount: String) {
        val code = "${AppConstants.USSD_TRANSFER_AIRTIME}$recipient*$amount#"
        smsRepo.dialUssd(code)
    }
}
