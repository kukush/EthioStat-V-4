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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val permissions = arrayOf(
            Manifest.permission.READ_SMS,
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.POST_NOTIFICATIONS
        )

        // Check if permissions are already granted to start scan immediately
        if (permissions.all { ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED }) {
            triggerBackgroundScan()
        } else {
            // Request SMS and phone permissions on startup
            ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE)
        }

        setContent {
            EthioBalanceAppUI()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            val smsGranted = grantResults.getOrNull(permissions.indexOf(Manifest.permission.READ_SMS)) == PackageManager.PERMISSION_GRANTED
            if (smsGranted) {
                triggerBackgroundScan()
            }
        }
    }

    private fun triggerBackgroundScan() {
        if (isScanStarted) return
        isScanStarted = true
        lifecycleScope.launch {
            Log.d("MainActivity", "Starting initial historical scan (90 days)...")
            val count = smsRepo.scanAllTransactionSources(90)
            Log.d("MainActivity", "Initial scan complete. Total messages processed: $count")
        }
    }

    companion object {
        private const val PERMISSION_REQUEST_CODE = 1
    }
}
