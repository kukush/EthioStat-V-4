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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ethiobalance.app.data.TransactionEntity
import com.ethiobalance.app.ui.Translations
import com.ethiobalance.app.ui.components.SummaryCard
import com.ethiobalance.app.ui.components.TransactionItem
import com.ethiobalance.app.ui.theme.*
import java.text.NumberFormat
import java.util.Locale

@Composable
fun HomeScreen(
    userName: String,
    userPhone: String,
    language: String,
    totalIncome: Double,
    totalExpense: Double,
    telecomBalance: Double,
    packages: List<com.ethiobalance.app.data.BalancePackageEntity>,
    transactions: List<TransactionEntity>,
    onViewAllTransactions: () -> Unit
) {
    val netBalance = totalIncome - totalExpense
    val fmt = NumberFormat.getNumberInstance(Locale.US).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .statusBarsPadding()
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = Translations.t(language, "dashboard"),
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Black,
                    color = Slate900
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${Translations.t(language, "welcome")}, $userName",
                        fontSize = 12.sp,
                        color = Slate400
                    )
                    if (userPhone.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = null,
                            tint = Blue600,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = userPhone,
                            fontSize = 12.sp,
                            color = Blue600
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Financial Summary Card
        Surface(
            shape = RoundedCornerShape(40.dp),
            color = Color.White,
            border = androidx.compose.foundation.BorderStroke(1.dp, Slate100),
            shadowElevation = 2.dp
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Blue50),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountBalanceWallet,
                                contentDescription = null,
                                tint = Blue600,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = Translations.t(language, "netCashFlow").uppercase(),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate400,
                            letterSpacing = 2.sp
                        )
                    }

                    Icon(
                        imageVector = if (netBalance >= 0) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                        contentDescription = null,
                        tint = if (netBalance >= 0) Emerald600 else Rose600,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "ETB ${fmt.format(netBalance)}",
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Black,
                    color = if (netBalance >= 0) Emerald600 else Rose600
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Telecom Assets Card (dark)
        Surface(
            shape = RoundedCornerShape(40.dp),
            color = Slate900
        ) {
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
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Overall Summary
        SectionHeader(Translations.t(language, "overallSummary"))
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SummaryCard(
                label = Translations.t(language, "income"),
                amount = totalIncome,
                isIncome = true,
                modifier = Modifier.weight(1f)
            )
            SummaryCard(
                label = Translations.t(language, "expense"),
                amount = totalExpense,
                isIncome = false,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Recent Activity
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SectionHeader(Translations.t(language, "recentActivity"))
            TextButton(onClick = onViewAllTransactions) {
                Text(
                    text = Translations.t(language, "viewAll"),
                    fontSize = 12.sp,
                    color = Blue600,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        val recentTransactions = transactions.take(4)
        if (recentTransactions.isEmpty()) {
            Text(
                text = "No recent transactions",
                fontSize = 14.sp,
                color = Slate400,
                modifier = Modifier.padding(vertical = 16.dp)
            )
        } else {
            recentTransactions.forEach { tx ->
                TransactionItem(transaction = tx)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        Spacer(modifier = Modifier.height(100.dp)) // Bottom nav padding
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text.uppercase(),
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        color = Slate400,
        letterSpacing = 2.sp
    )
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
