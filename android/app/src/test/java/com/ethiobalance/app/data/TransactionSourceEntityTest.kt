package com.ethiobalance.app.data

import org.junit.Assert.assertEquals
import org.junit.Test

class TransactionSourceEntityTest {

    @Test
    fun createTransactionSourceEntity() {
        val transactionSource = TransactionSourceEntity(
            abbreviation = "CBE",
            name = "Commercial Bank of Ethiopia",
            ussd = "*889#",
            senderId = "CBE,889",
            isEnabled = true,
            lastUpdated = System.currentTimeMillis()
        )

        assertEquals("CBE", transactionSource.abbreviation)
        assertEquals("Commercial Bank of Ethiopia", transactionSource.name)
        assertEquals("*889#", transactionSource.ussd)
        assertEquals("CBE,889", transactionSource.senderId)
        assertEquals(true, transactionSource.isEnabled)
        assert(transactionSource.lastUpdated > 0L)
    }

    @Test
    fun transactionSourceEntityWithDefaultValues() {
        val transactionSource = TransactionSourceEntity(
            abbreviation = "TB",
            name = "Telebirr",
            ussd = "*127#",
            senderId = "Telebirr"
        )

        assertEquals("TB", transactionSource.abbreviation)
        assertEquals("Telebirr", transactionSource.name)
        assertEquals("*127#", transactionSource.ussd)
        assertEquals("Telebirr", transactionSource.senderId)
        assertEquals(true, transactionSource.isEnabled) // Default value
        assert(transactionSource.lastUpdated > 0L) // Default value
    }

    @Test
    fun transactionSourceEntityEquality() {
        val timestamp = System.currentTimeMillis()
        val transactionSource1 = TransactionSourceEntity(abbreviation = "BOA", name = "Bank of Abyssinia", ussd = "*847#", senderId = "BoA,847", isEnabled = false, lastUpdated = timestamp)
        val transactionSource2 = TransactionSourceEntity(abbreviation = "BOA", name = "Bank of Abyssinia", ussd = "*847#", senderId = "BoA,847", isEnabled = false, lastUpdated = timestamp)

        assertEquals(transactionSource1, transactionSource2)
    }

    @Test
    fun transactionSourceEntityInequality() {
        val timestamp = System.currentTimeMillis()
        val transactionSource1 = TransactionSourceEntity(abbreviation = "AWASH", name = "Awash Bank", ussd = "*787#", senderId = "Awash", isEnabled = true, lastUpdated = timestamp)
        val transactionSource2 = TransactionSourceEntity(abbreviation = "HIBRET", name = "Hibret Bank", ussd = "*812#", senderId = "Hibret", isEnabled = true, lastUpdated = timestamp) // Different abbreviation and other fields

        assert(transactionSource1 != transactionSource2)
    }
}
