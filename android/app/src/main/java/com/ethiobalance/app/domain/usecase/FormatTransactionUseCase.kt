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
        customEndMs: Long? = null,
        typeFilter: String = "ALL",
        categoryFilter: String = "ALL"
    ): List<TransactionEntity> {
        val enabledResolved = configuredSources.map {
            it.abbreviation.lowercase()
        }.toSet()
        
        var filtered = transactions.filter {
            val resolved = it.source.lowercase()
            it.id.startsWith("tx-manual-") || (resolved != AppConstants.SOURCE_AIRTIME.lowercase() && enabledResolved.contains(resolved))
        }

        val now = System.currentTimeMillis()
        val startOfToday = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        // React web-style relative dates
        val sevenDaysAgo = now - 7L * AppConstants.MILLISECONDS_PER_DAY
        val thirtyDaysAgo = now - 30L * AppConstants.MILLISECONDS_PER_DAY
        val oneYearAgo = now - 365L * AppConstants.MILLISECONDS_PER_DAY

        filtered = when (timeFilter) {
            "today"      -> filtered.filter { it.timestamp >= startOfToday }
            "thisWeek", "weekly"   -> filtered.filter { it.timestamp >= sevenDaysAgo }
            "thisMonth", "monthly"  -> filtered.filter { it.timestamp >= thirtyDaysAgo }
            "thisYear", "yearly"   -> filtered.filter { it.timestamp >= oneYearAgo }
            "custom"     -> {
                val start = customStartMs ?: 0L
                val end = customEndMs ?: Long.MAX_VALUE
                filtered.filter { it.timestamp in start..end }
            }
            else -> filtered
        }

        if (sourceFilter != null && !sourceFilter.equals("ALL", ignoreCase = true)) {
            filtered = filtered.filter {
                it.source.equals(sourceFilter, ignoreCase = true)
            }
        }

        if (!typeFilter.equals("ALL", ignoreCase = true)) {
            filtered = filtered.filter {
                it.type.equals(typeFilter, ignoreCase = true)
            }
        }

        if (!categoryFilter.equals("ALL", ignoreCase = true)) {
            filtered = filtered.filter {
                it.category.equals(categoryFilter, ignoreCase = true)
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
