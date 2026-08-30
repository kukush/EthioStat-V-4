package com.ethiobalance.app.ui

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
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
import androidx.compose.foundation.background
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
import com.ethiobalance.app.ui.screens.LockedScreen
import com.ethiobalance.app.services.BiometricAuthService

@Composable
fun EthioBalanceAppUI(biometricAuthService: BiometricAuthService? = null) {
    val homeVM: HomeViewModel = hiltViewModel()
    val telecomVM: TelecomViewModel = hiltViewModel()
    val transactionVM: TransactionViewModel = hiltViewModel()
    val settingsVM: SettingsViewModel = hiltViewModel()

    val theme by settingsVM.theme.collectAsStateWithLifecycle()
    val language by settingsVM.language.collectAsStateWithLifecycle()
    val hasSeenOnboarding by settingsVM.hasSeenOnboarding.collectAsStateWithLifecycle()
    val isBiometricEnabled by settingsVM.isBiometricEnabled.collectAsStateWithLifecycle()
    val isPinEnabled by settingsVM.isPinEnabled.collectAsStateWithLifecycle()
    val storedPin by settingsVM.storedPin.collectAsStateWithLifecycle()
    val isAutoSyncEnabled by settingsVM.isAutoSyncEnabled.collectAsStateWithLifecycle()
    
    // Auto-Sync
    LaunchedEffect(Unit) {
        if (isAutoSyncEnabled) {
            homeVM.triggerManualSync()
        }
    }
    
    var isLocked by remember { mutableStateOf(true) }

    LaunchedEffect(isBiometricEnabled, isPinEnabled) {
        if (isBiometricEnabled || isPinEnabled) {
            isLocked = true
        }
    }
    
    // Only lock if at least one security method is enabled
    val shouldLock = isBiometricEnabled || isPinEnabled

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
        if (shouldLock && isLocked) {
            val activity = androidx.compose.ui.platform.LocalContext.current as? androidx.fragment.app.FragmentActivity
            LockedScreen(
                storedPin = storedPin,
                isPinEnabled = isPinEnabled,
                isBiometricEnabled = isBiometricEnabled,
                onSetPin = { pin -> settingsVM.setPin(pin) },
                onUnlock = { isLocked = false },
                biometricAuthService = biometricAuthService,
                activity = activity,
                language = language
            )
            return@EthioBalanceTheme
        }
        // Wait for onboarding state to load from DataStore
        if (hasSeenOnboarding == null) {
            Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
            return@EthioBalanceTheme
        }

        // Onboarding gate - show onboarding only for first-time installs and until completion
        val shouldShowOnboarding = OnboardingConfig.shouldShowOnboarding(hasSeenOnboarding!!)
        Log.d("OnboardingDebug", "UI gate hasSeenOnboarding=$hasSeenOnboarding shouldShow=$shouldShowOnboarding")
        if (shouldShowOnboarding) {
            OnboardingScreen(
                settingsViewModel = settingsVM,
                onComplete = {
                    Log.d("OnboardingDebug", "UI onboarding complete callback fired")
                    settingsVM.markOnboardingSeen()
                }
            )
            return@EthioBalanceTheme
        }
        val snackbarHostState = remember { SnackbarHostState() }

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
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
                            painter = painterResource(id = com.ethiobalance.app.R.drawable.app_icon),
                            contentDescription = stringResource(R.string.app_name),
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                        Spacer(modifier = Modifier.width(10.dp))
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
                        val isSyncing by homeVM.isSyncing.collectAsStateWithLifecycle()

                        LaunchedEffect(Unit) {
                            homeVM.syncEvent.collect { message ->
                                snackbarHostState.showSnackbar(message)
                            }
                        }

                        HomeScreen(
                            userName = userName,
                            language = language,
                            totalIncome = totalIncome,
                            totalExpense = totalExpense,
                            packages = packages,
                            transactions = transactions,
                            bankBalances = bankBalances,
                            isSyncing = isSyncing,
                            onSync = { homeVM.triggerManualSync() },
                            onViewAllTransactions = { currentRoute = "transactions" }
                        )
                    }

                    "telecom" -> {
                        val packages by telecomVM.packages.collectAsStateWithLifecycle()
                        val isSyncing by telecomVM.isSyncing.collectAsStateWithLifecycle()
                        val syncError by telecomVM.syncError.collectAsStateWithLifecycle()
                        val syncWarning by telecomVM.syncWarning.collectAsStateWithLifecycle()
                        val syncSuccess by telecomVM.syncSuccess.collectAsStateWithLifecycle()

                        LaunchedEffect(syncSuccess) {
                            syncSuccess?.let { message ->
                                snackbarHostState.showSnackbar(message)
                            }
                        }

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
                        val transactions by transactionVM.filteredTransactions.collectAsStateWithLifecycle()
                        val totalIncome by transactionVM.totalIncome.collectAsStateWithLifecycle()
                        val totalExpense by transactionVM.totalExpense.collectAsStateWithLifecycle()
                        val uniqueSources by transactionVM.uniqueSources.collectAsStateWithLifecycle()
                        val timeFilter by transactionVM.timeFilter.collectAsStateWithLifecycle()
                        val sourceFilter by transactionVM.sourceFilter.collectAsStateWithLifecycle()
                        val typeFilter by transactionVM.typeFilter.collectAsStateWithLifecycle()
                        val categoryFilter by transactionVM.categoryFilter.collectAsStateWithLifecycle()
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
                            typeFilter = typeFilter,
                            categoryFilter = categoryFilter,
                            searchQuery = searchQuery,
                            _isScanningHistory = isScanning,
                            customStartMs = customStartMs,
                            customEndMs = customEndMs,
                            onTimeFilterChange = { transactionVM.setTimeFilter(it) },
                            onSourceFilterChange = { transactionVM.setSourceFilter(it) },
                            onTypeFilterChange = { transactionVM.setTypeFilter(it) },
                            onCategoryFilterChange = { transactionVM.setCategoryFilter(it) },
                            onSearchChange = { transactionVM.setSearchQuery(it) },
                            onCustomRangeChange = { start, end -> transactionVM.setCustomRange(start, end) },
                            onExportCsv = { transactionVM.exportToCsv(context) },
                            _onScanAll = { transactionVM.scanSmsHistory() },
                            onAddManualTransaction = { type, source, amount, cat, party, ref ->
                                transactionVM.addManualTransaction(type, source, amount, cat, party, ref)
                            }
                        )
                    }

                    "settings" -> {
                        val userName by settingsVM.userName.collectAsStateWithLifecycle()
                        val userPhone by settingsVM.userPhone.collectAsStateWithLifecycle()
                        val userAvatar by settingsVM.userAvatar.collectAsStateWithLifecycle()
                        val transactionSources by settingsVM.transactionSources.collectAsStateWithLifecycle()
                        val isBiometricEnabled by settingsVM.isBiometricEnabled.collectAsStateWithLifecycle()
                        val isPinEnabled by settingsVM.isPinEnabled.collectAsStateWithLifecycle()
                        val isAutoSyncEnabled by settingsVM.isAutoSyncEnabled.collectAsStateWithLifecycle()

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
                            onUpdateSource = { settingsVM.updateTransactionSource(it) },
                            onToggleSource = { settingsVM.toggleTransactionSource(it) },
                            onRemoveSource = { settingsVM.removeTransactionSource(it) },
                            isBiometricEnabled = isBiometricEnabled,
                            isPinEnabled = isPinEnabled,
                            isAutoSyncEnabled = isAutoSyncEnabled,
                            onToggleBiometric = { settingsVM.setBiometricEnabled(it) },
                            onTogglePin = { settingsVM.setPinEnabled(it) },
                            onToggleAutoSync = { settingsVM.setAutoSyncEnabled(it) },
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
