package com.ethiobalance.app.data

import org.junit.Assert.assertEquals
import org.junit.Test

class SmsLogEntityTest {

    @Test
    fun createSmsLogEntity() {
        val smsLog = SmsLogEntity(
            id = 1L,
            sender = "SenderX",
            message = "Test message content",
            parsedType = "FULL_BALANCE",
            confidence = 0.95f,
            processed = true,
            timestamp = System.currentTimeMillis(),
            bodyHash = "Test message content".hashCode()
        )

        assertEquals(1L, smsLog.id)
        assertEquals("SenderX", smsLog.sender)
        assertEquals("Test message content", smsLog.message)
        assertEquals("FULL_BALANCE", smsLog.parsedType)
        assertEquals(0.95f, smsLog.confidence, 0.0f)
        assertEquals(true, smsLog.processed)
        assert(smsLog.timestamp > 0L)
        assertEquals("Test message content".hashCode(), smsLog.bodyHash)
    }

    @Test
    fun smsLogEntityWithDefaultValues() {
        val smsLog = SmsLogEntity(
            sender = "SenderY",
            message = "Another message",
            parsedType = null,
            confidence = 0.5f,
            processed = false,
            timestamp = System.currentTimeMillis()
        )

        assertEquals(0L, smsLog.id) // Default auto-generated ID
        assertEquals("SenderY", smsLog.sender)
        assertEquals("Another message", smsLog.message)
        assertEquals(null, smsLog.parsedType)
        assertEquals(0.5f, smsLog.confidence, 0.0f)
        assertEquals(false, smsLog.processed)
        assert(smsLog.timestamp > 0L)
        assertEquals(0, smsLog.bodyHash) // Default value
    }

    @Test
    fun smsLogEntityEquality() {
        val timestamp = System.currentTimeMillis()
        val smsLog1 = SmsLogEntity(id = 2L, sender = "SenderZ", message = "Message Z", parsedType = "TRANSACTION", confidence = 0.8f, processed = true, timestamp = timestamp, bodyHash = 12345)
        val smsLog2 = SmsLogEntity(id = 2L, sender = "SenderZ", message = "Message Z", parsedType = "TRANSACTION", confidence = 0.8f, processed = true, timestamp = timestamp, bodyHash = 12345)

        assertEquals(smsLog1, smsLog2)
    }

    @Test
    fun smsLogEntityInequality() {
        val timestamp = System.currentTimeMillis()
        val smsLog1 = SmsLogEntity(id = 3L, sender = "SenderA", message = "Message A", parsedType = "UNKNOWN", confidence = 0.3f, processed = false, timestamp = timestamp, bodyHash = 67890)
        val smsLog2 = SmsLogEntity(id = 4L, sender = "SenderA", message = "Message A", parsedType = "UNKNOWN", confidence = 0.3f, processed = false, timestamp = timestamp, bodyHash = 67890) // Different ID

        assert(smsLog1 != smsLog2)
    }
}
