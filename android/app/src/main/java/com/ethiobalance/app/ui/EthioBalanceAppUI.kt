package com.ethiobalance.app.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.core.content.ContextCompat
import com.ethiobalance.app.constants.Languages
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ethiobalance.app.R
import com.ethiobalance.app.ui.components.BottomNavBar
import com.ethiobalance.app.ui.screens.*
import com.ethiobalance.app.ui.theme.*
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

    // ── Permission state ──────────────────────────────────────────────────
    val context = LocalContext.current
    fun checkPermissions(): Boolean {
        val read = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED
        val receive = ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED
        return read && receive
    }

    var smsPermissionGranted by remember { mutableStateOf(checkPermissions()) }

    // Re-check on every resume (handles grant from system settings or dialog)
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        smsPermissionGranted = checkPermissions()
    }

    // Permission request launcher (used from Settings screen)
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val allGranted = results.values.all { it }
        smsPermissionGranted = allGranted
        if (allGranted) {
            settingsVM.onPermissionGranted()
        }
    }

    EthioBalanceTheme(themeId = theme) {
        Scaffold(
            topBar = {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 1.dp,
                    modifier = Modifier.statusBarsPadding()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(R.mipmap.ic_launcher_foreground),
                            contentDescription = stringResource(R.string.app_name),
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = stringResource(R.string.app_name), fontSize = 18.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface, letterSpacing = (-0.5).sp)
                        Spacer(modifier = Modifier.weight(1f))
                        // Language dropdown
                        var langMenuExpanded by remember { mutableStateOf(false) }
                        Box {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { langMenuExpanded = true }
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Language, contentDescription = "Language", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = when (language) { "am" -> "አማ"; "om" -> "OR"; else -> "EN" },
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                            }
                            DropdownMenu(
                                expanded = langMenuExpanded,
                                onDismissRequest = { langMenuExpanded = false }
                            ) {
                                Languages.SUPPORTED.forEach { lang ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                lang.displayName,
                                                fontWeight = if (language == lang.code) FontWeight.Bold else FontWeight.Normal,
                                                color = if (language == lang.code) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                            )
                                        },
                                        onClick = {
                                            settingsVM.setLanguage(lang.code)
                                            langMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            },
            bottomBar = {
                BottomNavBar(
                    currentRoute = currentRoute,
                    language = language,
                    onTabSelected = { currentRoute = it },
                    hasPermissionWarning = !smsPermissionGranted
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
                        val packages by homeVM.packages.collectAsStateWithLifecycle()
                        val transactions by homeVM.transactions.collectAsStateWithLifecycle()
                        val bankBalances by homeVM.bankBalances.collectAsStateWithLifecycle()

                        HomeScreen(
                            userName = userName,
                            userPhone = userPhone,
                            language = language,
                            totalIncome = totalIncome,
                            totalExpense = totalExpense,
                            packages = packages,
                            transactions = transactions,
                            bankBalances = bankBalances,
                            onViewAllTransactions = { currentRoute = "transactions" }
                        )
                    }

                    "telecom" -> {
                        val packages by telecomVM.packages.collectAsStateWithLifecycle()
                        val isSyncing by telecomVM.isSyncing.collectAsStateWithLifecycle()
                        val syncError by telecomVM.syncError.collectAsStateWithLifecycle()
                        val syncWarning by telecomVM.syncWarning.collectAsStateWithLifecycle()

                        TelecomScreen(
                            language = language,
                            packages = packages,
                            isSyncing = isSyncing,
                            syncError = syncError,
                            syncWarning = syncWarning,
                            smsPermissionGranted = smsPermissionGranted,
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
                        val customStartMs by transactionVM.customStartMs.collectAsStateWithLifecycle()
                        val customEndMs by transactionVM.customEndMs.collectAsStateWithLifecycle()

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
                            customStartMs = customStartMs,
                            customEndMs = customEndMs,
                            onTimeFilterChange = { transactionVM.setTimeFilter(it) },
                            onSourceFilterChange = { transactionVM.setSourceFilter(it) },
                            onSearchChange = { transactionVM.setSearchQuery(it) },
                            onCustomRangeChange = { start, end -> transactionVM.setCustomRange(start, end) },
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
                            smsPermissionGranted = smsPermissionGranted,
                            onThemeChange = { settingsVM.setTheme(it) },
                            onProfileUpdate = { n, p, a -> settingsVM.setUserProfile(n, p, a) },
                            onAddSource = { settingsVM.addTransactionSource(it) },
                            onRemoveSource = { settingsVM.removeTransactionSource(it) },
                            onRequestPermissions = {
                                permissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.READ_SMS,
                                        Manifest.permission.RECEIVE_SMS
                                    )
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}
