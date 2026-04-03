package com.ethiobalance.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import com.ethiobalance.app.ui.Translations
import com.ethiobalance.app.ui.components.BottomNavBar
import com.ethiobalance.app.ui.screens.*
import com.ethiobalance.app.ui.theme.EthioBalanceTheme
import com.ethiobalance.app.ui.viewmodel.*

@Composable
fun EthioBalanceApp() {
    val homeVM: HomeViewModel = viewModel()
    val telecomVM: TelecomViewModel = viewModel()
    val transactionVM: TransactionViewModel = viewModel()
    val settingsVM: SettingsViewModel = viewModel()

    val theme by settingsVM.theme.collectAsState()
    val language by settingsVM.language.collectAsState()

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
            Box(modifier = Modifier.fillMaxSize()) {
                when (currentRoute) {
                    "home" -> {
                        val userName by homeVM.userName.collectAsState()
                        val userPhone by homeVM.userPhone.collectAsState()
                        val totalIncome by homeVM.totalIncome.collectAsState()
                        val totalExpense by homeVM.totalExpense.collectAsState()
                        val telecomBalance by homeVM.telecomBalance.collectAsState()
                        val packages by homeVM.packages.collectAsState()
                        val transactions by homeVM.transactions.collectAsState()

                        HomeScreen(
                            userName = userName,
                            userPhone = userPhone,
                            language = language,
                            totalIncome = totalIncome,
                            totalExpense = totalExpense,
                            telecomBalance = telecomBalance,
                            packages = packages,
                            transactions = transactions,
                            onViewAllTransactions = { currentRoute = "transactions" }
                        )
                    }

                    "telecom" -> {
                        val packages by telecomVM.packages.collectAsState()
                        val telecomBalance by telecomVM.telecomBalance.collectAsState()
                        val isSyncing by telecomVM.isSyncing.collectAsState()
                        val syncError by telecomVM.syncError.collectAsState()
                        val syncWarning by telecomVM.syncWarning.collectAsState()

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
                        val transactions by transactionVM.filteredTransactions.collectAsState()
                        val totalIncome by transactionVM.totalIncome.collectAsState()
                        val totalExpense by transactionVM.totalExpense.collectAsState()
                        val uniqueSources by transactionVM.uniqueSources.collectAsState()
                        val timeFilter by transactionVM.timeFilter.collectAsState()
                        val sourceFilter by transactionVM.sourceFilter.collectAsState()
                        val searchQuery by transactionVM.searchQuery.collectAsState()
                        val isScanning by transactionVM.isScanningHistory.collectAsState()

                        TransactionScreen(
                            language = language,
                            transactions = transactions,
                            totalIncome = totalIncome,
                            totalExpense = totalExpense,
                            uniqueSources = uniqueSources,
                            timeFilter = timeFilter,
                            sourceFilter = sourceFilter,
                            searchQuery = searchQuery,
                            isScanningHistory = isScanning,
                            onTimeFilterChange = { transactionVM.setTimeFilter(it) },
                            onSourceFilterChange = { transactionVM.setSourceFilter(it) },
                            onSearchChange = { transactionVM.setSearchQuery(it) },
                            onExportCsv = { transactionVM.exportToCsv(context) },
                            onScanAll = { transactionVM.scanSmsHistory() }
                        )
                    }


                    "settings" -> {
                        val userName by settingsVM.userName.collectAsState()
                        val userPhone by settingsVM.userPhone.collectAsState()
                        val userAvatar by settingsVM.userAvatar.collectAsState()
                        val simCards by settingsVM.simCards.collectAsState()
                        val transactionSources by settingsVM.transactionSources.collectAsState()

                        SettingsScreen(
                            language = language,
                            theme = theme,
                            userName = userName,
                            userPhone = userPhone,
                            userAvatar = userAvatar,
                            simCards = simCards,
                            transactionSources = transactionSources,
                            onLanguageChange = { settingsVM.setLanguage(it) },
                            onThemeChange = { settingsVM.setTheme(it) },
                            onProfileUpdate = { n, p, a -> settingsVM.setUserProfile(n, p, a) },
                            onDetectSims = { settingsVM.detectSimCards() },
                            onDeleteSim = { settingsVM.deleteSimCard(it) },
                            onSetPrimarySim = { settingsVM.setPrimarySim(it) },
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
