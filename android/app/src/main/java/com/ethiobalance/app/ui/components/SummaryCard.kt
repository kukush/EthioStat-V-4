package com.ethiobalance.app.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
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
    transactionCount: Int? = null, // Optional for Home screen
    timeFilter: String? = null,    // Optional for Home screen
    sourceFilter: String? = null,
    lastActivity: String? = null,
    showAmounts: Boolean = true,
    onToggleAmounts: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val fmt = NumberFormat.getNumberInstance(Locale.US).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }

    Surface(
        shape = RoundedCornerShape(40.dp),
        color = Color.White,
        border = androidx.compose.foundation.BorderStroke(1.dp, Slate100),
        shadowElevation = 2.dp,
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
                        modifier = Modifier.size(32.dp).clip(CircleShape).background(Blue50),
                        contentAlignment = Alignment.Center
                    ) {
                        // Using Payments icon as requested
                        Icon(Icons.Default.Payments, null, tint = Blue600, modifier = Modifier.size(16.dp))
                    }
                    Spacer(Modifier.width(8.dp))
                    val titleLabel = sourceFilter?.let { com.ethiobalance.app.AppConstants.displaySource(it).uppercase() }
                        ?: Translations.t(language, "financialSummary").takeIf { it.isNotEmpty() }?.uppercase() 
                        ?: "FINANCIAL SUMMARY"
                    Text(
                        text = titleLabel,
                        fontSize = 10.sp, fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp, color = Slate400
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (onToggleAmounts != null) {
                        IconButton(onClick = onToggleAmounts, modifier = Modifier.size(28.dp)) {
                            Icon(
                                if (showAmounts) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                null, tint = Slate400, modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                    }
                    
                    if (timeFilter != null) {
                        Icon(Icons.Default.CalendarToday, null, tint = Slate400, modifier = Modifier.size(12.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(
                            timeFilter.uppercase(),
                            fontSize = 10.sp, fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp, color = Slate400
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Main Balance Row
            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        Translations.t(language, "netCashFlow").takeIf { it.isNotEmpty() }?.uppercase() ?: "NET BALANCE",
                        fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Slate400, letterSpacing = 2.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        if (!showAmounts) {
                            Text("••••••", fontSize = 24.sp, fontWeight = FontWeight.Black, color = if (netBalance >= 0) Emerald600 else Rose600)
                        } else {
                            Text(
                                text = "ETB", 
                                fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Slate400,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = fmt.format(netBalance),
                                fontSize = 32.sp, fontWeight = FontWeight.Black,
                                color = if (netBalance >= 0) Emerald600 else Rose600,
                                letterSpacing = (-1).sp
                            )
                        }
                    }
                }
                
                if (transactionCount != null) {
                    Column(modifier = Modifier.weight(0.5f), horizontalAlignment = Alignment.End) {
                        Text(
                            Translations.t(language, "transactions").uppercase(),
                            fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Slate400, letterSpacing = 2.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = transactionCount.toString(),
                            fontSize = 24.sp, fontWeight = FontWeight.Black, color = Slate900
                        )
                    }
                } else {
                    // For Home view, show history trends simplified
                    Column(modifier = Modifier.weight(0.5f), horizontalAlignment = Alignment.End) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.TrendingUp, null, tint = Emerald600, modifier = Modifier.size(12.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(if(showAmounts) "+${fmt.format(totalIncome)}" else "••••", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Emerald600)
                        }
                        Spacer(Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.TrendingDown, null, tint = Rose600, modifier = Modifier.size(12.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(if(showAmounts) "-${fmt.format(totalExpense)}" else "••••", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Rose600)
                        }
                    }
                }
            }

            // Income / Expense Detailed Row (Only if transactionCount is provided, i.e., Transaction Screen)
            if (transactionCount != null) {
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = Slate50)
                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(16.dp)).background(Emerald50),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.AutoMirrored.Filled.TrendingUp, null, tint = Emerald600, modifier = Modifier.size(20.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                Translations.t(language, "income").uppercase(),
                                fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Slate400, letterSpacing = 2.sp
                            )
                            Text(
                                if (showAmounts) "+${fmt.format(totalIncome)}" else "••••",
                                fontSize = 14.sp, fontWeight = FontWeight.Black, color = Emerald600
                            )
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(16.dp)).background(Rose50),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.AutoMirrored.Filled.TrendingDown, null, tint = Rose600, modifier = Modifier.size(20.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                Translations.t(language, "expense").uppercase(),
                                fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Slate400, letterSpacing = 2.sp
                            )
                            Text(
                                if (showAmounts) "-${fmt.format(totalExpense)}" else "••••",
                                fontSize = 14.sp, fontWeight = FontWeight.Black, color = Rose600
                            )
                        }
                    }
                }
            }

            if (lastActivity != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "LAST ACTIVITY: $lastActivity",
                    fontSize = 8.sp, fontWeight = FontWeight.Bold,
                    color = Slate300, letterSpacing = 2.sp,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
