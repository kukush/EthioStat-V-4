package com.ethiobalance.app.domain.usecase

import com.ethiobalance.app.data.TransactionEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class GetFinancialSummaryUseCaseTest {

    private val useCase = GetFinancialSummaryUseCase()

    private fun transaction(id: String, type: String, amount: Double) = TransactionEntity(
        id = id,
        type = type,
        amount = amount,
        category = "TRANSFER",
        source = "CBE",
        timestamp = 1_000L,
        reference = null,
        partyName = null,
        transactionSubType = null
    )

    @Test
    fun invoke_sumsIncomeAndExpenseSeparately() {
        val summary = useCase(
            listOf(
                transaction("income-1", "INCOME", 1_000.0),
                transaction("income-2", "INCOME", 250.5),
                transaction("expense-1", "EXPENSE", 300.0),
                transaction("ignored", "TRANSFER", 999.0)
            )
        )

        assertEquals(1_250.5, summary.totalIncome, 0.0)
        assertEquals(300.0, summary.totalExpense, 0.0)
    }

    @Test
    fun invoke_returnsZeroesForEmptyList() {
        val summary = useCase(emptyList())

        assertEquals(0.0, summary.totalIncome, 0.0)
        assertEquals(0.0, summary.totalExpense, 0.0)
    }
}
