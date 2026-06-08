package com.ethiobalance.app.domain.usecase

import com.ethiobalance.app.data.TransactionEntity
import javax.inject.Inject

class GetFinancialSummaryUseCase @Inject constructor() {

    operator fun invoke(transactions: List<TransactionEntity>): FinancialSummary {
        val income = transactions.filter { it.type == "INCOME" }.sumOf { it.amount }
        val expense = transactions.filter { it.type == "EXPENSE" }.sumOf { it.amount }
        return FinancialSummary(totalIncome = income, totalExpense = expense)
    }

    data class FinancialSummary(
        val totalIncome: Double,
        val totalExpense: Double
    )
}
