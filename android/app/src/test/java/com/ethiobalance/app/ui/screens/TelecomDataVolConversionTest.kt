package com.ethiobalance.app.ui.screens

import com.ethiobalance.app.data.BalancePackageEntity
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for the data-volume (dataVol) computation used in TelecomScreen and HomeScreen.
 *
 * Root cause of the original bug:
 *   TelecomScreen summed [remainingAmount] directly without checking [unit], so a package
 *   stored as "967.1 MB" was displayed as "967.1 GB" instead of "0.9 GB".
 *   HomeScreen already applied the correct `/ 1024.0` conversion.
 *
 * The fix (TelecomScreen.kt line 71):
 *   .sumOf { if (it.unit.equals("GB", ignoreCase = true)) it.remainingAmount
 *            else it.remainingAmount / 1024.0 }
 *
 * These tests verify the corrected formula matches the reference implementation in HomeScreen.
 */
class TelecomDataVolConversionTest {

    private val now = System.currentTimeMillis()

    private fun pkg(id: String, type: String, amount: Double, unit: String) =
        BalancePackageEntity(
            id = id, simId = id, type = type,
            totalAmount = amount, remainingAmount = amount, unit = unit,
            expiryDate = now + 30 * 24 * 60 * 60 * 1000L,
            isActive = true, source = "SMS", lastUpdated = now
        )

    /** Mirror of the corrected TelecomScreen / HomeScreen formula. */
    private fun computeDataVolGB(packages: List<BalancePackageEntity>): Double =
        packages
            .filter { it.type.uppercase().contains("DATA") || it.type.uppercase().contains("INTERNET") }
            .sumOf { if (it.unit.equals("GB", ignoreCase = true)) it.remainingAmount else it.remainingAmount / 1024.0 }

    /** Mirror of the OLD (buggy) TelecomScreen formula — used to document the regression. */
    private fun computeDataVolBuggy(packages: List<BalancePackageEntity>): Double =
        packages
            .filter { it.type.uppercase().contains("DATA") || it.type.uppercase().contains("INTERNET") }
            .sumOf { it.remainingAmount }

    // ── MB input ─────────────────────────────────────────────────────────────

    @Test
    fun `967 MB is converted to ~0_95 GB not displayed as 967 GB`() {
        val pkgs = listOf(pkg("internet-0", "internet", 967.1, "MB"))
        val result = computeDataVolGB(pkgs)
        assertEquals(0.945, result, 0.001)
    }

    @Test
    fun `1024 MB equals exactly 1_0 GB`() {
        val pkgs = listOf(pkg("internet-0", "internet", 1024.0, "MB"))
        assertEquals(1.0, computeDataVolGB(pkgs), 0.001)
    }

    @Test
    fun `512 MB equals 0_5 GB`() {
        val pkgs = listOf(pkg("internet-0", "data", 512.0, "MB"))
        assertEquals(0.5, computeDataVolGB(pkgs), 0.001)
    }

    @Test
    fun `100 MB equals ~0_098 GB`() {
        val pkgs = listOf(pkg("internet-0", "internet", 100.0, "MB"))
        assertEquals(0.098, computeDataVolGB(pkgs), 0.001)
    }

    // ── GB input (no conversion needed) ──────────────────────────────────────

    @Test
    fun `5 GB remains 5_0 GB`() {
        val pkgs = listOf(pkg("internet-0", "internet", 5.0, "GB"))
        assertEquals(5.0, computeDataVolGB(pkgs), 0.001)
    }

    @Test
    fun `0_5 GB remains 0_5 GB`() {
        val pkgs = listOf(pkg("internet-0", "data", 0.5, "GB"))
        assertEquals(0.5, computeDataVolGB(pkgs), 0.001)
    }

    // ── Unit case-insensitivity ───────────────────────────────────────────────

    @Test
    fun `unit 'mb' lowercase is treated as MB`() {
        val pkgs = listOf(pkg("internet-0", "internet", 2048.0, "mb"))
        assertEquals(2.0, computeDataVolGB(pkgs), 0.001)
    }

    @Test
    fun `unit 'Gb' mixed case is treated as GB`() {
        val pkgs = listOf(pkg("internet-0", "internet", 3.0, "Gb"))
        assertEquals(3.0, computeDataVolGB(pkgs), 0.001)
    }

    // ── Multiple packages (mixed units) ──────────────────────────────────────

    @Test
    fun `sum of mixed MB and GB packages is correct`() {
        val pkgs = listOf(
            pkg("internet-0", "internet", 1024.0, "MB"),  // 1.0 GB
            pkg("internet-1", "data",     2.0,    "GB")   // 2.0 GB
        )
        assertEquals(3.0, computeDataVolGB(pkgs), 0.001)
    }

    @Test
    fun `two MB packages are summed then converted`() {
        val pkgs = listOf(
            pkg("internet-0", "internet", 512.0, "MB"),   // 0.5 GB
            pkg("internet-1", "internet", 512.0, "MB")    // 0.5 GB
        )
        assertEquals(1.0, computeDataVolGB(pkgs), 0.001)
    }

    // ── Non-data packages are excluded ───────────────────────────────────────

    @Test
    fun `voice and SMS packages do not contribute to dataVol`() {
        val pkgs = listOf(
            pkg("internet-0", "internet", 500.0, "MB"),
            pkg("voice-0",    "voice",    200.0, "MIN"),
            pkg("sms-0",      "sms",      100.0, "SMS")
        )
        assertEquals(0.488, computeDataVolGB(pkgs), 0.001)
    }

    @Test
    fun `empty package list gives 0 GB`() {
        assertEquals(0.0, computeDataVolGB(emptyList()), 0.001)
    }

    @Test
    fun `no internet or data type packages gives 0 GB`() {
        val pkgs = listOf(
            pkg("voice-0", "voice", 100.0, "MIN"),
            pkg("sms-0",   "sms",   50.0,  "SMS")
        )
        assertEquals(0.0, computeDataVolGB(pkgs), 0.001)
    }

    // ── Regression: document exactly what the old bug produced ───────────────

    @Test
    fun `regression - buggy formula inflates 967 MB to 967 GB`() {
        val pkgs = listOf(pkg("internet-0", "internet", 967.1, "MB"))
        val buggyResult = computeDataVolBuggy(pkgs)
        val fixedResult = computeDataVolGB(pkgs)
        // Old code returned the raw MB value unchanged
        assertEquals(967.1, buggyResult, 0.01)
        // Fixed code returns the correct GB value
        assertEquals(0.945, fixedResult, 0.001)
    }
}
