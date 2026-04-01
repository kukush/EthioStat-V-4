package com.ethiobalance.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ethiobalance.app.data.BalancePackageEntity
import com.ethiobalance.app.ui.Translations
import com.ethiobalance.app.ui.components.PackageCard
import com.ethiobalance.app.ui.theme.*
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelecomScreen(
    language: String,
    packages: List<BalancePackageEntity>,
    telecomBalance: Double,
    isSyncing: Boolean,
    syncError: String?,
    onSync: () -> Unit,
    onRecharge: (String) -> Unit,
    onTransfer: (String, String) -> Unit
) {
    val fmt = NumberFormat.getNumberInstance(Locale.US).apply {
        minimumFractionDigits = 2; maximumFractionDigits = 2
    }

    var showRechargeSheet by remember { mutableStateOf(false) }
    var showTransferSheet by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .statusBarsPadding()
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Title
        Text(
            text = Translations.t(language, "telecom"),
            fontSize = 30.sp,
            fontWeight = FontWeight.Black,
            color = Slate900
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Balance Card (dark)
        Surface(shape = RoundedCornerShape(40.dp), color = Slate900) {
            Box {
                // Decorative circles
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .offset(x = (-30).dp, y = (-30).dp)
                        .clip(CircleShape)
                        .background(Blue600.copy(alpha = 0.1f))
                )
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .align(Alignment.BottomEnd)
                        .offset(x = 20.dp, y = 20.dp)
                        .clip(CircleShape)
                        .background(Purple600.copy(alpha = 0.1f))
                )

                Column(modifier = Modifier.padding(24.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            tint = Amber500,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = Translations.t(language, "telecomAssets").uppercase(),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate400,
                            letterSpacing = 2.sp
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Icon(
                            imageVector = Icons.Default.Public,
                            contentDescription = null,
                            tint = Slate400,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = Translations.t(language, "ethio_telecom"),
                            fontSize = 10.sp,
                            color = Slate400
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Airtime balance
                        Column {
                            Text(
                                text = Translations.t(language, "availableAirtime").uppercase(),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate400,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = fmt.format(telecomBalance),
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                            Text(
                                text = "ETB",
                                fontSize = 12.sp,
                                color = Slate400
                            )
                        }

                        // Package indicators
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            val dataUsage = packages.filter { it.type.uppercase().contains("DATA") || it.type.uppercase().contains("INTERNET") }
                                .sumOf { it.remainingAmount }
                            val voiceUsage = packages.filter { it.type.uppercase() == "VOICE" }
                                .sumOf { it.remainingAmount }
                            val smsUsage = packages.filter { it.type.uppercase() == "SMS" }
                                .sumOf { it.remainingAmount }

                            PackageIndicator(Translations.t(language, "data"), dataUsage, Blue400)
                            PackageIndicator(Translations.t(language, "voice"), voiceUsage, Green500)
                            PackageIndicator(Translations.t(language, "sms"), smsUsage, Purple500)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Action buttons row
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ActionButton(Translations.t(language, "sync"), Icons.Default.Refresh, Blue600, isSyncing) { onSync() }
                        ActionButton(Translations.t(language, "recharge"), Icons.Default.Add, Emerald600) { showRechargeSheet = true }
                        ActionButton(Translations.t(language, "transfer"), Icons.Default.SwapHoriz, Amber500) { showTransferSheet = true }
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // Sync Error Display
        syncError?.let { error ->
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Rose50,
                border = androidx.compose.foundation.BorderStroke(1.dp, Rose600.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Error, null, tint = Rose600, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(error, fontSize = 13.sp, color = Rose600, modifier = Modifier.weight(1f))
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        // Active Packages
        Text(
            Translations.t(language, "activePackages").uppercase(),
            fontSize = 10.sp, fontWeight = FontWeight.Bold,
            color = Slate400, letterSpacing = 2.sp
        )
        Spacer(Modifier.height(12.dp))

        val activePackages = packages.filter { it.expiryDate > System.currentTimeMillis() }
        if (activePackages.isEmpty()) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Slate50,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.Inbox, null, tint = Slate300, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(8.dp))
                    Text(Translations.t(language, "noActivePackages"), fontSize = 14.sp, color = Slate400)
                    Text(Translations.t(language, "syncToSeePackages"), fontSize = 12.sp, color = Slate300)
                }
            }
        } else {
            activePackages.forEach { pkg ->
                PackageCard(pkg = pkg, language = language)
                Spacer(Modifier.height(12.dp))
            }
        }

        Spacer(Modifier.height(100.dp))
    }

    // Recharge Bottom Sheet
    if (showRechargeSheet) {
        RechargeSheet(
            language = language,
            onDismiss = { showRechargeSheet = false },
            onRecharge = { voucher ->
                onRecharge(voucher)
                showRechargeSheet = false
            }
        )
    }

    // Transfer Bottom Sheet
    if (showTransferSheet) {
        TransferSheet(
            language = language,
            onDismiss = { showTransferSheet = false },
            onTransfer = { recipient, amount ->
                onTransfer(recipient, amount)
                showTransferSheet = false
            }
        )
    }
}

@Composable
private fun PackageIndicator(label: String, value: Double, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .width(40.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(color.copy(alpha = 0.3f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth((value / 100.0).toFloat().coerceIn(0f, 1f))
                    .clip(RoundedCornerShape(2.dp))
                    .background(color)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "${String.format("%.0f", value)} $label",
            fontSize = 11.sp,
            color = Color.White.copy(alpha = 0.8f)
        )
    }
}

@Composable
private fun ActionButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    loading: Boolean = false,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = !loading,
        colors = ButtonDefaults.buttonColors(containerColor = color.copy(alpha = 0.2f)),
        shape = RoundedCornerShape(16.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
    ) {
        if (loading) {
            CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = color)
        } else {
            Icon(icon, null, tint = color, modifier = Modifier.size(14.dp))
        }
        Spacer(Modifier.width(4.dp))
        Text(label.uppercase(), fontSize = 9.sp, fontWeight = FontWeight.Bold, color = color, letterSpacing = 1.sp)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RechargeSheet(
    language: String,
    onDismiss: () -> Unit,
    onRecharge: (String) -> Unit
) {
    var voucher by remember { mutableStateOf("") }
    val isValid = voucher.length in 13..15 && voucher.all { it.isDigit() }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 48.dp, topEnd = 48.dp),
        containerColor = Color.White
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(
                Translations.t(language, "rechargeBalance"),
                fontSize = 22.sp, fontWeight = FontWeight.Black, color = Slate900
            )
            Text(
                Translations.t(language, "chooseRechargeMethod"),
                fontSize = 13.sp, color = Slate400
            )
            Spacer(Modifier.height(20.dp))

            OutlinedTextField(
                value = voucher,
                onValueChange = { if (it.length <= 15) voucher = it.filter { c -> c.isDigit() } },
                label = { Text(Translations.t(language, "voucherNumber")) },
                placeholder = { Text(Translations.t(language, "enterVoucher")) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                singleLine = true
            )
            Spacer(Modifier.height(8.dp))
            Text(Translations.t(language, "ussdRechargeInfo"), fontSize = 11.sp, color = Slate400)
            Spacer(Modifier.height(20.dp))

            Button(
                onClick = { onRecharge(voucher) },
                enabled = isValid,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Slate900)
            ) {
                Text(Translations.t(language, "rechargeViaUSSD"), fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TransferSheet(
    language: String,
    onDismiss: () -> Unit,
    onTransfer: (String, String) -> Unit
) {
    var recipient by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    val amountNum = amount.toDoubleOrNull() ?: 0.0
    val isValid = recipient.length >= 10 && amountNum in 5.0..1000.0

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 48.dp, topEnd = 48.dp),
        containerColor = Color.White
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(
                Translations.t(language, "balanceTransfer"),
                fontSize = 22.sp, fontWeight = FontWeight.Black, color = Slate900
            )
            Text(
                Translations.t(language, "transferAirtimeInfo"),
                fontSize = 13.sp, color = Slate400
            )
            Spacer(Modifier.height(20.dp))

            OutlinedTextField(
                value = recipient,
                onValueChange = { recipient = it.filter { c -> c.isDigit() } },
                label = { Text(Translations.t(language, "recipientNumber")) },
                placeholder = { Text(Translations.t(language, "enterPhone")) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                singleLine = true
            )
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it.filter { c -> c.isDigit() || c == '.' } },
                label = { Text(Translations.t(language, "amountEtb")) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                singleLine = true
            )
            Spacer(Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("${Translations.t(language, "minTransfer")}: 5 ETB", fontSize = 11.sp, color = Slate400)
                Text("${Translations.t(language, "maxTransfer")}: 1000 ETB", fontSize = 11.sp, color = Slate400)
            }
            Spacer(Modifier.height(20.dp))

            Button(
                onClick = { onTransfer(recipient, amount) },
                enabled = isValid,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Slate900)
            ) {
                Text(Translations.t(language, "transferViaUSSD"), fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}
