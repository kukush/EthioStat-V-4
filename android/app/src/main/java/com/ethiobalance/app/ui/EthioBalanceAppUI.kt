package com.ethiobalance.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ethiobalance.app.ui.components.BottomNavBar
import com.ethiobalance.app.ui.screens.*
import com.ethiobalance.app.ui.theme.EthioBalanceTheme
import com.ethiobalance.app.ui.viewmodel.*

@Composable
fun EthioBalanceAppUI() {
    val homeVM: HomeViewModel = hiltViewModel()
    val telecomVM: TelecomViewModel = hiltViewModel()
    val transactionVM: TransactionViewModel = hiltViewModel()
    val settingsVM: SettingsViewModel = hiltViewModel()

    val theme by settingsVM.theme.collectAsStateWithLifecycle()
    val language by settingsVM.language.collectAsStateWithLifecycle()

    var currentRoute by remember { mutableStateOf("home") }

    EthioBalanceTheme(themeId = theme) {
        Scaffold(
            bottomBar = {
                BottomNavBar(
                    currentRoute = currentRoute,
                    language = language,
                    onTabSelected = { currentRoute = it }
                )
            }
        ) { innerPadding ->
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                when (currentRoute) {
                    "home" -> {
                        val userName by homeVM.userName.collectAsStateWithLifecycle()
                        val userPhone by homeVM.userPhone.collectAsStateWithLifecycle()
                        val totalIncome by homeVM.totalIncome.collectAsStateWithLifecycle()
                        val totalExpense by homeVM.totalExpense.collectAsStateWithLifecycle()
                        val telecomBalance by homeVM.telecomBalance.collectAsStateWithLifecycle()
                        val packages by homeVM.packages.collectAsStateWithLifecycle()
                        val transactions by homeVM.transactions.collectAsStateWithLifecycle()
                        val bankBalances by homeVM.bankBalances.collectAsStateWithLifecycle()

                        HomeScreen(
                            userName = userName,
                            userPhone = userPhone,
                            language = language,
                            totalIncome = totalIncome,
                            totalExpense = totalExpense,
                            telecomBalance = telecomBalance,
                            packages = packages,
                            transactions = transactions,
                            bankBalances = bankBalances,
                            onViewAllTransactions = { currentRoute = "transactions" }
                        )
                    }

                    "telecom" -> {
                        val packages by telecomVM.packages.collectAsStateWithLifecycle()
                        val telecomBalance by telecomVM.telecomBalance.collectAsStateWithLifecycle()
                        val isSyncing by telecomVM.isSyncing.collectAsStateWithLifecycle()
                        val syncError by telecomVM.syncError.collectAsStateWithLifecycle()
                        val syncWarning by telecomVM.syncWarning.collectAsStateWithLifecycle()

                        TelecomScreen(
                            language = language,
                            packages = packages,
                            telecomBalance = telecomBalance,
                            isSyncing = isSyncing,
                            syncError = syncError,
                            syncWarning = syncWarning,
                            onSync = { telecomVM.handleSync() },
                            onRecharge = { telecomVM.rechargeViaUssd(it) },
                            onTransfer = { r, a -> telecomVM.transferAirtime(r, a) }
                        )
                    }

                    "transactions" -> {
                        val context = LocalContext.current
                        val transactions by transactionVM.filteredTransactions.collectAsStateWithLifecycle()
                        val totalIncome by transactionVM.totalIncome.collectAsStateWithLifecycle()
                        val totalExpense by transactionVM.totalExpense.collectAsStateWithLifecycle()
                        val uniqueSources by transactionVM.uniqueSources.collectAsStateWithLifecycle()
                        val timeFilter by transactionVM.timeFilter.collectAsStateWithLifecycle()
                        val sourceFilter by transactionVM.sourceFilter.collectAsStateWithLifecycle()
                        val searchQuery by transactionVM.searchQuery.collectAsStateWithLifecycle()
                        val isScanning by transactionVM.isScanningHistory.collectAsStateWithLifecycle()

                        TransactionScreen(
                            language = language,
                            transactions = transactions,
                            totalIncome = totalIncome,
                            totalExpense = totalExpense,
                            uniqueSources = uniqueSources,
                            timeFilter = timeFilter,
                            sourceFilter = sourceFilter,
                            searchQuery = searchQuery,
                            _isScanningHistory = isScanning,
                            onTimeFilterChange = { transactionVM.setTimeFilter(it) },
                            onSourceFilterChange = { transactionVM.setSourceFilter(it) },
                            onSearchChange = { transactionVM.setSearchQuery(it) },
                            onExportCsv = { transactionVM.exportToCsv(context) },
                            _onScanAll = { transactionVM.scanSmsHistory() }
                        )
                    }

                    "settings" -> {
                        val userName by settingsVM.userName.collectAsStateWithLifecycle()
                        val userPhone by settingsVM.userPhone.collectAsStateWithLifecycle()
                        val userAvatar by settingsVM.userAvatar.collectAsStateWithLifecycle()
                        val transactionSources by settingsVM.transactionSources.collectAsStateWithLifecycle()

                        SettingsScreen(
                            language = language,
                            theme = theme,
                            userName = userName,
                            userPhone = userPhone,
                            userAvatar = userAvatar,
                            transactionSources = transactionSources,
                            onLanguageChange = { settingsVM.setLanguage(it) },
                            onThemeChange = { settingsVM.setTheme(it) },
                            onProfileUpdate = { n, p, a -> settingsVM.setUserProfile(n, p, a) },
                            onAddSource = { settingsVM.addTransactionSource(it) },
                            onRemoveSource = { settingsVM.removeTransactionSource(it) },
                            onClearData = { settingsVM.clearAllData() }
                        )
                    }
                }
            }
        }
    }
}
