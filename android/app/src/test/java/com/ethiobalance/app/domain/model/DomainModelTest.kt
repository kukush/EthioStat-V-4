package com.ethiobalance.app.domain.model

import com.ethiobalance.app.constants.Avatars
import com.ethiobalance.app.constants.Languages
import com.ethiobalance.app.constants.PhoneConstants
import com.ethiobalance.app.data.BalancePackageEntity
import com.ethiobalance.app.data.SmsEntity
import com.ethiobalance.app.data.SmsLogEntity
import com.ethiobalance.app.data.TransactionEntity
import com.ethiobalance.app.data.TransactionSourceEntity
import com.ethiobalance.app.data.UssdEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class DomainModelTest {

    @Test
    fun telecomPackageType_classifiesKnownPackageTypes() {
        assertSame(TelecomPackageType.SMS, TelecomPackageType.classify("25 SMS", "sms"))
        assertSame(TelecomPackageType.BONUS_FUND, TelecomPackageType.classify("gift balance", "bonus"))
        assertSame(TelecomPackageType.DATA, TelecomPackageType.classify("500 MB internet", "internet"))
        assertSame(TelecomPackageType.BONUS_VOICE, TelecomPackageType.classify("free voice minutes", "voice"))
        assertSame(TelecomPackageType.NIGHT_VOICE, TelecomPackageType.classify("night package", "voice"))
        assertSame(TelecomPackageType.RECURRING_VOICE, TelecomPackageType.classify("recurring voice", "voice"))
        assertSame(TelecomPackageType.UNKNOWN, TelecomPackageType.classify("mystery", "unknown"))
    }

    @Test
    fun telecomPackageType_classifiesVoicePairByNightQuota() {
        assertSame(TelecomPackageType.NIGHT_VOICE, TelecomPackageType.classifyVoicePair(63.0))
        assertSame(TelecomPackageType.RECURRING_VOICE, TelecomPackageType.classifyVoicePair(64.0))
    }

    @Test
    fun constantsExposeExpectedDefaultsAndHelpers() {
        assertTrue(Avatars.OPTIONS.contains(Avatars.DEFAULT))
        assertEquals("English", Languages.getDisplayName("en"))
        assertEquals("missing", Languages.getDisplayName("missing"))
        assertTrue(PhoneConstants.isValidEthiopianPhone("912345678"))
        assertTrue(PhoneConstants.isValidEthiopianPhone("712345678"))
        assertFalse(PhoneConstants.isValidEthiopianPhone("812345678"))
        assertFalse(PhoneConstants.isValidEthiopianPhone("91234567"))
    }

    @Test
    fun dataEntitiesExposeConstructorValues() {
        val transaction = TransactionEntity(
            id = "tx-1",
            type = "INCOME",
            amount = 42.0,
            category = "CREDIT",
            source = "CBE",
            timestamp = 1_000L,
            reference = "REF",
            partyName = "Sender",
            transactionSubType = "bank_transfer"
        )
        val source = TransactionSourceEntity(
            abbreviation = "CBE",
            name = "Commercial Bank of Ethiopia",
            ussd = "*889#",
            senderId = "847,CBE",
            isEnabled = false,
            lastUpdated = 2_000L
        )
        val sms = SmsEntity(
            id = 1,
            sender = "CBE",
            body = "credited",
            timestamp = 3_000L,
            simSlot = 0,
            isSynced = true
        )
        val log = SmsLogEntity(
            id = 7L,
            sender = "CBE",
            message = "credited",
            parsedType = "TRANSACTION",
            confidence = 0.95f,
            processed = true,
            timestamp = 4_000L,
            bodyHash = 123
        )
        val balancePackage = BalancePackageEntity(
            id = "internet-night",
            simId = "sim-1",
            type = "internet",
            subType = "Night",
            totalAmount = 600.0,
            remainingAmount = 512.0,
            unit = "MB",
            expiryDate = 6_000L,
            isActive = true,
            source = "SMS",
            lastUpdated = 7_000L
        )

        assertEquals("tx-1", transaction.id)
        assertEquals("CBE", source.abbreviation)
        assertEquals("credited", sms.body)
        assertEquals(0.95f, log.confidence)
        assertEquals("Night", balancePackage.subType)
    }
}
