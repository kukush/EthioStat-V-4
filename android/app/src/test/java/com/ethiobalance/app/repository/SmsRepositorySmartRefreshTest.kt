package com.ethiobalance.app.repository

import com.ethiobalance.app.repository.SmsRepository.Companion.SmsRow
import com.ethiobalance.app.repository.SmsRepository.Companion.isMultiSegmentBalance
import com.ethiobalance.app.repository.SmsRepository.Companion.pickRefreshTargets
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [SmsRepository]'s smart refresh decision logic.
 *
 * Validates the three startup/sync scenarios described in the plan:
 *   1. Latest 994 SMS is multi-segment             → process only it.
 *   2. Latest is single-segment, prior multi exists → process multi FIRST, then single.
 *   3. Latest is single-segment, no prior multi     → process only the latest single.
 *   4. No 994 SMS at all                            → no-op.
 *
 * Production scan depths:
 *   • Startup refresh (MainActivity): scanDepth = 5
 *   • Sync button (TelecomViewModel): scanDepth = 10 (deeper scan to find multi-segment
 *     even when newer single-segment notifications arrived after the last balance SMS)
 */
class SmsRepositorySmartRefreshTest {

    private val multiSegmentBody = "Dear Customer, your remaining amount from Monthly Internet Package 12GB from telebirr to be expired after 30 days is 11458.902 MB with expiry date on 2026-05-16 at 11:42:05;  from 50 minutes + 200 MB Free is 44 minute and 3 second with expiry date on 2026-04-22 at 00:00:00;   from Create Your Own Package Monthly is 136 SMS with expiry date on 2026-04-19 at 00:22:19;"

    private val singleSegmentBody = "Dear customer You have received Night Internet package 600MB from telebirr expire after 24 hr from 0. The package Will be expired on 17-04-2026 06:59:59."

    @Test
    fun `isMultiSegmentBalance identifies multi-segment SMS`() {
        assertTrue(isMultiSegmentBalance(multiSegmentBody))
    }

    @Test
    fun `isMultiSegmentBalance rejects single-segment SMS`() {
        assertFalse(isMultiSegmentBalance(singleSegmentBody))
    }

    @Test
    fun `latest multi-segment processes only the latest`() {
        val rows = listOf(
            SmsRow("994", multiSegmentBody, 2000L),
            SmsRow("994", singleSegmentBody, 1000L)
        )
        val targets = pickRefreshTargets(rows)
        assertEquals(1, targets.size)
        assertEquals(2000L, targets[0].ts)
    }

    @Test
    fun `latest single with prior multi processes multi first then single`() {
        val rows = listOf(
            SmsRow("994", singleSegmentBody, 2000L),
            SmsRow("994", multiSegmentBody, 1000L)
        )
        val targets = pickRefreshTargets(rows)
        assertEquals(2, targets.size)
        // Multi-segment (older) must be processed FIRST so it purges first
        assertEquals(1000L, targets[0].ts)
        assertTrue(isMultiSegmentBalance(targets[0].body))
        // Then the latest single-segment is additively merged
        assertEquals(2000L, targets[1].ts)
        assertFalse(isMultiSegmentBalance(targets[1].body))
    }

    @Test
    fun `latest single without any prior multi processes only the single`() {
        val rows = listOf(
            SmsRow("994", singleSegmentBody, 3000L),
            SmsRow("994", singleSegmentBody, 2000L),
            SmsRow("994", singleSegmentBody, 1000L)
        )
        val targets = pickRefreshTargets(rows)
        assertEquals(1, targets.size)
        assertEquals(3000L, targets[0].ts)
    }

    @Test
    fun `empty inbox returns empty list`() {
        assertTrue(pickRefreshTargets(emptyList()).isEmpty())
    }

    @Test
    fun `deeper prior multi within scan depth is still found`() {
        val rows = listOf(
            SmsRow("994", singleSegmentBody, 5000L), // latest = single
            SmsRow("994", singleSegmentBody, 4000L),
            SmsRow("994", singleSegmentBody, 3000L),
            SmsRow("994", multiSegmentBody, 2000L),  // prior multi, deeper
            SmsRow("994", singleSegmentBody, 1000L)
        )
        val targets = pickRefreshTargets(rows)
        assertEquals(2, targets.size)
        assertEquals(2000L, targets[0].ts) // multi first
        assertEquals(5000L, targets[1].ts) // latest single
    }
}
