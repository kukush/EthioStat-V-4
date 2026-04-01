package com.ethiobalance.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ethiobalance.app.data.TransactionEntity
import com.ethiobalance.app.ui.Translations
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

    val uniqueSources = transactions.map { it.source }.distinct().filter { it != "Unknown" }

    // Telecom Package computations
    val internetPkgs = packages.filter { it.type.contains("internet", ignoreCase = true) || it.type.contains("data", ignoreCase = true) }
    val dataVol = internetPkgs.sumOf { 
        val v = it.remainingAmount
        if (it.unit.equals("GB", ignoreCase = true)) v else v / 1024.0
    }
    
    val voicePkgs = packages.filter { it.type.contains("voice", ignoreCase = true) }
    val voiceVol = voicePkgs.sumOf { it.remainingAmount }
    
    val smsPkgs = packages.filter { it.type.contains("sms", ignoreCase = true) }
    val smsVol = smsPkgs.sumOf { it.remainingAmount }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .statusBarsPadding()
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Header
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = Translations.t(language, "dashboard").takeIf { it.isNotEmpty() } ?: "Dashboard",
                fontSize = 30.sp,
                fontWeight = FontWeight.Black,
                color = Slate900,
                letterSpacing = (-1).sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${Translations.t(language, "welcome").takeIf { it.isNotEmpty() } ?: "WELCOME"}, ${userName.ifEmpty { "USER" }}".uppercase(),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Slate400,
                letterSpacing = 2.sp
            )
            if (userPhone.isNotEmpty() && userPhone != "Unknown") {
                Text(
                    text = userPhone,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Blue500,
                    letterSpacing = 2.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            } else {
                Text(
                    text = "PRIMARY SIM ACTIVE",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate300,
                    letterSpacing = 2.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Financial Summary Card
        Surface(
            shape = RoundedCornerShape(40.dp),
            color = Color.White,
            border = androidx.compose.foundation.BorderStroke(1.dp, Slate100),
            shadowElevation = 2.dp
        ) {
            Column(modifier = Modifier.padding(24.dp).fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(Blue50), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.AccountBalanceWallet, null, tint = Blue600, modifier = Modifier.size(16.dp))
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            Translations.t(language, "financialSummary").takeIf { it.isNotEmpty() }?.uppercase() ?: "FINANCIAL SUMMARY",
                            fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Slate400, letterSpacing = 2.sp
                        )
                    }
                    Text("HISTORY", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Blue600, letterSpacing = 2.sp)
                }

                Spacer(modifier = Modifier.height(32.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                    Column {
                        Text("NET CASH FLOW", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Slate400, letterSpacing = 2.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text("ETB", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Slate400, modifier = Modifier.padding(bottom = 6.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(
                                fmt.format(netBalance),
                                fontSize = 32.sp, fontWeight = FontWeight.Black,
                                color = if (netBalance >= 0) Emerald600 else Rose600,
                                letterSpacing = (-1).sp
                            )
                        }
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.TrendingUp, null, tint = Emerald600, modifier = Modifier.size(12.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("+${fmt.format(totalIncome)}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Emerald600)
                        }
                        Spacer(Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.TrendingDown, null, tint = Rose600, modifier = Modifier.size(12.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("-${fmt.format(totalExpense)}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Rose600)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Telecom Assets Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(40.dp))
                .background(Slate900)
                .drawBehind {
                    // Decorative glow effects matching "blur-3xl" logic
                    drawCircle(
                        brush = Brush.radialGradient(listOf(Blue600.copy(alpha=0.3f), Color.Transparent)),
                        radius = size.width * 0.4f,
                        center = androidx.compose.ui.geometry.Offset(size.width * 0.9f, size.height * 0.1f)
                    )
                    drawCircle(
                        brush = Brush.radialGradient(listOf(Purple600.copy(alpha=0.3f), Color.Transparent)),
                        radius = size.width * 0.4f,
                        center = androidx.compose.ui.geometry.Offset(size.width * 0.1f, size.height * 0.9f)
                    )
                }
        ) {
            Column(modifier = Modifier.padding(24.dp).fillMaxWidth()) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(Color.White.copy(alpha=0.1f)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Bolt, null, tint = Blue400, modifier = Modifier.size(16.dp))
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            Translations.t(language, "telecomAssets").takeIf { it.isNotEmpty() }?.uppercase() ?: "TELECOM ASSETS",
                            fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha=0.6f), letterSpacing = 2.sp
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Language, null, tint = Blue400, modifier = Modifier.size(12.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("ETHIO TELECOM", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Blue400, letterSpacing = 2.sp)
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    // Left Column: Airtime
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            Translations.t(language, "availableAirtime").takeIf { it.isNotEmpty() }?.uppercase() ?: "AVAILABLE AIRTIME",
                            fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha=0.6f), letterSpacing = 2.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text("ETB", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha=0.6f), modifier = Modifier.padding(bottom=4.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(
                                fmt.format(telecomBalance),
                                fontSize = 28.sp, fontWeight = FontWeight.Black,
                                color = Color.White, letterSpacing = (-1).sp
                            )
                        }
                    }

                    // Right Column: Package Summaries
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.width(4.dp).height(24.dp).clip(CircleShape).background(Blue500))
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text("DATA", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha=0.6f), letterSpacing = 2.sp)
                                Row(verticalAlignment = Alignment.Bottom) {
                                    Text("%.1f".format(Locale.US, dataVol), fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color.White)
                                    Text(" GB", fontSize = 8.sp, color = Color.White.copy(alpha=0.6f), modifier = Modifier.padding(bottom=1.dp))
                                }
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.width(4.dp).height(24.dp).clip(CircleShape).background(Emerald500))
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text("AUDIO", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha=0.6f), letterSpacing = 2.sp)
                                Row(verticalAlignment = Alignment.Bottom) {
                                    Text(fmt.format(voiceVol), fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color.White)
                                    Text(" Min", fontSize = 8.sp, color = Color.White.copy(alpha=0.6f), modifier = Modifier.padding(bottom=1.dp))
                                }
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.width(4.dp).height(24.dp).clip(CircleShape).background(Purple500))
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text("SMS", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha=0.6f), letterSpacing = 2.sp)
                                Row(verticalAlignment = Alignment.Bottom) {
                                    Text(fmt.format(smsVol), fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color.White)
                                    Text(" SMS", fontSize = 8.sp, color = Color.White.copy(alpha=0.6f), modifier = Modifier.padding(bottom=1.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Source Summaries
        if (uniqueSources.isNotEmpty()) {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                Text("SOURCE SUMMARIES", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Slate400, letterSpacing = 2.sp)
            }
            Spacer(modifier = Modifier.height(16.dp))
            uniqueSources.forEach { src ->
                val srcTxs = transactions.filter { it.source == src }
                val srcInc = srcTxs.filter { it.type.uppercase() == "INCOME" }.sumOf { it.amount }
                val srcExp = srcTxs.filter { it.type.uppercase() == "EXPENSE" }.sumOf { it.amount }
                val srcNet = srcInc - srcExp

                Surface(
                    shape = RoundedCornerShape(32.dp),
                    color = Color.White,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Slate100),
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    Row(modifier = Modifier.padding(20.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(48.dp).clip(RoundedCornerShape(16.dp)).background(Slate50), contentAlignment = Alignment.Center) {
                            Text(src.take(2).uppercase(), fontSize = 16.sp, fontWeight = FontWeight.Black, color = Slate900)
                        }
                        Spacer(Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(src, fontSize = 14.sp, fontWeight = FontWeight.Black, color = Slate900)
                            Text("${srcTxs.size} Transactions", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Slate400, letterSpacing = 1.sp, modifier = Modifier.padding(top=2.dp))
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(fmt.format(srcNet), fontSize = 14.sp, fontWeight = FontWeight.Black, color = if(srcNet >= 0) Emerald600 else Rose600)
                            Text("NET FLOW", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Slate400, letterSpacing = 1.sp, modifier = Modifier.padding(top=2.dp))
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        // Recent Activity
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                Translations.t(language, "recentActivity").takeIf { it.isNotEmpty() }?.uppercase() ?: "RECENT ACTIVITY",
                fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Slate400, letterSpacing = 2.sp
            )
            Text(
                Translations.t(language, "viewAll").takeIf { it.isNotEmpty() }?.uppercase() ?: "VIEW ALL",
                fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Blue600, letterSpacing = 2.sp,
                modifier = Modifier.clickable { onViewAllTransactions() }
            )
        }
        Spacer(modifier = Modifier.height(16.dp))

        val recentTransactions = transactions.take(4)
        if (recentTransactions.isEmpty()) {
            Surface(
                shape = RoundedCornerShape(32.dp),
                color = Slate50,
                border = androidx.compose.foundation.BorderStroke(1.dp, Slate100),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "No recent transactions",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate400,
                    modifier = Modifier.padding(vertical = 32.dp),
                    textAlign = TextAlign.Center
                )
            }
        } else {
            recentTransactions.forEach { tx ->
                TransactionItem(transaction = tx)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        Spacer(modifier = Modifier.height(120.dp)) // Nav bar padding
    }
}
