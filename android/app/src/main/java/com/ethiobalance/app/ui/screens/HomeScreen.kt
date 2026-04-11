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
import com.ethiobalance.app.ui.components.SummaryCard
import com.ethiobalance.app.ui.components.TelecomAssetCard
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
    bankBalances: Map<String, Double> = emptyMap(),
    onViewAllTransactions: () -> Unit
) {
    val netBalance = totalIncome - totalExpense
    val fmt = NumberFormat.getNumberInstance(Locale.US).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }

    // Group transactions by resolved source name to normalize ("127" and "Telebirr" become "TeleBirr")
    // and filter out "AIRTIME" transactions from financial summaries.
    val financialTransactions = transactions.filter { 
        com.ethiobalance.app.AppConstants.resolveSource(it.source) != com.ethiobalance.app.AppConstants.SOURCE_AIRTIME 
    }
    
    val groupedTransactions = financialTransactions.groupBy { 
        com.ethiobalance.app.AppConstants.resolveSource(it.source) 
    }
    
    // Include sources that have transactions OR bank balances
    val txSources = groupedTransactions.keys.filter { it != "Unknown" }
    val balanceSources = bankBalances.keys
    val uniqueSources = (txSources + balanceSources).distinct().sorted()

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
        SummaryCard(
            language = language,
            netBalance = netBalance,
            totalIncome = totalIncome,
            totalExpense = totalExpense
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Telecom Assets Card
        TelecomAssetCard(
            language = language,
            telecomBalance = telecomBalance,
            dataVol = dataVol,
            voiceVol = voiceVol,
            smsVol = smsVol
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Source Summaries
        if (uniqueSources.isNotEmpty()) {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                Text("SOURCE SUMMARIES", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Slate400, letterSpacing = 2.sp)
            }
            Spacer(modifier = Modifier.height(16.dp))
            uniqueSources.forEach { src ->
                val srcTxs = groupedTransactions[src] ?: emptyList()
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
                            Text(
                                if (srcTxs.isNotEmpty()) "${srcTxs.size} Transactions" else "Balance only",
                                fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Slate400, letterSpacing = 1.sp, modifier = Modifier.padding(top=2.dp)
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            if (srcTxs.isNotEmpty()) {
                                Text(fmt.format(srcNet), fontSize = 14.sp, fontWeight = FontWeight.Black, color = if(srcNet >= 0) Emerald600 else Rose600)
                                Text("NET FLOW", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Slate400, letterSpacing = 1.sp, modifier = Modifier.padding(top=2.dp))
                            }
                            val bal = bankBalances[src]
                            if (bal != null) {
                                Text(fmt.format(bal), fontSize = 14.sp, fontWeight = FontWeight.Black, color = Blue600)
                                Text("BALANCE", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Slate400, letterSpacing = 1.sp, modifier = Modifier.padding(top=2.dp))
                            }
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

        val recentTransactions = financialTransactions.take(4)
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
