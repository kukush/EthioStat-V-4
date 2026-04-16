package com.ethiobalance.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.activity.result.contract.ActivityResultContracts
import com.ethiobalance.app.repository.SmsRepository
import com.ethiobalance.app.ui.EthioBalanceAppUI
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var smsRepo: SmsRepository

    private var isScanStarted = false

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val smsGranted = permissions[Manifest.permission.READ_SMS] ?: false
        if (smsGranted) {
            triggerBackgroundScan()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val permissions = arrayOf(
            Manifest.permission.READ_SMS,
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.POST_NOTIFICATIONS
        )

        // Check if permissions are already granted
        if (permissions.all { ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED }) {
            triggerBackgroundScan()
        } else {
            // Request permissions using the modern launcher
            requestPermissionLauncher.launch(permissions)
        }

        setContent {
            EthioBalanceAppUI()
        }
    }

    private fun triggerBackgroundScan() {
        if (isScanStarted) return
        isScanStarted = true
        lifecycleScope.launch {
            Log.d("MainActivity", "Refreshing telecom from last 2 SMS messages...")
            val count = smsRepo.refreshTelecomFromLatestSms(limit = 2)
            Log.d("MainActivity", "Telecom refresh complete. Messages processed: $count")
        }
    }
}
