package com.ethiobalance.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.fragment.app.FragmentActivity
import com.ethiobalance.app.R
import com.ethiobalance.app.services.BiometricAuthService
import com.ethiobalance.app.ui.Translations

@Composable
fun LockedScreen(
    storedPin: String?,
    isPinEnabled: Boolean,
    isBiometricEnabled: Boolean,
    onSetPin: (String) -> Unit,
    onUnlock: () -> Unit,
    biometricAuthService: BiometricAuthService?,
    activity: FragmentActivity?,
    language: String
) {
    var pin by remember { mutableStateOf("") }
    var isPinVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var successMessage by remember { mutableStateOf("") }
    
    val hasPin = !storedPin.isNullOrEmpty()

    // Automatically trigger biometric authentication if enabled on launch
    LaunchedEffect(isBiometricEnabled, biometricAuthService, activity) {
        if (isBiometricEnabled && biometricAuthService != null && activity != null) {
            if (biometricAuthService.canAuthenticate()) {
                biometricAuthService.authenticate(
                    activity = activity,
                    title = Translations.t(language, "biometric_title").takeIf { it != "biometric_title" } ?: "Verify Identity",
                    subtitle = Translations.t(language, "biometric_subtitle").takeIf { it != "biometric_subtitle" } ?: "Scan fingerprint or face",
                    onSuccess = {
                        onUnlock()
                    },
                    onError = { _, _ ->
                        // Silently handle errors/cancellation for auto-trigger
                    }
                )
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF030712) // slate-950
    ) {
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

        // Show set PIN instructions for the first time when user sets a PIN
        if (!hasPin) {
            Text(
                text = Translations.t(language, "pinSetupTitle").takeIf { it != "pinSetupTitle" } ?: "Set 4-Digit PIN",
                fontSize = 20.sp,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = Translations.t(language, "pinRequirement").takeIf { it != "pinRequirement" } 
                    ?: "PIN must be exactly 4 digits (numbers only).",
                fontSize = 12.sp,
                color = Color(0xFF94A3B8), // slate-400
                modifier = Modifier.padding(horizontal = 24.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        OutlinedTextField(
            value = pin,
            onValueChange = { input ->
                val filtered = input.filter { it.isDigit() }
                if (filtered.length <= 4) {
                    pin = filtered
                    errorMessage = ""
                    successMessage = ""
                }
            },
            label = { Text("PIN", color = Color(0xFF94A3B8)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            visualTransformation = if (isPinVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { isPinVisible = !isPinVisible }) {
                    Icon(
                        imageVector = if (isPinVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = if (isPinVisible) "Hide PIN" else "Show PIN",
                        tint = Color(0xFF94A3B8)
                    )
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF10B981),
                unfocusedBorderColor = Color(0xFF334155),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            modifier = Modifier.fillMaxWidth(0.8f)
        )
        
        if (errorMessage.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = errorMessage,
                color = Color(0xFFFB7185), // rose-400
                fontSize = 14.sp
            )
        }

        if (successMessage.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = successMessage,
                color = Color(0xFF34D399), // emerald-400
                fontSize = 14.sp
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = {
                if (pin.length != 4) {
                    errorMessage = Translations.t(language, "pinRequirement").takeIf { it != "pinRequirement" } 
                        ?: "PIN must be exactly 4 digits (numbers only)."
                } else if (hasPin) {
                    if (pin == storedPin) {
                        onUnlock()
                    } else {
                        errorMessage = Translations.t(language, "invalidPinError").takeIf { it != "invalidPinError" } 
                            ?: "Invalid PIN. Please try again."
                    }
                } else {
                    onSetPin(pin)
                    successMessage = Translations.t(language, "pinSetupSuccess").takeIf { it != "pinSetupSuccess" } 
                        ?: "PIN set successfully!"
                    onUnlock()
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)), // emerald-600
            modifier = Modifier.fillMaxWidth(0.8f)
        ) {
            val buttonText = if (hasPin) {
                Translations.t(language, "unlock").takeIf { it != "unlock" } ?: "Unlock"
            } else {
                Translations.t(language, "setPin").takeIf { it != "setPin" } ?: "Set PIN"
            }
            Text(buttonText, color = Color.White)
        }
        
        // Use Biometric button
        // Only show if biometric is available/supported
        val isBioSupported = biometricAuthService != null && activity != null && biometricAuthService.canAuthenticate()
        if (isBioSupported) {
            Spacer(modifier = Modifier.height(16.dp))
            TextButton(
                onClick = {
                    biometricAuthService?.authenticate(
                        activity = activity!!,
                        title = Translations.t(language, "biometric_title").takeIf { it != "biometric_title" } ?: "Verify Identity",
                        subtitle = Translations.t(language, "biometric_subtitle").takeIf { it != "biometric_subtitle" } ?: "Scan fingerprint or face",
                        onSuccess = {
                            onUnlock()
                        },
                        onError = { _, errString ->
                            errorMessage = errString.toString()
                        }
                    )
                },
                colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF34D399))
            ) {
                Icon(
                    imageVector = Icons.Default.Fingerprint,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = Color(0xFF34D399)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(Translations.t(language, "useBiometric").takeIf { it != "useBiometric" } ?: "Use Biometric")
            }
        }
      }
    }
}
