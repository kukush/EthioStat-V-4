package com.ethiobalance.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.app.ActivityCompat
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

    private var isStartupRun = false

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val smsGranted = permissions[Manifest.permission.READ_SMS] ?: false
        // Seed AFTER the permission result is known so the SMS-based filter
        // in SettingsRepository actually sees the real permission state.
        runStartupSeedAndScan(smsGranted)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val permissions = arrayOf(
            Manifest.permission.READ_SMS,
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.POST_NOTIFICATIONS
        )

        val smsAlreadyGranted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.READ_SMS
        ) == PackageManager.PERMISSION_GRANTED

        // Check if permissions are already granted
        if (permissions.all { ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED }) {
            runStartupSeedAndScan(smsAlreadyGranted)
        } else {
            // Request permissions using the modern launcher.
            // Seeding is deferred to the permission callback so we know whether
            // we can inspect the SMS inbox.
            requestPermissionLauncher.launch(permissions)
        }

        setContent {
            EthioBalanceAppUI()
        }
    }

    /**
     * Seed default sources, then (if SMS is readable) run the 90-day historical
     * scan so the Home/History screens are populated on first launch.
     * Seeding must complete before the scan since [SmsRepository.scanAllTransactionSources]
     * reads the configured senders from the transaction_sources DAO.
     */
    private fun runStartupSeedAndScan(smsGranted: Boolean) {
        if (isStartupRun) return
        isStartupRun = true
        lifecycleScope.launch {
        
            settingsRepo.seedDefaultSourcesIfEmpty()

            if (!smsGranted) {
            
                return@launch
            }
        
            // Drop any default source that ended up with 0 parsed transactions
            // (e.g. device has CBE promo SMS but no actual transactions in 90d).
            settingsRepo.pruneEmptyDefaultSources()
        }
    }
}
