package com.ethiobalance.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ethiobalance.app.AppConstants
import com.ethiobalance.app.data.BalancePackageEntity
import com.ethiobalance.app.domain.usecase.SyncAirtimeUseCase
import com.ethiobalance.app.repository.BalanceRepository
import com.ethiobalance.app.repository.SettingsRepository
import com.ethiobalance.app.repository.SmsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.util.Log

@HiltViewModel
class TelecomViewModel @Inject constructor(
    private val balanceRepo: BalanceRepository,
    private val settingsRepo: SettingsRepository,
    private val smsRepo: SmsRepository,
    private val syncAirtimeUseCase: SyncAirtimeUseCase
) : ViewModel() {

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

    private val _syncWarning = MutableStateFlow<String?>(null)
    val syncWarning: StateFlow<String?> = _syncWarning.asStateFlow()

    /**
     * Sync balance: opens dialer with *804# pre-filled and waits for SMS response.
     * User presses call button, SMS is automatically captured and UI updates.
     */
    fun handleSync() {
        viewModelScope.launch {
            _isSyncing.value = true
            _syncError.value = null
            _syncWarning.value = null
            try {
                // Use dialer method - opens with *804# pre-filled
                smsRepo.dialUssd(AppConstants.USSD_BALANCE_CHECK)
                
                // Wait for user to dial the number (up to 30 seconds)
                val maxDialWait = 30000L // 30 seconds max wait for user to dial
                val dialCheckInterval = 2000L // Check every 2 seconds
                var totalDialWaited = 0L
                var userDialed = false
                
                _syncWarning.value = "Please dial the USSD number..."
                
                // First wait for user to dial (no closing attempts yet)
                while (totalDialWaited < maxDialWait && !userDialed) {
                    kotlinx.coroutines.delay(dialCheckInterval)
                    totalDialWaited += dialCheckInterval
                    
                    // Check if any SMS has arrived (user dialed and got response)
                    val scanned = smsRepo.scanTelecomHistory(days = 1)
                    if (scanned > 0) {
                        _syncWarning.value = null
                        userDialed = true
                        Log.d("TelecomViewModel", "User dialed and received SMS response")
                        break
                    }
                    
                    _syncWarning.value = "Please dial the USSD number... (${(maxDialWait - totalDialWaited)/2000}s)"
                }
                
                if (!userDialed) {
                    _syncWarning.value = "No USSD call detected. Please dial *804# to check balance."
                    // Don't proceed further if user didn't dial
                    _isSyncing.value = false
                    return@launch
                }
                
                // User has dialed and pressed OK, wait for USSD response in background
                _syncWarning.value = "Processing USSD response in background..."
                kotlinx.coroutines.delay(3000) // Wait for USSD to complete
                
                // Try to close USSD popup if it appears
                val maxPopupWait = 10000L // 10 seconds max wait for popup
                val popupCheckInterval = 1000L // Check every 1 second
                var totalPopupWaited = 0L
                var popupClosed = false
                
                _syncWarning.value = "Waiting for USSD response..."
                
                while (totalPopupWaited < maxPopupWait && !popupClosed) {
                    kotlinx.coroutines.delay(popupCheckInterval)
                    totalPopupWaited += popupCheckInterval
                    
                    // Try to close the USSD popup
                    val ussdService = com.ethiobalance.app.services.UssdAccessibilityService.getInstance()
                    if (ussdService != null) {
                        ussdService.closeDialer()
                        Log.d("TelecomViewModel", "Attempted to close USSD popup after ${totalPopupWaited/1000}s")
                        
                        // Wait a moment to see if popup was closed
                        kotlinx.coroutines.delay(500)
                        
                        // Check if SMS was received (popup was successfully closed and processed)
                        val popupScanned = smsRepo.scanTelecomHistory(days = 1)
                        if (popupScanned > 0) {
                            _syncWarning.value = null
                            popupClosed = true
                            Log.d("TelecomViewModel", "USSD popup closed and processed successfully")
                            break
                        }
                        
                        _syncWarning.value = "Waiting for USSD response... (${(maxPopupWait - totalPopupWaited)/1000}s)"
                    }
                }
                
                // After popup handling, continue waiting for SMS
                if (!popupClosed) {
                    _syncWarning.value = "Waiting for balance SMS..."
                    val maxSmsWait = 15000L // 15 seconds max wait for SMS
                    val smsCheckInterval = 2000L // Check every 2 seconds
                    var totalSmsWaited = 0L
                    var smsReceived = false
                    
                    while (totalSmsWaited < maxSmsWait && !smsReceived) {
                        kotlinx.coroutines.delay(smsCheckInterval)
                        totalSmsWaited += smsCheckInterval
                        
                        // Check if SMS has arrived
                        val scanned = smsRepo.scanTelecomHistory(days = 1)
                        if (scanned > 0) {
                            _syncWarning.value = null
                            smsReceived = true
                            Log.d("TelecomViewModel", "Balance SMS received and processed")
                            break
                        }
                        
                        _syncWarning.value = "Waiting for balance SMS... (${(maxSmsWait - totalSmsWaited)/2000}s)"
                    }
                    
                    if (!smsReceived) {
                        _syncWarning.value = "No USSD response detected. Please return to app to check balance."
                    }
                }
                
                // Wait for SMS response
                kotlinx.coroutines.delay(7000)
                val scanned = smsRepo.scanTelecomHistory(days = 1)
                
                if (scanned == 0) {
                    // Fallback: scan all recent SMS for balance keywords
                    val balanceScanned = smsRepo.scanAllTransactionSources(days = 1)
                    if (balanceScanned > 0) {
                        _syncWarning.value = null // Clear warning if balance SMS found
                    } else {
                        // Only show warning if no SMS received after delay
                        _syncWarning.value = "No balance SMS received. Please ensure the USSD call was completed."
                    }
                } else {
                    _syncWarning.value = null // Clear any warning when SMS is received
                }
            } catch (e: Exception) {
                _syncError.value = e.message ?: "Sync failed"
            } finally {
                _isSyncing.value = false
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
