package com.ethiobalance.app.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ethiobalance.app.ui.Translations
import com.ethiobalance.app.ui.theme.*
import java.text.NumberFormat
import java.util.Locale

@Composable
fun SummaryCard(
    language: String,
    netBalance: Double,
    totalIncome: Double,
    totalExpense: Double,
    transactionCount: Int? = null,
    timeFilter: String? = null,
    sourceFilter: String? = null,
    lastActivity: String? = null,
    showAmounts: Boolean = true,
    onToggleAmounts: (() -> Unit)? = null,
    isSyncing: Boolean = false,
    onSync: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val fmt = NumberFormat.getNumberInstance(Locale.US).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = Slate900,
        border = BorderStroke(1.dp, Slate800),
        shadowElevation = 8.dp,
        modifier = modifier.fillMaxWidth().animateContentSize()
    ) {
        // Optional background gradient effect to simulate Tailwind's bg-gradient-to-br from-slate-900 via-slate-800 to-slate-900
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.linearGradient(listOf(Slate900, Slate800, Slate900)))
        )

        Column(modifier = Modifier.padding(20.dp)) {
            // Header Row (NET BALANCE, Sync button, Eye icon)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Emerald600.copy(alpha = 0.1f))
                            .border(1.dp, Emerald500.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.AccountBalanceWallet, null, tint = Emerald400, modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = Translations.t(language, "netBalance").takeIf { it.isNotEmpty() }?.uppercase() ?: "NET BALANCE",
                                fontSize = 10.sp, fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp, color = Slate400
                            )
                            if (onSync != null && transactionCount == null) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    color = Emerald600.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, Emerald500.copy(alpha = 0.4f)),
                                    onClick = onSync,
                                    enabled = !isSyncing
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        if (isSyncing) {
                                            CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 2.dp, color = Emerald400)
                                        } else {
                                            Icon(Icons.Default.Refresh, null, tint = Emerald400, modifier = Modifier.size(12.dp))
                                        }
                                        Spacer(Modifier.width(4.dp))
                                        Text(Translations.t(language, "sync"), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Emerald400)
                                    }
                                }
                            }
                        }
                        Text(
                            text = sourceFilter?.let { com.ethiobalance.app.AppConstants.displaySource(it) } ?: Translations.t(language, "overallSummary").takeIf { it.isNotEmpty() } ?: "Overall Summary",
                            fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                            color = Slate500
                        )
                    }
                }
                
                if (onToggleAmounts != null) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Slate800)
                            .border(1.dp, Slate700, RoundedCornerShape(12.dp))
                            .clickable { onToggleAmounts() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (showAmounts) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            null, tint = Slate300, modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Fake pill row for Home Screen
            if (transactionCount == null && timeFilter == null) {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0F172A).copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                        .border(1.dp, Slate800, RoundedCornerShape(12.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(modifier = Modifier.background(Slate700, RoundedCornerShape(8.dp)).padding(horizontal = 10.dp, vertical = 4.dp)) {
                        Text(Translations.t(language, "allTime").takeIf { it.isNotEmpty() } ?: "All Time", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    Box(modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)) {
                        Text(Translations.t(language, "today").takeIf { it.isNotEmpty() } ?: "Today", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Slate400)
                    }
                    Box(modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)) {
                        Text(Translations.t(language, "thisWeek").takeIf { it.isNotEmpty() } ?: "Weekly", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Slate400)
                    }
                    Box(modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)) {
                        Text(Translations.t(language, "thisMonth").takeIf { it.isNotEmpty() } ?: "Monthly", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Slate400)
                    }
                }
            }
            
            // Transaction Time Filter (if transaction mode)
            if (timeFilter != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CalendarToday, null, tint = Slate400, modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        timeFilter.uppercase(),
                        fontSize = 10.sp, fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp, color = Slate400
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Main Balance Row
            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        if (!showAmounts) {
                            Text("••••••••", fontSize = 28.sp, fontWeight = FontWeight.Black, color = Color.White)
                        } else {
                            Text(
                                text = fmt.format(netBalance),
                                fontSize = 32.sp, fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                letterSpacing = (-1).sp
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "ETB",
                            fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Emerald400,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.AutoMirrored.Filled.TrendingUp, null, tint = Emerald400, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Net Cash Flow", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Slate400)
                        Spacer(Modifier.width(4.dp))
                        Text(
                            if (netBalance >= 0) "+ Positive" else "- Deficit", 
                            fontSize = 12.sp, fontWeight = FontWeight.Bold, 
                            color = if (netBalance >= 0) Emerald400 else Rose400
                        )
                    }
                }
            }

            // Income / Expense Breakdown
            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = Slate700.copy(alpha = 0.6f))
            Spacer(modifier = Modifier.height(14.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                // Income Box
                Box(
                    modifier = Modifier.weight(1f)
                        .background(Emerald600.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                        .border(1.dp, Emerald500.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                        .padding(12.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.background(Emerald500.copy(alpha = 0.2f), RoundedCornerShape(6.dp)).padding(4.dp)) {
                                Icon(Icons.AutoMirrored.Filled.TrendingUp, null, tint = Emerald400, modifier = Modifier.size(14.dp))
                            }
                            Spacer(Modifier.width(6.dp))
                            Text(
                                Translations.t(language, "income").takeIf { it.isNotEmpty() } ?: "Income",
                                fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Emerald400
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            if (showAmounts) "+${fmt.format(totalIncome)}" else "••••••",
                            fontSize = 16.sp, fontWeight = FontWeight.Black, color = Color.White
                        )
                        Text(
                            "ETB RECEIVED",
                            fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = Emerald300.copy(alpha = 0.7f), letterSpacing = 1.sp,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
                
                // Expense Box
                Box(
                    modifier = Modifier.weight(1f)
                        .background(Rose600.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                        .border(1.dp, Rose500.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                        .padding(12.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.background(Rose500.copy(alpha = 0.2f), RoundedCornerShape(6.dp)).padding(4.dp)) {
                                Icon(Icons.AutoMirrored.Filled.TrendingDown, null, tint = Rose400, modifier = Modifier.size(14.dp))
                            }
                            Spacer(Modifier.width(6.dp))
                            Text(
                                Translations.t(language, "expense").takeIf { it.isNotEmpty() } ?: "Expense",
                                fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Rose400
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            if (showAmounts) "-${fmt.format(totalExpense)}" else "••••••",
                            fontSize = 16.sp, fontWeight = FontWeight.Black, color = Color.White
                        )
                        Text(
                            "ETB SPENT",
                            fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = Rose300.copy(alpha = 0.7f), letterSpacing = 1.sp,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }

            if (transactionCount != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        Translations.t(language, "transactions").uppercase(),
                        fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Slate400, letterSpacing = 2.sp
                    )
                    Text(
                        text = transactionCount.toString(),
                        fontSize = 16.sp, fontWeight = FontWeight.Black, color = Color.White
                    )
                }
            }
            
            if (lastActivity != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "LAST ACTIVITY: $lastActivity",
                    fontSize = 8.sp, fontWeight = FontWeight.Bold,
                    color = Slate500, letterSpacing = 2.sp,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
