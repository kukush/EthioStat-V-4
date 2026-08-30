package com.ethiobalance.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import com.ethiobalance.app.R

@Composable
fun LockedScreen(
    onUnlock: () -> Unit,
    onUseBiometric: () -> Unit
) {
    var pin by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(contentAlignment = Alignment.BottomEnd) {
            Image(
                painter = painterResource(id = R.drawable.app_icon),
                contentDescription = "Logo",
                modifier = Modifier.size(80.dp).clip(MaterialTheme.shapes.medium)
            )
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Locked",
                modifier = Modifier.size(32.dp).background(Color.White, CircleShape).padding(4.dp),
                tint = Color.Gray
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        OutlinedTextField(
            value = pin,
            onValueChange = { if (it.length <= 4) pin = it },
            label = { Text("PIN") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(0.8f)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(
            onClick = { /* Handle PIN verification */ },
            modifier = Modifier.fillMaxWidth(0.8f)
        ) {
            Text("Unlock")
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        TextButton(onClick = onUseBiometric) {
            Text("Use Biometric")
        }
    }
}
