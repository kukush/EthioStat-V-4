package com.ethiobalance.app.ui.viewmodel

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ProcessLifecycleOwner
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

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _syncError = MutableStateFlow<String?>(null)
    val syncError: StateFlow<String?> = _syncError.asStateFlow()

    private val _syncWarning = MutableStateFlow<String?>(null)
    val syncWarning: StateFlow<String?> = _syncWarning.asStateFlow()

    /**
     * Sync telecom packages:
     * 1. Open dialer with *804# pre-filled (user presses Call manually)
     * 2. Wait for user to dismiss USSD popup and return to the app
     * 3. Read latest 994 SMS to refresh package data
     *
     * Handles two edge cases after user clicks OK on USSD popup:
     * - USSD failed (connection problem / invalid MMI) → no SMS arrives → show warning
     * - USSD succeeded but SMS arrived while still in dialer → SmsReceiver already
     *   processed it, UI auto-updated via Room Flow; best-effort re-read on return
     */
    fun handleSync() {
        viewModelScope.launch {
            _isSyncing.value = true
            _syncError.value = null
            _syncWarning.value = null

            // Snapshot packages before sync to detect if SmsReceiver updated data
            // while the user was in the dialer
            val packagesBefore = packages.value

            try {
                // Open dialer with *804#, user presses Call
                smsRepo.dialUssd(AppConstants.USSD_BALANCE_CHECK)

                // Wait for user to return to the app after dismissing the USSD popup.
                // Flow: app → dialer (ON_STOP) → user dials → popup → OK/Cancel → Back → app (ON_START)
                val returned = CompletableDeferred<Unit>()
                var wentToBackground = false

                val observer = LifecycleEventObserver { _, event ->
                    when (event) {
                        Lifecycle.Event.ON_STOP -> {
                            wentToBackground = true
                            Log.d("TelecomVM", "App went to background (dialer opened)")
                        }
                        Lifecycle.Event.ON_START -> {
                            if (wentToBackground) {
                                Log.d("TelecomVM", "App returned to foreground")
                                returned.complete(Unit)
                            }
                        }
                        else -> {}
                    }
                }

                // Also listen for 994 SMS arrival broadcast — completes sync early
                // and brings app to foreground automatically
                val smsReceiver = object : BroadcastReceiver() {
                    override fun onReceive(ctx: Context, intent: Intent) {
                        Log.d("TelecomVM", "Telecom SMS arrived — completing sync early")
                        returned.complete(Unit)
                    }
                }
                ContextCompat.registerReceiver(
                    context,
                    smsReceiver,
                    IntentFilter(AppConstants.ACTION_TELECOM_SMS_ARRIVED),
                    ContextCompat.RECEIVER_NOT_EXPORTED
                )

                // Attach lifecycle observer — ON_STOP will fire when dialer takes focus
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    ProcessLifecycleOwner.get().lifecycle.addObserver(observer)
                }

                // Wait up to 120s — completed by EITHER:
                // 1. 994 SMS broadcast (auto-return, best case)
                // 2. User manually presses Back (ON_START fires)
                // 3. Timeout (USSD failed, no SMS, user didn't return)
                withTimeoutOrNull(120_000) { returned.await() } ?: run {
                    _syncWarning.value = "Sync timed out — showing latest available data"
                }

                // Clean up observer and receiver
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    ProcessLifecycleOwner.get().lifecycle.removeObserver(observer)
                }
                runCatching { context.unregisterReceiver(smsReceiver) }

                // Small delay to let any in-flight 994 SMS arrive
                kotlinx.coroutines.delay(3_000)

                // Best-effort re-read of latest 994 SMS
                val refreshed = smsRepo.refreshTelecomFromLatestSms(limit = 10)
                Log.d("TelecomVM", "Refreshed $refreshed telecom packages from 994 SMS")

                // Check if data changed: either from this re-read or from SmsReceiver
                // processing while user was in the dialer
                val packagesAfter = packages.value
                val dataChanged = refreshed > 0 || packagesAfter != packagesBefore

                if (!dataChanged) {
                    _syncWarning.value = "No new data received — USSD may have failed. " +
                        "If SMS arrives later, data will update automatically."
                    // Auto-dismiss warning after 5 seconds
                    kotlinx.coroutines.delay(5_000)
                    _syncWarning.value = null
                }
            } catch (e: Exception) {
                // On error, still try to refresh from latest SMS
                smsRepo.refreshTelecomFromLatestSms(limit = 10)
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
