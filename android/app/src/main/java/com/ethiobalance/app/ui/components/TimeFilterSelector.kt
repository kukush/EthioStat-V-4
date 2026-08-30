package com.ethiobalance.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ethiobalance.app.AppConstants
import com.ethiobalance.app.ui.Translations
import com.ethiobalance.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeFilterSelector(
    language: String,
    selectedFilter: String,
    onFilterSelected: (String) -> Unit,
    customStartMs: Long?,
    customEndMs: Long?,
    onCustomRangeChange: (Long?, Long?) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDatePicker by remember { mutableStateOf(false) }

    val filters = listOf(
        "allTime" to "allTime",
        "today" to "today",
        "thisWeek" to "thisWeek",
        "thisMonth" to "thisMonth",
        "yearly" to "yearly"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF0F172A).copy(alpha = 0.6f), RoundedCornerShape(12.dp))
            .border(BorderStroke(1.dp, Slate800), RoundedCornerShape(12.dp))
            .padding(4.dp)
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Predefined filter tabs
        filters.forEach { (key, translateKey) ->
            val isSelected = selectedFilter == key
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onFilterSelected(key) }
                    .background(if (isSelected) Slate700 else Color.Transparent)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = Translations.t(language, translateKey).takeIf { it.isNotEmpty() } ?: translateKey,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) Color.White else Slate400
                )
            }
        }

        // Custom Filter Tab
        val isCustomSelected = selectedFilter == "custom"
        val customPillText = if (isCustomSelected && customStartMs != null && customEndMs != null) {
            val df = SimpleDateFormat("MMM d", Locale.US)
            val displayEndMs = customEndMs - (24 * 60 * 60 * 1000L - 1)
            "${df.format(Date(customStartMs))}–${df.format(Date(displayEndMs))}".uppercase()
        } else {
            Translations.t(language, "custom")
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable {
                    if (isCustomSelected) {
                        showDatePicker = true
                    } else {
                        onFilterSelected("custom")
                        showDatePicker = true
                    }
                }
                .background(if (isCustomSelected) Slate700 else Color.Transparent)
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                    tint = if (isCustomSelected) Color.White else Slate400
                )
                Text(
                    text = customPillText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isCustomSelected) Color.White else Slate400
                )
            }
        }
    }

    if (showDatePicker) {
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
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val startMs = dateRangePickerState.selectedStartDateMillis
                        val endMs = dateRangePickerState.selectedEndDateMillis
                        if (startMs != null && endMs != null) {
                            val endOfDay = endMs + (24 * 60 * 60 * 1000L - 1)
                            onCustomRangeChange(startMs, endOfDay)
                        }
                        showDatePicker = false
                    },
                    enabled = dateRangePickerState.selectedStartDateMillis != null && dateRangePickerState.selectedEndDateMillis != null
                ) {
                    Text(Translations.t(language, "done").uppercase())
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
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
}
