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
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
    homeTimeFilter: String = "ALL_TIME",
    onHomeTimeFilterSelected: ((String) -> Unit)? = null,
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
        Column(modifier = Modifier.padding(24.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(Slate800, RoundedCornerShape(10.dp))
                            .border(1.dp, Slate700, RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.AccountBalanceWallet, null, tint = Emerald400, modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            Translations.t(language, "netBalance").takeIf { it.isNotEmpty() } ?: "Net Balance",
                            fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Slate400,
                            letterSpacing = 1.sp
                        )
                        if (sourceFilter != null) {
                            Text(
                                sourceFilter.uppercase(),
                                fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Slate300
                            )
                        }
                    }
                }
                
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (onSync != null) {
                        Surface(
                            shape = CircleShape,
                            color = Slate800,
                            border = BorderStroke(1.dp, Slate700),
                            modifier = Modifier.clip(CircleShape).clickable(enabled = !isSyncing) { onSync() }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                if (isSyncing) {
                                    CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 2.dp, color = Emerald400)
                                } else {
                                    Icon(Icons.Default.Refresh, null, tint = Emerald400, modifier = Modifier.size(12.dp))
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(Translations.t(language, "sync"), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Emerald400)
                            }
                        }
                    }

                    if (onToggleAmounts != null) {
                        Surface(
                            shape = CircleShape,
                            color = Slate800,
                            border = BorderStroke(1.dp, Slate700),
                            modifier = Modifier.clip(CircleShape).clickable { onToggleAmounts() }
                        ) {
                            Box(modifier = Modifier.padding(8.dp)) {
                                Icon(
                                    if (showAmounts) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = "Toggle amounts",
                                    tint = Slate400,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

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
                    val filters = listOf("ALL_TIME" to "allTime", "TODAY" to "today", "THIS_WEEK" to "thisWeek", "THIS_MONTH" to "thisMonth")
                    filters.forEach { (key, translateKey) ->
                        val isSelected = homeTimeFilter == key
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onHomeTimeFilterSelected?.invoke(key) }
                                .background(if (isSelected) Slate700 else Color.Transparent, RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                Translations.t(language, translateKey).takeIf { it.isNotEmpty() } ?: translateKey,
                                fontSize = 11.sp, 
                                fontWeight = FontWeight.Bold, 
                                color = if (isSelected) Color.White else Slate400
                            )
                        }
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
