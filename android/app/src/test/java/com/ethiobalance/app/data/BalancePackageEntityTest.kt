package com.ethiobalance.app.data

import org.junit.Assert.assertEquals
import org.junit.Test

class BalancePackageEntityTest {

    @Test
    fun createBalancePackageEntity() {
        val balancePackage = BalancePackageEntity(
            id = "test_id_1",
            simId = "sim_1",
            type = "internet",
            subType = "monthly",
            totalAmount = 1024.0,
            remainingAmount = 512.0,
            unit = "MB",
            expiryDate = System.currentTimeMillis() + 86400000L,
            isActive = true,
            source = "SMS",
            lastUpdated = System.currentTimeMillis()
        )

        assertEquals("test_id_1", balancePackage.id)
        assertEquals("sim_1", balancePackage.simId)
        assertEquals("internet", balancePackage.type)
        assertEquals("monthly", balancePackage.subType)
        assertEquals(1024.0, balancePackage.totalAmount, 0.0)
        assertEquals(512.0, balancePackage.remainingAmount, 0.0)
        assertEquals("MB", balancePackage.unit)
        assert(balancePackage.expiryDate > System.currentTimeMillis())
        assertEquals(true, balancePackage.isActive)
        assertEquals("SMS", balancePackage.source)
        assert(balancePackage.lastUpdated > 0L)
    }

    @Test
    fun balancePackageEntityDefaults() {
        val balancePackage = BalancePackageEntity(
            id = "test_id_2",
            simId = "sim_2",
            type = "voice",
            totalAmount = 50.0,
            remainingAmount = 25.0,
            unit = "MIN",
            expiryDate = 0L,
            isActive = false,
            source = "USSD",
            lastUpdated = 0L
        )

        // subType defaults to empty string if not provided
        assertEquals("", balancePackage.subType)

        assertEquals("test_id_2", balancePackage.id)
        assertEquals("sim_2", balancePackage.simId)
        assertEquals("voice", balancePackage.type)
        assertEquals(50.0, balancePackage.totalAmount, 0.0)
        assertEquals(25.0, balancePackage.remainingAmount, 0.0)
        assertEquals("MIN", balancePackage.unit)
        assertEquals(0L, balancePackage.expiryDate)
        assertEquals(false, balancePackage.isActive)
        assertEquals("USSD", balancePackage.source)
        assertEquals(0L, balancePackage.lastUpdated)
    }

    @Test
    fun balancePackageEntityEquality() {
        val balancePackage1 = BalancePackageEntity(
            id = "test_id_3",
            simId = "sim_3",
            type = "sms",
            totalAmount = 200.0,
            remainingAmount = 100.0,
            unit = "SMS",
            expiryDate = 1000L,
            isActive = true,
            source = "SYSTEM",
            lastUpdated = 500L
        )
        val balancePackage2 = BalancePackageEntity(
            id = "test_id_3",
            simId = "sim_3",
            type = "sms",
            totalAmount = 200.0,
            remainingAmount = 100.0,
            unit = "SMS",
            expiryDate = 1000L,
            isActive = true,
            source = "SYSTEM",
            lastUpdated = 500L
        )

        assertEquals(balancePackage1, balancePackage2)
    }

    @Test
    fun balancePackageEntityInequality() {
        val balancePackage1 = BalancePackageEntity(
            id = "test_id_4",
            simId = "sim_4",
            type = "bonus",
            totalAmount = 10.0,
            remainingAmount = 10.0,
            unit = "ETB",
            expiryDate = 2000L,
            isActive = true,
            source = "SMS",
            lastUpdated = 1500L
        )
        val balancePackage2 = BalancePackageEntity(
            id = "test_id_5", // Different ID
            simId = "sim_4",
            type = "bonus",
            totalAmount = 10.0,
            remainingAmount = 10.0,
            unit = "ETB",
            expiryDate = 2000L,
            isActive = true,
            source = "SMS",
            lastUpdated = 1500L
        )

        assert(balancePackage1 != balancePackage2)
    }
}
