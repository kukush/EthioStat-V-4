package com.ethiobalance.app.domain.usecase

import com.ethiobalance.app.AppConstants
import com.ethiobalance.app.data.TransactionEntity
import com.ethiobalance.app.data.TransactionSourceEntity
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
            AppConstants.resolveSource(it.senderId).lowercase()
        }.toSet()
        
        var filtered = transactions.filter {
            val resolved = AppConstants.resolveSource(it.source).lowercase()
            resolved != AppConstants.SOURCE_AIRTIME.lowercase() && enabledResolved.contains(resolved)
        }

        val now = System.currentTimeMillis()
        filtered = when (timeFilter) {
            "today" -> filtered.filter { now - it.timestamp < 24L * 60 * 60 * 1000 }
            "thisWeek" -> filtered.filter { now - it.timestamp < 7L * 24 * 60 * 60 * 1000 }
            "thisMonth" -> filtered.filter { now - it.timestamp < 30L * 24 * 60 * 60 * 1000 }
            else -> filtered
        }

        if (sourceFilter != null) {
            filtered = filtered.filter {
                AppConstants.resolveSource(it.source).equals(sourceFilter, ignoreCase = true)
            }
        }

        if (searchQuery.isNotBlank()) {
            val q = searchQuery.lowercase()
            filtered = filtered.filter {
                it.source.lowercase().contains(q) ||
                it.category.lowercase().contains(q) ||
                it.id.lowercase().contains(q)
            }
        }

        return filtered
    }
}
