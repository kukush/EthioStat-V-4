package com.ethiobalance.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme
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
import com.ethiobalance.app.data.TransactionEntity
import com.ethiobalance.app.ui.Translations
import com.ethiobalance.app.ui.components.SummaryCard
import com.ethiobalance.app.ui.components.TransactionItem
import com.ethiobalance.app.ui.theme.*
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TransactionScreen(
    language: String,
    transactions: List<TransactionEntity>,
    totalIncome: Double,
    totalExpense: Double,
    uniqueSources: List<Pair<String, String>>,
    timeFilter: String,
    sourceFilter: String?,
    searchQuery: String,
    _isScanningHistory: Boolean = false,
    onTimeFilterChange: (String) -> Unit,
    onSourceFilterChange: (String?) -> Unit,
    onSearchChange: (String) -> Unit,
    onExportCsv: () -> Unit,
    _onScanAll: () -> Unit
) {
    var showAmounts by remember { mutableStateOf(true) }
    val netBalance = totalIncome - totalExpense
    val listState = rememberLazyListState()

    // Show the compact sticky bar when the user has scrolled past the header item (index 0)
    val showStickyBar by remember {
        derivedStateOf { listState.firstVisibleItemIndex >= 1 }
    }

    val fmt = NumberFormat.getNumberInstance(Locale.US).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }

    val dateFormat = SimpleDateFormat("MMM d, yyyy HH:mm", Locale.US)
    val lastActivity = if (transactions.isNotEmpty()) {
        try { dateFormat.format(Date(transactions.first().timestamp)) } catch (e: Exception) { "N/A" }
    } else { "N/A" }

    Box(modifier = Modifier.fillMaxSize().statusBarsPadding()) {

        // ── Main Scrollable List ──────────────────────────────────────────────
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 120.dp)
        ) {
            // ── HEADER ITEM (scrolls away) ────────────────────────────────────
            item {
                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                    Spacer(modifier = Modifier.height(16.dp))

                    // Page title + scan button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = Translations.t(language, "transactions")
                                .takeIf { it.isNotEmpty() } ?: "Transactions",
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Black,
                            color = Slate900,
                            letterSpacing = (-1).sp
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Search bar
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = onSearchChange,
                        placeholder = {
                            Text("Search transactions…", fontSize = 14.sp, fontWeight = FontWeight.Medium)
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

                    Spacer(modifier = Modifier.height(12.dp))

                    // Time Period Filters
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            "allTime" to "allTime",
                            "today" to "today",
                            "thisWeek" to "thisWeek",
                            "thisMonth" to "thisMonth"
                        ).forEach { (translationKey, filterVal) ->
                            val isSelected = timeFilter == filterVal
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) Slate900 else Color.White)
                                    .border(1.dp, if (isSelected) Slate900 else Slate100, RoundedCornerShape(12.dp))
                                    .clickable { onTimeFilterChange(filterVal) }
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = Translations.t(language, translationKey)
                                        .takeIf { it.isNotEmpty() } ?: translationKey.uppercase(),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (isSelected) Color.White else Slate400,
                                    letterSpacing = 2.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Source Filter Chips
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // ALL chip
                        SourceChip(
                            label = "ALL",
                            isSelected = sourceFilter == null,
                            onClick = { onSourceFilterChange(null) }
                        )
                        // Per-source chips
                        uniqueSources.forEach { (abbreviation, name) ->
                            SourceChip(
                                label = name.take(8),
                                abbreviation = abbreviation,
                                isSelected = sourceFilter == name,
                                onClick = { onSourceFilterChange(name) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Summary Card
                    SummaryCard(
                        language = language,
                        netBalance = netBalance,
                        totalIncome = totalIncome,
                        totalExpense = totalExpense,
                        transactionCount = transactions.size,
                        timeFilter = timeFilter,
                        sourceFilter = sourceFilter,
                        lastActivity = lastActivity,
                        showAmounts = showAmounts,
                        onToggleAmounts = { showAmounts = !showAmounts }
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Text(
                            Translations.t(language, "history").uppercase(),
                            fontSize = 10.sp, fontWeight = FontWeight.Bold,
                            color = Slate400, letterSpacing = 2.sp
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { onExportCsv() }
                        ) {
                            Text("EXPORT CSV", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Blue600, letterSpacing = 2.sp)
                            Spacer(Modifier.width(4.dp))
                            Icon(Icons.Default.Download, null, tint = Blue600, modifier = Modifier.size(12.dp))
                        }
                    }
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
                            .clip(RoundedCornerShape(40.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(40.dp))
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

        // ── STICKY COMPACT BAR (appears after scrolling past header) ──────────
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
                    // Compact filter pills
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("allTime" to "allTime", "today" to "today", "thisWeek" to "thisWeek", "thisMonth" to "thisMonth").forEach { (key, val_) ->
                            val isSel = timeFilter == val_
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSel) Slate900 else Slate50)
                                    .clickable { onTimeFilterChange(val_) }
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    Translations.t(language, key).takeIf { it.isNotEmpty() } ?: key.uppercase(),
                                    fontSize = 7.sp, fontWeight = FontWeight.Black,
                                    color = if (isSel) Color.White else Slate400,
                                    letterSpacing = 1.sp
                                )
                            }
                        }
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

// ── Reusable source chip component ───────────────────────────────────────────
@Composable
private fun SourceChip(label: String, abbreviation: String = label, isSelected: Boolean, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }.width(64.dp)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(if (isSelected) Slate900 else Color.White)
                .border(
                    if (isSelected) 3.dp else 1.dp,
                    if (isSelected) Slate700 else Slate100,
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = abbreviation.uppercase(),
                fontSize = when {
                    abbreviation.length <= 2 -> 15.sp
                    abbreviation.length <= 3 -> 13.sp
                    else -> 10.sp
                },
                fontWeight = FontWeight.Black,
                color = if (isSelected) Color.White else Slate400
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = label.uppercase(),
            fontSize = 9.sp, fontWeight = FontWeight.Bold,
            color = if (isSelected) Slate900 else Slate400,
            letterSpacing = 1.sp,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
