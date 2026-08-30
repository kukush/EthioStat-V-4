package com.ethiobalance.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ethiobalance.app.AppConstants
import com.ethiobalance.app.data.TransactionEntity
import com.ethiobalance.app.ui.Translations
import com.ethiobalance.app.ui.components.*
import com.ethiobalance.app.ui.theme.*
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionScreen(
    language: String,
    transactions: List<TransactionEntity>,
    totalIncome: Double,
    totalExpense: Double,
    uniqueSources: List<Pair<String, String>>,
    timeFilter: String,
    sourceFilter: String?,
    typeFilter: String = "ALL",
    categoryFilter: String = "ALL",
    searchQuery: String,
    _isScanningHistory: Boolean = false,
    customStartMs: Long? = null,
    customEndMs: Long? = null,
    onTimeFilterChange: (String) -> Unit,
    onSourceFilterChange: (String?) -> Unit,
    onTypeFilterChange: (String) -> Unit = {},
    onCategoryFilterChange: (String) -> Unit = {},
    onSearchChange: (String) -> Unit,
    onCustomRangeChange: (Long?, Long?) -> Unit = { _, _ -> },
    onExportCsv: () -> Unit,
    _onScanAll: () -> Unit,
    onAddManualTransaction: (type: String, source: String, amount: Double, category: String, partyName: String, reference: String) -> Unit = { _, _, _, _, _, _ -> }
) {
    var showExportModal by remember { mutableStateOf(false) }
    var showAddManualDialog by remember { mutableStateOf(false) }
    var showChart by remember { mutableStateOf(false) }
    
    val netBalance = totalIncome - totalExpense
    val listState = rememberLazyListState()

    var showDateRangePicker by remember { mutableStateOf(false) }
    var showAmounts by remember { mutableStateOf(true) }
    
    val lastActivity = remember(transactions, language) {
        transactions.firstOrNull()?.let {
            try {
                Translations.formatDate(language, it.timestamp)
            } catch (e: Exception) {
                null
            }
        }
    }
    
    val showStickyBar by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 0 }
    }
    
    val fmt = remember {
        NumberFormat.getNumberInstance(Locale.US).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (showExportModal) {
            ExportPreviewDialog(
                isOpen = true,
                onClose = { showExportModal = false },
                onConfirm = { 
                    onExportCsv()
                    showExportModal = false
                },
                transactions = transactions,
                totalIncome = totalIncome,
                totalExpense = totalExpense,
                userName = "Abebe Bikila",
                userPhone = "0911234567"
            )
        }

        if (showAddManualDialog) {
            AddTransactionDialog(
                isOpen = true,
                onClose = { showAddManualDialog = false },
                onConfirm = onAddManualTransaction,
                language = language,
                uniqueSources = uniqueSources
            )
        }
        
        // ── Main Scrollable List ──────────────────────────────────────────────
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 120.dp)
        ) {
            // ── HEADER ITEM ──────────────────────────────────────────────────
            item {
                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                    Spacer(modifier = Modifier.height(16.dp))

                    // ── TOP BAR WITH ACTIONS ──────────────────────────────────
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = Translations.t(language, "transactionHistory"),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Chart Analysis Toggle
                            IconButton(
                                onClick = { showChart = !showChart },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(if (showChart) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant),
                                colors = IconButtonDefaults.iconButtonColors(
                                    contentColor = if (showChart) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            ) {
                                Icon(Icons.Default.Equalizer, contentDescription = "Chart Analysis", modifier = Modifier.size(18.dp))
                            }

                            // Add Manual Transaction
                            IconButton(
                                onClick = { showAddManualDialog = true },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                colors = IconButtonDefaults.iconButtonColors(
                                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Add Transaction", modifier = Modifier.size(18.dp))
                            }

                            // Export CSV
                            IconButton(
                                onClick = { showExportModal = true },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                colors = IconButtonDefaults.iconButtonColors(
                                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            ) {
                                Icon(Icons.Default.Download, contentDescription = "Export CSV", modifier = Modifier.size(18.dp))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // ── SEARCH BAR ────────────────────────────────────────────
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = onSearchChange,
                        placeholder = {
                            Text(Translations.t(language, "searchTransactions"), fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { onSearchChange("") }) {
                                    Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp),
                        shape = RoundedCornerShape(16.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // ── DATE FILTER CHIPS ─────────────────────────────────────
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            "allTime" to "allTime",
                            "today" to "today",
                            "thisWeek" to "thisWeek",
                            "thisMonth" to "thisMonth",
                            "yearly" to "yearly"
                        ).forEach { (translationKey, filterVal) ->
                            val isSelected = timeFilter == filterVal
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    .clickable { onTimeFilterChange(filterVal) }
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = Translations.t(language, translationKey),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Custom Date Range Picker Pill
                        val isCustomSelected = timeFilter == "custom"
                        val customPillText = if (isCustomSelected && customStartMs != null && customEndMs != null) {
                            val df = SimpleDateFormat("MMM d", Locale.US)
                            val displayEndMs = customEndMs - (24 * 60 * 60 * 1000L - 1)
                            "${df.format(Date(customStartMs))}–${df.format(Date(displayEndMs))}".uppercase()
                        } else {
                            Translations.t(language, "custom")
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isCustomSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .clickable { showDateRangePicker = true }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(
                                    Icons.Default.DateRange,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = if (isCustomSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = customPillText,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isCustomSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // DateRangePicker Dialog
                    if (showDateRangePicker) {
                        val todayEthiopia = Calendar.getInstance(AppConstants.ETHIOPIA_TIMEZONE)
                        val utcCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                        utcCalendar.timeInMillis = todayEthiopia.timeInMillis
                        utcCalendar.set(Calendar.HOUR_OF_DAY, 23)
                        utcCalendar.set(Calendar.MINUTE, 59)
                        utcCalendar.set(Calendar.SECOND, 59)
                        utcCalendar.set(Calendar.MILLISECOND, 999)
                        val endOfTodayUtc = utcCalendar.timeInMillis
                        val dateRangePickerState = rememberDateRangePickerState(
                            selectableDates = object : SelectableDates {
                                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                                    return utcTimeMillis <= endOfTodayUtc
                                }
                            }
                        )
                        DatePickerDialog(
                            onDismissRequest = { showDateRangePicker = false },
                            confirmButton = {
                                TextButton(
                                    onClick = {
                                        val startMs = dateRangePickerState.selectedStartDateMillis
                                        val endMs = dateRangePickerState.selectedEndDateMillis
                                        if (startMs != null && endMs != null) {
                                            val endOfDay = endMs + (24 * 60 * 60 * 1000L - 1)
                                            onCustomRangeChange(startMs, endOfDay)
                                        }
                                        showDateRangePicker = false
                                    },
                                    enabled = dateRangePickerState.selectedStartDateMillis != null && dateRangePickerState.selectedEndDateMillis != null
                                ) {
                                    Text(Translations.t(language, "done").uppercase())
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showDateRangePicker = false }) {
                                    Text(Translations.t(language, "cancel").uppercase())
                                }
                            }
                        ) {
                            DateRangePicker(
                                state = dateRangePickerState,
                                title = {
                                    Text(
                                        text = Translations.t(language, "selectDateRange"),
                                        modifier = Modifier.padding(start = 24.dp, top = 16.dp),
                                        style = MaterialTheme.typography.titleLarge
                                    )
                                },
                                modifier = Modifier.heightIn(max = 500.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // ── WEB-STYLE FILTER DROPDOWNS ────────────────────────────
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 1. Sources Dropdown
                        var sourceExpanded by remember { mutableStateOf(false) }
                        val sourceLabel = if (sourceFilter == null) {
                            Translations.t(language, "allSources")
                        } else {
                            uniqueSources.find { it.second == sourceFilter }?.first?.uppercase() ?: sourceFilter.uppercase()
                        }

                        Box {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                    .clickable { sourceExpanded = true }
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = sourceLabel,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            DropdownMenu(
                                expanded = sourceExpanded,
                                onDismissRequest = { sourceExpanded = false },
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.surface)
                                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                            ) {
                                // "All Sources" Option
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = Translations.t(language, "allSources"),
                                            fontSize = 13.sp,
                                            fontWeight = if (sourceFilter == null) FontWeight.Bold else FontWeight.Normal,
                                            color = if (sourceFilter == null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                    },
                                    onClick = {
                                        onSourceFilterChange(null)
                                        sourceExpanded = false
                                    }
                                )
                                // Each discovered source
                                uniqueSources.forEach { (abbreviation, name) ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = abbreviation.uppercase(),
                                                fontSize = 13.sp,
                                                fontWeight = if (sourceFilter == name) FontWeight.Bold else FontWeight.Normal,
                                                color = if (sourceFilter == name) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                            )
                                        },
                                        onClick = {
                                            onSourceFilterChange(name)
                                            sourceExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        // 2. Types Dropdown
                        var typeExpanded by remember { mutableStateOf(false) }
                        val typeLabel = when (typeFilter) {
                            "INCOME" -> Translations.t(language, "incomesOnly")
                            "EXPENSE" -> Translations.t(language, "expensesOnly")
                            "TRANSFER" -> Translations.t(language, "transfersOnly")
                            else -> Translations.t(language, "allTypes")
                        }

                        Box {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                    .clickable { typeExpanded = true }
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = typeLabel,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            DropdownMenu(
                                expanded = typeExpanded,
                                onDismissRequest = { typeExpanded = false },
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.surface)
                                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                            ) {
                                listOf(
                                    "ALL" to "allTypes",
                                    "INCOME" to "incomesOnly",
                                    "EXPENSE" to "expensesOnly",
                                    "TRANSFER" to "transfersOnly"
                                ).forEach { (t, key) ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = Translations.t(language, key),
                                                fontSize = 13.sp,
                                                fontWeight = if (typeFilter == t) FontWeight.Bold else FontWeight.Normal,
                                                color = if (typeFilter == t) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                            )
                                        },
                                        onClick = {
                                            onTypeFilterChange(t)
                                            typeExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        // 3. Categories Dropdown
                        var categoryExpanded by remember { mutableStateOf(false) }
                        val categoryLabel = when (categoryFilter) {
                            "ALL" -> Translations.t(language, "allCategories")
                            else -> Translations.t(language, categoryFilter.lowercase()).uppercase()
                        }

                        Box {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                    .clickable { categoryExpanded = true }
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = categoryLabel,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            DropdownMenu(
                                expanded = categoryExpanded,
                                onDismissRequest = { categoryExpanded = false },
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.surface)
                                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                            ) {
                                listOf(
                                    "ALL" to "allCategories",
                                    "GENERAL" to "general",
                                    "SALARY" to "salary",
                                    "TELECOM" to "telecom",
                                    "RECHARGE" to "recharge",
                                    "SHOPPING" to "shopping",
                                    "DINING" to "dining",
                                    "UTILITY" to "utility",
                                    "TRANSFER" to "transfer"
                                ).forEach { (cat, key) ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = Translations.t(language, key),
                                                fontSize = 13.sp,
                                                fontWeight = if (categoryFilter == cat) FontWeight.Bold else FontWeight.Normal,
                                                color = if (categoryFilter == cat) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                            )
                                        },
                                        onClick = {
                                            onCategoryFilterChange(cat)
                                            categoryExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // ── SEARCH RESULTS SUMMARY BAR ────────────────────────────
                    val filteredIncome = remember(transactions) {
                        transactions.filter { it.type == "INCOME" }.sumOf { it.amount }
                    }
                    val filteredExpense = remember(transactions) {
                        transactions.filter { it.type == "EXPENSE" }.sumOf { it.amount }
                    }
                    val filteredNet = filteredIncome - filteredExpense

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${transactions.size} items",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "[+${fmt.format(filteredIncome)} -${fmt.format(filteredExpense)}]",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${if (filteredNet >= 0) "+" else ""}${fmt.format(filteredNet)} ETB",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                color = if (filteredNet >= 0) Emerald600 else Rose600
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // ── CHART ANALYSIS CARD ───────────────────────────────────
                    AnimatedVisibility(
                        visible = showChart,
                        enter = fadeIn(tween(250)) + expandVertically(),
                        exit = fadeOut(tween(200)) + shrinkVertically()
                    ) {
                        TransactionChart(transactions = transactions, language = language)
                    }

                    // ── SUMMARY CARD ──────────────────────────────────────────
                    val displayTimeFilter = if (timeFilter == "custom" && customStartMs != null && customEndMs != null) {
                        val df = SimpleDateFormat("MMM d", Locale.US)
                        val displayEndMs = customEndMs - (24 * 60 * 60 * 1000L - 1)
                        "${df.format(Date(customStartMs))} – ${df.format(Date(displayEndMs))}".uppercase()
                    } else {
                        timeFilter
                    }
                    SummaryCard(
                        language = language,
                        netBalance = netBalance,
                        totalIncome = totalIncome,
                        totalExpense = totalExpense,
                        transactionCount = transactions.size,
                        timeFilter = displayTimeFilter,
                        sourceFilter = sourceFilter,
                        lastActivity = lastActivity,
                        showAmounts = showAmounts,
                        onToggleAmounts = { showAmounts = !showAmounts }
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Section Divider
                    Text(
                        text = Translations.t(language, "history").uppercase(),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate400,
                        letterSpacing = 1.5.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            // ── EMPTY STATE OR LIST ───────────────────────────────────────────
            if (transactions.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 16.dp)
                            .clip(RoundedCornerShape(32.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(32.dp))
                            .padding(48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier.size(64.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Payments, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(32.dp))
                            }
                            Spacer(Modifier.height(16.dp))
                            Text("No transactions found", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Text(
                                "Try adjusting your filters or search query",
                                fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            } else {
                items(transactions, key = { it.id }) { tx ->
                    Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) {
                        TransactionItem(transaction = tx)
                    }
                }
            }
        }

        // ── STICKY COMPACT BAR ────────────────────────────────────────────────
        AnimatedVisibility(
            visible = showStickyBar,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(200)),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(8.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Compact date/type filters summary
                    Column {
                        Text(
                            text = "${transactions.size} items filtered",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    // Net balance chip
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (netBalance >= 0) Emerald50 else Rose50)
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = "${if (netBalance >= 0) "+" else ""}${fmt.format(netBalance)} ETB",
                            fontSize = 10.sp, fontWeight = FontWeight.Black,
                            color = if (netBalance >= 0) Emerald600 else Rose600
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TransactionChart(
    transactions: List<TransactionEntity>,
    language: String
) {
    val sourceData = remember(transactions) {
        val groups = transactions.groupBy { it.source.uppercase() }
        groups.map { (source, txs) ->
            val income = txs.filter { it.type == "INCOME" }.sumOf { it.amount }
            val expense = txs.filter { it.type == "EXPENSE" }.sumOf { it.amount }
            source to (income to expense)
        }.filter { it.second.first > 0 || it.second.second > 0 }
         .sortedByDescending { it.second.first + it.second.second }
         .take(5) // Show top 5 sources for high-fidelity clean visual layout
    }

    if (sourceData.isEmpty()) return

    val maxVal = remember(sourceData) {
        val max = sourceData.maxOf { maxOf(it.second.first, it.second.second) }
        if (max == 0.0) 1.0 else max
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = Translations.t(language, "sourceFlow"),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                sourceData.forEach { (source, flows) ->
                    val income = flows.first
                    val expense = flows.second

                    val incomeHeightPct = (income / maxVal).toFloat().coerceIn(0.04f, 1f)
                    val expenseHeightPct = (expense / maxVal).toFloat().coerceIn(0.04f, 1f)

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxHeight(0.85f)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            // Income Bar (Green)
                            Box(
                                modifier = Modifier
                                    .width(14.dp)
                                    .fillMaxHeight(incomeHeightPct)
                                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                    .background(Emerald500)
                            )
                            // Expense Bar (Red)
                            Box(
                                modifier = Modifier
                                    .width(14.dp)
                                    .fillMaxHeight(expenseHeightPct)
                                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                    .background(Rose500)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = source,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            // Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Emerald500))
                Spacer(modifier = Modifier.width(6.dp))
                Text(Translations.t(language, "income"), fontSize = 11.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.width(20.dp))
                Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Rose500))
                Spacer(modifier = Modifier.width(6.dp))
                Text(Translations.t(language, "expense"), fontSize = 11.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionDialog(
    isOpen: Boolean,
    onClose: () -> Unit,
    onConfirm: (type: String, source: String, amount: Double, category: String, partyName: String, reference: String) -> Unit,
    language: String,
    uniqueSources: List<Pair<String, String>>
) {
    if (!isOpen) return

    var amount by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("EXPENSE") }
    var selectedSource by remember { mutableStateOf("CASH") }
    var selectedCategory by remember { mutableStateOf("GENERAL") }
    var partyName by remember { mutableStateOf("") }
    var reference by remember { mutableStateOf("") }

    val categories = listOf("GENERAL", "SALARY", "TELECOM", "RECHARGE", "SHOPPING", "DINING", "UTILITY", "TRANSFER")
    val defaultSources = listOf("CASH", "TELEBIRR", "CBE") + uniqueSources.map { it.first }.filter { it != "CASH" && it != "TELEBIRR" && it != "CBE" }

    AlertDialog(
        onDismissRequest = onClose,
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            tonalElevation = 6.dp,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = Translations.t(language, "addTransactionManually"),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Type Selection Row
                Column {
                    Text(
                        text = Translations.t(language, "type").uppercase(),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("INCOME" to Emerald500, "EXPENSE" to Rose500, "TRANSFER" to Purple500).forEach { (t, color) ->
                            val isSel = selectedType == t
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSel) color.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    .border(1.5.dp, if (isSel) color else Color.Transparent, RoundedCornerShape(12.dp))
                                    .clickable { selectedType = t }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = Translations.t(language, t.lowercase()).uppercase(),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSel) color else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Amount
                OutlinedTextField(
                    value = amount,
                    onValueChange = { if (it.isEmpty() || it.toDoubleOrNull() != null || it.endsWith(".")) amount = it },
                    label = { Text(Translations.t(language, "amount") + " (ETB)") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                // Source Choice Chips
                Column {
                    Text(
                        text = Translations.t(language, "source").uppercase(),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        defaultSources.forEach { s ->
                            val isSel = selectedSource == s
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable { selectedSource = s }
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = s,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Category Choice Chips
                Column {
                    Text(
                        text = Translations.t(language, "category").uppercase(),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        categories.forEach { c ->
                            val isSel = selectedCategory == c
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable { selectedCategory = c }
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = Translations.t(language, c.lowercase()).uppercase(),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Recipient / Sender Name
                OutlinedTextField(
                    value = partyName,
                    onValueChange = { partyName = it },
                    label = { Text(Translations.t(language, "partyName")) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                // Reference ID
                OutlinedTextField(
                    value = reference,
                    onValueChange = { reference = it },
                    label = { Text(Translations.t(language, "reference")) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Spacer(Modifier.height(8.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End)
                ) {
                    TextButton(onClick = onClose) {
                        Text(Translations.t(language, "cancel").uppercase(), fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = {
                            val amtVal = amount.toDoubleOrNull() ?: 0.0
                            if (amtVal > 0.0) {
                                onConfirm(selectedType, selectedSource, amtVal, selectedCategory, partyName, reference)
                                onClose()
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        enabled = amount.toDoubleOrNull() != null && (amount.toDoubleOrNull() ?: 0.0) > 0.0
                    ) {
                        Text(Translations.t(language, "addTransaction").uppercase(), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
