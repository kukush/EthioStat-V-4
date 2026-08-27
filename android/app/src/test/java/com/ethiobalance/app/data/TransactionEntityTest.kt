package com.ethiobalance.app.data

import org.junit.Assert.assertEquals
import org.junit.Test

class TransactionEntityTest {

    @Test
    fun createTransactionEntity() {
        val transaction = TransactionEntity(
            id = "txn_1",
            type = "INCOME",
            amount = 1000.0,
            category = "SALARY",
            source = "TELEBIRR",
            timestamp = System.currentTimeMillis(),
            reference = "REF123",
            partyName = "Employer",
            transactionSubType = "Direct Deposit"
        )

        assertEquals("txn_1", transaction.id)
        assertEquals("INCOME", transaction.type)
        assertEquals(1000.0, transaction.amount, 0.0)
        assertEquals("SALARY", transaction.category)
        assertEquals("TELEBIRR", transaction.source)
        assert(transaction.timestamp > 0L)
        assertEquals("REF123", transaction.reference)
        assertEquals("Employer", transaction.partyName)
        assertEquals("Direct Deposit", transaction.transactionSubType)
    }

    @Test
    fun transactionEntityWithNullableFields() {
        val transaction = TransactionEntity(
            id = "txn_2",
            type = "EXPENSE",
            amount = 50.0,
            category = "PURCHASE",
            source = "SMS",
            timestamp = System.currentTimeMillis(),
            reference = null,
            partyName = null,
            transactionSubType = null
        )

        assertEquals("txn_2", transaction.id)
        assertEquals("EXPENSE", transaction.type)
        assertEquals(50.0, transaction.amount, 0.0)
        assertEquals("PURCHASE", transaction.category)
        assertEquals("SMS", transaction.source)
        assert(transaction.timestamp > 0L)
        assertEquals(null, transaction.reference)
        assertEquals(null, transaction.partyName)
        assertEquals(null, transaction.transactionSubType)
    }

    @Test
    fun transactionEntityEquality() {
        val timestamp = System.currentTimeMillis()
        val transaction1 = TransactionEntity(id = "txn_3", type = "INCOME", amount = 200.0, category = "GIFT", source = "USSD", timestamp = timestamp, reference = "GIFTREF", partyName = "Friend", transactionSubType = null)
        val transaction2 = TransactionEntity(id = "txn_3", type = "INCOME", amount = 200.0, category = "GIFT", source = "USSD", timestamp = timestamp, reference = "GIFTREF", partyName = "Friend", transactionSubType = null)

        assertEquals(transaction1, transaction2)
    }

    @Test
    fun transactionEntityInequality() {
        val timestamp = System.currentTimeMillis()
        val transaction1 = TransactionEntity(id = "txn_4", type = "EXPENSE", amount = 75.0, category = "FOOD", source = "TELEBIRR", timestamp = timestamp, reference = null, partyName = null, transactionSubType = "Restaurant")
        val transaction2 = TransactionEntity(id = "txn_5", type = "EXPENSE", amount = 75.0, category = "FOOD", source = "TELEBIRR", timestamp = timestamp, reference = null, partyName = null, transactionSubType = "Restaurant") // Different ID

        assert(transaction1 != transaction2)
    }
}
