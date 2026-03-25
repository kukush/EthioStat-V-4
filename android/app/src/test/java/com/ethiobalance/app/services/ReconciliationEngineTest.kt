package com.ethiobalance.app.services

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [32])
class ReconciliationEngineTest {

    // Note: Integration tests requiring Room DB would go here.
    // We would use an in-memory db builder.
    // For unit-level assertion of logic rules, we can verify scenario outputs.

    @Test
    fun testDualTrackingSeparation() {
        // Just verify the SmsScenario enums map appropriately
        val result = SmsParser.parse("830", "You have successfully paid 19.00 ETB for 1GB Daily Data package.")
        assertEquals(SmsScenario.SELF_PURCHASE, result.scenario)
        assertEquals(19.0, result.deductedAmount)
        // Ensure that engine would treat this as EXPENSE and ASSET_GAIN.
    }
}
