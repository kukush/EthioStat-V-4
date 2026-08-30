package com.ethiobalance.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ethiobalance.app.data.TransactionEntity
import com.ethiobalance.app.ui.Translations
import com.ethiobalance.app.ui.components.SummaryCard
import com.ethiobalance.app.ui.components.TelecomAssetCard
import com.ethiobalance.app.ui.components.TransactionItem
import com.ethiobalance.app.ui.theme.*
import java.text.NumberFormat
import java.util.Locale

@Composable
fun HomeScreen(
    userName: String,
    language: String,
    totalIncome: Double,
    totalExpense: Double,
    packages: List<com.ethiobalance.app.data.BalancePackageEntity>,
    transactions: List<TransactionEntity>,
    bankBalances: Map<String, Double> = emptyMap(),
    isSyncing: Boolean = false,
    onSync: () -> Unit = {},
    onViewAllTransactions: () -> Unit,
) {
    var showAmounts by remember { mutableStateOf(true) }
    var timeFilter by remember { mutableStateOf("allTime") }
    var customStartMs by remember { mutableStateOf<Long?>(null) }
    var customEndMs by remember { mutableStateOf<Long?>(null) }
    
    val fmt = NumberFormat.getNumberInstance(Locale.US).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }

    val now = System.currentTimeMillis()
    val filteredTransactions = remember(transactions, timeFilter, customStartMs, customEndMs) {
        val startOfToday = java.util.Calendar.getInstance().apply {
            timeInMillis = now
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis

        val sevenDaysAgo = now - 7L * 24 * 60 * 60 * 1000L
        val thirtyDaysAgo = now - 30L * 24 * 60 * 60 * 1000L
        val oneYearAgo = now - 365L * 24 * 60 * 60 * 1000L

        transactions.filter {
            when (timeFilter) {
                "today" -> it.timestamp >= startOfToday
                "thisWeek" -> it.timestamp >= sevenDaysAgo
                "thisMonth" -> it.timestamp >= thirtyDaysAgo
                "yearly" -> it.timestamp >= oneYearAgo
                "custom" -> {
                    val start = customStartMs ?: 0L
                    val end = customEndMs ?: Long.MAX_VALUE
                    it.timestamp in start..end
                }
                else -> true
            }
        }
    }

    val financialTransactions = filteredTransactions.filter {
        it.source != com.ethiobalance.app.AppConstants.SOURCE_AIRTIME
    }

    val currentTotalIncome = financialTransactions.filter { it.type.equals("INCOME", ignoreCase = true) }.sumOf { it.amount }
    val currentTotalExpense = financialTransactions.filter { it.type.equals("EXPENSE", ignoreCase = true) }.sumOf { it.amount }
    val currentNetBalance = currentTotalIncome - currentTotalExpense

    val groupedTransactions = financialTransactions.groupBy {
        it.source
    }

    val txSources = groupedTransactions.keys.filter { it != "Unknown" }
    val uniqueSources = txSources.distinct().sorted()

    val internetPkgs = packages.filter { it.type.contains("internet", ignoreCase = true) || it.type.contains("data", ignoreCase = true) }
    val dataVol = internetPkgs.sumOf {
        val v = it.remainingAmount
        if (it.unit.equals("GB", ignoreCase = true)) v else v / 1024.0
    }

    val voicePkgs = packages.filter { it.type.contains("voice", ignoreCase = true) }
    val voiceVol = voicePkgs.sumOf { it.remainingAmount }

    val smsPkgs = packages.filter { it.type.contains("sms", ignoreCase = true) }
    val smsVol = smsPkgs.sumOf { it.remainingAmount }
    
    val activeBanks = com.ethiobalance.app.AppConstants.KNOWN_BANKS.filter { uniqueSources.contains(it.abbreviation) || bankBalances.containsKey(it.abbreviation) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            // Simulating dark mode for Home Screen body
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        
        // Welcome Header
        Text(
            text = "${Translations.t(language, "welcome").takeIf { it.isNotEmpty() } ?: "WELCOME"}, ${userName.ifEmpty { "USER" }}".uppercase(),
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Slate400,
            letterSpacing = 2.sp,
            modifier = Modifier.padding(start = 4.dp, bottom = 12.dp)
        )

        // Financial Summary Card
        SummaryCard(
            language = language,
            netBalance = currentNetBalance,
            totalIncome = currentTotalIncome,
            totalExpense = currentTotalExpense,
            showAmounts = showAmounts,
            onToggleAmounts = { showAmounts = !showAmounts },
            isSyncing = isSyncing,
            onSync = onSync,
            homeTimeFilter = timeFilter,
            onHomeTimeFilterSelected = { timeFilter = it },
            homeCustomStartMs = customStartMs,
            homeCustomEndMs = customEndMs,
            onHomeCustomRangeChange = { start, end ->
                customStartMs = start
                customEndMs = end
                timeFilter = "custom"
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Telecom Assets Card
        TelecomAssetCard(
            language = language,
            dataVol = dataVol,
            voiceVol = voiceVol,
            smsVol = smsVol,
            airtimeBalance = 0.0, // Set if available
            isCompact = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Source Summaries (Dark Grid)
        if (activeBanks.isNotEmpty()) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Slate900,
                border = BorderStroke(1.dp, Slate800),
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Purple600.copy(alpha = 0.1f))
                                    .border(1.dp, Purple500.copy(alpha = 0.2f), RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.AccountBalanceWallet, null, tint = Purple400, modifier = Modifier.size(16.dp))
                            }
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(
                                    "SOURCE SUMMARIES",
                                    fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Slate400, letterSpacing = 2.sp
                                )
                                Text(
                                    "Active Ethiopian Accounts",
                                    fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = Slate500
                                )
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { /* Handle Settings navigation if needed */ }) {
                            Text(Translations.t(language, "manage").takeIf { it.isNotEmpty() } ?: "Manage", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Emerald400)
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = Emerald400, modifier = Modifier.size(16.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 2-Column Grid for Banks
                    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        activeBanks.chunked(2).forEach { rowBanks ->
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                rowBanks.forEach { bank ->
                                    val bal = bankBalances[bank.abbreviation] ?: 0.0
                                    val colorPrimary = Emerald500
                                    
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .background(Slate900.copy(alpha = 0.7f), RoundedCornerShape(16.dp))
                                            .border(1.dp, Slate800, RoundedCornerShape(16.dp))
                                            .padding(12.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(28.dp)
                                                        .background(colorPrimary, RoundedCornerShape(8.dp)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        bank.abbreviation.take(3).uppercase(),
                                                        fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = Color.White
                                                    )
                                                }
                                                Spacer(Modifier.width(8.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        bank.fullName,
                                                        fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White,
                                                        maxLines = 1, overflow = TextOverflow.Ellipsis
                                                    )
                                                    Text(
                                                        bank.senderId,
                                                        fontSize = 9.sp, fontWeight = FontWeight.Medium, color = Slate400
                                                    )
                                                }
                                            }
                                            Column(horizontalAlignment = Alignment.End) {
                                                Text(
                                                    fmt.format(bal),
                                                    fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color.White
                                                )
                                                Text(
                                                    "ETB",
                                                    fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, color = Slate500
                                                )
                                            }
                                        }
                                    }
                                }
                                // Fill empty space if odd number of items
                                if (rowBanks.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
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
                fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Emerald400, letterSpacing = 2.sp,
                modifier = Modifier.clickable { onViewAllTransactions() }
            )
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        val recentTransactions = financialTransactions.take(4)
        if (recentTransactions.isEmpty()) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Slate900,
                border = BorderStroke(1.dp, Slate800),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "No recent transactions",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate500,
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
        
        Spacer(modifier = Modifier.height(120.dp))
    }
}
