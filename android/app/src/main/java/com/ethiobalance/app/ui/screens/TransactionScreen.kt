package com.ethiobalance.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ethiobalance.app.data.TransactionEntity
import com.ethiobalance.app.ui.Translations
import com.ethiobalance.app.ui.components.SummaryCard
import com.ethiobalance.app.ui.components.TransactionItem
import com.ethiobalance.app.ui.theme.*

@Composable
fun TransactionScreen(
    language: String,
    transactions: List<TransactionEntity>,
    totalIncome: Double,
    totalExpense: Double,
    uniqueSources: List<String>,
    timeFilter: String,
    sourceFilter: String?,
    searchQuery: String,
    onTimeFilterChange: (String) -> Unit,
    onSourceFilterChange: (String?) -> Unit,
    onSearchChange: (String) -> Unit,
    onExportCsv: () -> Unit,
    onScanAll: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        // Fixed header
        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = Translations.t(language, "history"),
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Black,
                    color = Slate900
                )
                Row {
                    IconButton(onClick = onScanAll) {
                        Icon(Icons.Default.Refresh, contentDescription = "Scan SMS", tint = Blue600)
                    }
                    IconButton(onClick = onExportCsv) {
                        Icon(Icons.Default.FileDownload, contentDescription = "Export", tint = Blue600)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                placeholder = { Text("Search transactions...", fontSize = 14.sp) },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = Slate400, modifier = Modifier.size(20.dp)) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchChange("") }) {
                            Icon(Icons.Default.Close, null, tint = Slate400, modifier = Modifier.size(18.dp))
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = Slate100,
                    focusedBorderColor = Blue600
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Time filter pills
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("allTime", "today", "thisWeek", "thisMonth").forEach { filter ->
                    FilterChip(
                        selected = timeFilter == filter,
                        onClick = { onTimeFilterChange(filter) },
                        label = {
                            Text(
                                Translations.t(language, filter),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Slate900,
                            selectedLabelColor = Color.White,
                            containerColor = Color.White,
                            labelColor = Slate600
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = Slate100,
                            selectedBorderColor = Slate900,
                            enabled = true,
                            selected = timeFilter == filter
                        )
                    )
                }
            }

            // Source filter pills
            if (uniqueSources.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = sourceFilter == null,
                        onClick = { onSourceFilterChange(null) },
                        label = { Text("All", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        shape = RoundedCornerShape(16.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Blue600,
                            selectedLabelColor = Color.White,
                            containerColor = Color.White,
                            labelColor = Slate600
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = Slate100,
                            selectedBorderColor = Blue600,
                            enabled = true,
                            selected = sourceFilter == null
                        )
                    )
                    uniqueSources.forEach { source ->
                        FilterChip(
                            selected = sourceFilter == source,
                            onClick = { onSourceFilterChange(source) },
                            label = { Text(source, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            shape = RoundedCornerShape(16.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Blue600,
                                selectedLabelColor = Color.White,
                                containerColor = Color.White,
                                labelColor = Slate600
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                borderColor = Slate100,
                                selectedBorderColor = Blue600,
                                enabled = true,
                                selected = sourceFilter == source
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Summary cards
            Text(
                Translations.t(language, "overallSummary").uppercase(),
                fontSize = 10.sp, fontWeight = FontWeight.Bold,
                color = Slate400, letterSpacing = 2.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SummaryCard(
                    label = Translations.t(language, "income"),
                    amount = totalIncome,
                    isIncome = true,
                    modifier = Modifier.weight(1f)
                )
                SummaryCard(
                    label = Translations.t(language, "expense"),
                    amount = totalExpense,
                    isIncome = false,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Scrollable transaction list
        if (transactions.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(48.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Receipt, null, tint = Slate300, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(8.dp))
                    Text("No transactions found", fontSize = 14.sp, color = Slate400)
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 0.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(transactions, key = { it.id }) { tx ->
                    TransactionItem(transaction = tx)
                }
                item { Spacer(Modifier.height(100.dp)) }
            }
        }
    }
}
