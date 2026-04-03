package com.ethiobalance.app

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.app.ActivityCompat
import com.ethiobalance.app.ui.EthioBalanceApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Request SMS and phone permissions on startup for EthioStat Monitoring
        ActivityCompat.requestPermissions(this, arrayOf(
            Manifest.permission.READ_SMS,
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.POST_NOTIFICATIONS
        ), 1)

        setContent {
            EthioBalanceApp()
        }
    }
}
