package com.ethiobalance.app.domain.usecase

import com.ethiobalance.app.AppConstants
import com.ethiobalance.app.data.TransactionEntity
import com.ethiobalance.app.data.TransactionSourceEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FormatTransactionUseCaseTest {

    private val useCase = FormatTransactionUseCase()

    private val cbeSource = TransactionSourceEntity(
        abbreviation = "CBE",
        name = "Commercial Bank of Ethiopia",
        ussd = "*889#",
        senderId = "847,CBE"
    )

    private val telebirrSource = TransactionSourceEntity(
        abbreviation = "TELEBIRR",
        name = "Telebirr",
        ussd = "*127#",
        senderId = "127,Telebirr"
    )

    private fun transaction(
        id: String,
        source: String,
        timestamp: Long,
        type: String = "EXPENSE",
        amount: Double = 100.0,
        category: String = "TRANSFER",
        reference: String? = "REF-$id",
        partyName: String? = "Abebe Kebede",
        transactionSubType: String? = "bank_transfer"
    ) = TransactionEntity(
        id = id,
        type = type,
        amount = amount,
        category = category,
        source = source,
        timestamp = timestamp,
        reference = reference,
        partyName = partyName,
        transactionSubType = transactionSubType
    )

    @Test
    fun invoke_keepsOnlyConfiguredFinancialSourcesAndExcludesAirtime() {
        val now = System.currentTimeMillis()
        val transactions = listOf(
            transaction("cbe", "CBE", now),
            transaction("telebirr", "TELEBIRR", now),
            transaction("airtime", AppConstants.SOURCE_AIRTIME, now),
            transaction("awash", "AWASH", now)
        )

        val result = useCase(
            transactions = transactions,
            timeFilter = "all",
            sourceFilter = null,
            searchQuery = "",
            configuredSources = listOf(cbeSource, telebirrSource)
        )

        assertEquals(listOf("cbe", "telebirr"), result.map { it.id })
    }

    @Test
    fun invoke_appliesSourceFilterIgnoringCase() {
        val now = System.currentTimeMillis()
        val transactions = listOf(
            transaction("cbe", "CBE", now),
            transaction("telebirr", "TELEBIRR", now)
        )

        val result = useCase(
            transactions = transactions,
            timeFilter = "all",
            sourceFilter = "telebirr",
            searchQuery = "",
            configuredSources = listOf(cbeSource, telebirrSource)
        )

        assertEquals(listOf("telebirr"), result.map { it.id })
    }

    @Test
    fun invoke_appliesCustomDateRangeInclusively() {
        val transactions = listOf(
            transaction("before", "CBE", timestamp = 999L),
            transaction("start", "CBE", timestamp = 1_000L),
            transaction("middle", "CBE", timestamp = 1_500L),
            transaction("end", "CBE", timestamp = 2_000L),
            transaction("after", "CBE", timestamp = 2_001L)
        )

        val result = useCase(
            transactions = transactions,
            timeFilter = "custom",
            sourceFilter = null,
            searchQuery = "",
            configuredSources = listOf(cbeSource),
            customStartMs = 1_000L,
            customEndMs = 2_000L
        )

        assertEquals(listOf("start", "middle", "end"), result.map { it.id })
    }

    @Test
    fun invoke_appliesRelativeTimeFilters() {
        val now = System.currentTimeMillis()
        val fortyDaysAgo = now - (40L * AppConstants.MILLISECONDS_PER_DAY)
        val transactions = listOf(
            transaction("recent", "CBE", now),
            transaction("old", "CBE", fortyDaysAgo)
        )

        val today = useCase(transactions, "today", null, "", listOf(cbeSource))
        val thisWeek = useCase(transactions, "thisWeek", null, "", listOf(cbeSource))
        val thisMonth = useCase(transactions, "thisMonth", null, "", listOf(cbeSource))

        assertEquals(listOf("recent"), today.map { it.id })
        assertEquals(listOf("recent"), thisWeek.map { it.id })
        assertEquals(listOf("recent"), thisMonth.map { it.id })
    }

    @Test
    fun invoke_searchesAcrossVisibleTransactionFields() {
        val now = System.currentTimeMillis()
        val transactions = listOf(
            transaction(
                id = "party",
                source = "CBE",
                timestamp = now,
                partyName = "Mekdes Alemu"
            ),
            transaction(
                id = "reference",
                source = "CBE",
                timestamp = now,
                reference = "TXN-99881"
            ),
            transaction(
                id = "subtype",
                source = "CBE",
                timestamp = now,
                transactionSubType = "school_fee"
            ),
            transaction(
                id = "amount",
                source = "CBE",
                timestamp = now,
                amount = 450.50
            ),
            transaction(
                id = "miss",
                source = "CBE",
                timestamp = now,
                partyName = "Other Person",
                reference = "OTHER",
                transactionSubType = "utility"
            )
        )

        val party = useCase(transactions, "all", null, "mekdes", listOf(cbeSource))
        val reference = useCase(transactions, "all", null, "99881", listOf(cbeSource))
        val subtype = useCase(transactions, "all", null, "school", listOf(cbeSource))
        val amount = useCase(transactions, "all", null, "450.5", listOf(cbeSource))

        assertEquals(listOf("party"), party.map { it.id })
        assertEquals(listOf("reference"), reference.map { it.id })
        assertEquals(listOf("subtype"), subtype.map { it.id })
        assertEquals(listOf("amount"), amount.map { it.id })
    }

    @Test
    fun invoke_searchesSourceCategoryAndType() {
        val now = System.currentTimeMillis()
        val transactions = listOf(
            transaction("source", "TELEBIRR", now),
            transaction("category", "CBE", now, category = "SCHOOL_FEE"),
            transaction("type", "CBE", now, type = "INCOME")
        )

        assertTrue(useCase(transactions, "all", null, "telebirr", listOf(cbeSource, telebirrSource)).any { it.id == "source" })
        assertTrue(useCase(transactions, "all", null, "school", listOf(cbeSource, telebirrSource)).any { it.id == "category" })
        assertTrue(useCase(transactions, "all", null, "income", listOf(cbeSource, telebirrSource)).any { it.id == "type" })
    }
}
