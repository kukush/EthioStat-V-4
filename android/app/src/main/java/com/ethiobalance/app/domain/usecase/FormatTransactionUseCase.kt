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
        configuredSources: List<TransactionSourceEntity>
    ): List<TransactionEntity> {
        val enabledResolved = configuredSources.map {
            it.abbreviation.lowercase()
        }.toSet()
        
        var filtered = transactions.filter {
            val resolved = it.source.lowercase()
            resolved != AppConstants.SOURCE_AIRTIME.lowercase() && enabledResolved.contains(resolved)
        }

        val cal = Calendar.getInstance()
        val startOfToday = cal.apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val startOfWeek = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val startOfMonth = Calendar.getInstance().apply {
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
