package com.ethiobalance.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.activity.result.contract.ActivityResultContracts
import com.ethiobalance.app.repository.SettingsRepository
import com.ethiobalance.app.repository.SmsRepository
import com.ethiobalance.app.ui.EthioBalanceAppUI
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var smsRepo: SmsRepository

    @Inject
    lateinit var settingsRepo: SettingsRepository

    private val allPermissions = arrayOf(
        Manifest.permission.READ_SMS,
        Manifest.permission.RECEIVE_SMS,
        Manifest.permission.POST_NOTIFICATIONS
    )

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val smsGranted = result[Manifest.permission.READ_SMS] == true
        lifecycleScope.launch {
            seedAndScan(smsGranted)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        var isReady = false
        lifecycleScope.launch {
            var isFirstEmission = true
            var previousValue = false
            settingsRepo.hasSeenOnboarding.collect { onboardingDone ->
                isReady = true
                if (isFirstEmission || previousValue != onboardingDone) {
                    if (onboardingDone) {
                        // Onboarding completed
                        if (!isFirstEmission) {
                            // Only trigger permission prompt on state transition (false->true)
                            triggerPermissionAndScan()
                        }
                    } else {
                        // Still in onboarding (or fresh install) — seed only, no permission prompt
                        settingsRepo.seedDefaultSourcesIfEmpty()
                    }
                }
                isFirstEmission = false
                previousValue = onboardingDone
            }
        }
        
        splashScreen.setKeepOnScreenCondition { !isReady }

        setContent { EthioBalanceAppUI() }
    }

    /**
     * Called when onboarding completes (Get Started tapped) or on every
     * subsequent launch. Requests permissions if not yet granted, then seeds
     * + scans SMS history.
     */
    fun triggerPermissionAndScan() {
        if (allPermissions.all { ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED }) {
            val smsGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED
            lifecycleScope.launch { seedAndScan(smsGranted) }
        } else {
            requestPermissionLauncher.launch(allPermissions)
        }
    }

    private suspend fun seedAndScan(smsGranted: Boolean) {
        if (!smsGranted) return
        settingsRepo.seedDefaultSourcesIfEmpty()
        smsRepo.refreshTelecomSmart()
        smsRepo.scanAllTransactionSources(days = 90)
        settingsRepo.pruneEmptyDefaultSources()
    }
}
