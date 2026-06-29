package com.ethiobalance.app.data

import org.junit.Assert.assertEquals
import org.junit.Test

class UssdEntityTest {

    @Test
    fun ussdEntity_constructorAndGettersWorkCorrectly() {
        val ussdEntity = UssdEntity(
            id = 1,
            request = "*804#",
            response = "Your balance is 100 ETB",
            timestamp = 1678886400000L, // March 15, 2023 12:00:00 PM GMT
            simSlot = 0
        )

        assertEquals(1, ussdEntity.id)
        assertEquals("*804#", ussdEntity.request)
        assertEquals("Your balance is 100 ETB", ussdEntity.response)
        assertEquals(1678886400000L, ussdEntity.timestamp)
        assertEquals(0, ussdEntity.simSlot)
    }

    @Test
    fun ussdEntity_defaultIdIsZero() {
        val ussdEntity = UssdEntity(
            request = "*804#",
            response = "Your balance is 100 ETB",
            timestamp = 1678886400000L,
            simSlot = 0
        )
        assertEquals(0, ussdEntity.id)
    }
}
