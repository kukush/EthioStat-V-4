package com.ethiobalance.app.domain.usecase

import com.ethiobalance.app.AppConstants
import com.ethiobalance.app.data.TransactionEntity
import com.ethiobalance.app.data.TransactionSourceEntity
import java.util.Calendar
import javax.inject.Inject

class FormatTransactionUseCase @Inject constructor() {

    operator fun invoke(
        transactions: List<TransactionEntity>,
        timeFilter: String,
        sourceFilter: String?,
        searchQuery: String,
        configuredSources: List<TransactionSourceEntity>,
        customStartMs: Long? = null,
        customEndMs: Long? = null
    ): List<TransactionEntity> {
        val enabledResolved = configuredSources.map {
            it.abbreviation.lowercase()
        }.toSet()
        
        var filtered = transactions.filter {
            val resolved = it.source.lowercase()
            resolved != AppConstants.SOURCE_AIRTIME.lowercase() && enabledResolved.contains(resolved)
        }

        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance()
        val startOfToday = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val startOfWeek = Calendar.getInstance().apply {
            timeInMillis = now
            val currentDay = get(Calendar.DAY_OF_WEEK)
            val daysToSubtract = if (firstDayOfWeek == Calendar.SUNDAY) {
                currentDay - Calendar.SUNDAY
            } else {
                // For Monday as first day of week
                if (currentDay == Calendar.SUNDAY) 6 else currentDay - Calendar.MONDAY
            }
            add(Calendar.DAY_OF_MONTH, -daysToSubtract)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val startOfMonth = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        filtered = when (timeFilter) {
            "today"     -> filtered.filter { it.timestamp >= startOfToday }
            "thisWeek"  -> filtered.filter { it.timestamp >= startOfWeek }
            "thisMonth" -> filtered.filter { it.timestamp >= startOfMonth }
            "custom"    -> {
                val start = customStartMs ?: 0L
                val end = customEndMs ?: Long.MAX_VALUE
                filtered.filter { it.timestamp in start..end }
            }
            else -> filtered
        }

        if (sourceFilter != null) {
            filtered = filtered.filter {
                it.source.equals(sourceFilter, ignoreCase = true)
            }
        }

        if (searchQuery.isNotBlank()) {
            val q = searchQuery.lowercase()
            filtered = filtered.filter {
                it.source.lowercase().contains(q) ||
                it.category.lowercase().contains(q) ||
                it.type.lowercase().contains(q) ||
                it.partyName?.lowercase()?.contains(q) == true ||
                it.reference?.lowercase()?.contains(q) == true ||
                it.transactionSubType?.lowercase()?.contains(q) == true ||
                it.amount.toString().contains(q)
            }
        }

        return filtered
    }
}
