package com.ethiobalance.app.data

import org.junit.Assert.assertEquals
import org.junit.Test

class SmsEntityTest {

    @Test
    fun createSmsEntity() {
        val smsEntity = SmsEntity(
            id = 1,
            sender = "EthioTelecom",
            body = "You have 100MB remaining.",
            timestamp = System.currentTimeMillis(),
            simSlot = 0,
            isSynced = false
        )

        assertEquals(1, smsEntity.id)
        assertEquals("EthioTelecom", smsEntity.sender)
        assertEquals("You have 100MB remaining.", smsEntity.body)
        assert(smsEntity.timestamp > 0L)
        assertEquals(0, smsEntity.simSlot)
        assertEquals(false, smsEntity.isSynced)
    }

    @Test
    fun smsEntityWithDefaultIdAndIsSynced() {
        val smsEntity = SmsEntity(
            sender = "CBE",
            body = "Your account has been credited with 500 ETB.",
            timestamp = System.currentTimeMillis(),
            simSlot = 1
        )

        assertEquals(0, smsEntity.id) // Default value
        assertEquals("CBE", smsEntity.sender)
        assertEquals("Your account has been credited with 500 ETB.", smsEntity.body)
        assert(smsEntity.timestamp > 0L)
        assertEquals(1, smsEntity.simSlot)
        assertEquals(false, smsEntity.isSynced) // Default value
    }

    @Test
    fun smsEntityEquality() {
        val timestamp = System.currentTimeMillis()
        val smsEntity1 = SmsEntity(id = 2, sender = "SenderX", body = "Message X", timestamp = timestamp, simSlot = 0, isSynced = true)
        val smsEntity2 = SmsEntity(id = 2, sender = "SenderX", body = "Message X", timestamp = timestamp, simSlot = 0, isSynced = true)

        assertEquals(smsEntity1, smsEntity2)
    }

    @Test
    fun smsEntityInequality() {
        val timestamp = System.currentTimeMillis()
        val smsEntity1 = SmsEntity(id = 3, sender = "SenderY", body = "Message Y", timestamp = timestamp, simSlot = 1, isSynced = false)
        val smsEntity2 = SmsEntity(id = 4, sender = "SenderY", body = "Message Y", timestamp = timestamp, simSlot = 1, isSynced = false) // Different ID

        assert(smsEntity1 != smsEntity2)
    }
}
