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
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = Translations.t(language, "pinRequirement").takeIf { it != "pinRequirement" } 
                    ?: "PIN must be exactly 4 digits (numbers only).",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
            label = { Text("PIN") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            visualTransformation = if (isPinVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { isPinVisible = !isPinVisible }) {
                    Icon(
                        imageVector = if (isPinVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = if (isPinVisible) "Hide PIN" else "Show PIN"
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(0.8f)
        )
        
        if (errorMessage.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                fontSize = 14.sp
            )
        }

        if (successMessage.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = successMessage,
                color = MaterialTheme.colorScheme.primary,
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
            modifier = Modifier.fillMaxWidth(0.8f)
        ) {
            val buttonText = if (hasPin) {
                Translations.t(language, "unlock").takeIf { it != "unlock" } ?: "Unlock"
            } else {
                Translations.t(language, "setPin").takeIf { it != "setPin" } ?: "Set PIN"
            }
            Text(buttonText)
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
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Fingerprint,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(Translations.t(language, "useBiometric").takeIf { it != "useBiometric" } ?: "Use Biometric")
            }
        }
    }
}
