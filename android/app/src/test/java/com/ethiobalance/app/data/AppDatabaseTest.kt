package com.ethiobalance.app.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ethiobalance.app.data.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class AppDatabaseTest {

    private lateinit var db: AppDatabase
    private lateinit var balancePackageDao: BalancePackageDao
    private lateinit var smsDao: SmsDao
    private lateinit var smsLogDao: SmsLogDao
    private lateinit var transactionDao: TransactionDao
    private lateinit var ussdDao: UssdDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(
            context, AppDatabase::class.java
        ).allowMainThreadQueries().build()
        balancePackageDao = db.balancePackageDao()
        smsDao = db.smsDao()
        smsLogDao = db.smsLogDao()
        transactionDao = db.transactionDao()
        ussdDao = db.ussdDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    @Throws(Exception::class)
    fun writeAndReadBalancePackage() = runBlocking {
        // The BalancePackageEntity now requires a full set of fields. Provide dummy values for the test.
        val balancePackage = BalancePackageEntity(
            id = "pkg-1",
            simId = "sim-1",
            type = "voice",
            subType = "",
            totalAmount = 100.0,
            remainingAmount = 100.0,
            unit = "MIN",
            expiryDate = System.currentTimeMillis() + 100_000,
            isActive = true,
            source = "SMS",
            lastUpdated = System.currentTimeMillis()
        )
        // Use the existing insertOrUpdate DAO method.
        balancePackageDao.insertOrUpdate(balancePackage)
        // Retrieve the inserted package via getAllPackages and verify it exists.
        val all = balancePackageDao.getAllPackages().first()
        val inserted = all.firstOrNull { it.id == balancePackage.id }
        assert(inserted != null)
    }

    @Test
    @Throws(Exception::class)
    fun writeAndReadSms() = runBlocking {
        // SmsEntity fields have changed: use body and provide simSlot.
        val sms = SmsEntity(
            sender = "Sender1",
            body = "Message1",
            timestamp = System.currentTimeMillis(),
            simSlot = 0,
            isSynced = false
        )
        smsDao.insert(sms)
        // Retrieve SMS list via the Flow API provided by the DAO.
        val allSms = smsDao.getAllSmsFlow().first()
        assert(allSms.isNotEmpty())
        assert(allSms[0].sender == sms.sender)
    }

    @Test
    @Throws(Exception::class)
    fun writeAndReadSmsLog() = runBlocking {
        // SmsLogEntity now requires a different set of fields.
        val smsLog = SmsLogEntity(
            sender = "Sender1",
            message = "Message1",
            parsedType = null,
            confidence = 1.0f,
            processed = true,
            timestamp = System.currentTimeMillis(),
            bodyHash = 0
        )
        // Insert using the DAO (assume insert method exists).
        smsLogDao.insert(smsLog)
        // SmsLogDao provides getAllLogs() returning a List.
        val allSmsLogs = smsLogDao.getAllLogs()
        assert(allSmsLogs.isNotEmpty())
        assert(allSmsLogs[0].processed == smsLog.processed)
    }

    @Test
    @Throws(Exception::class)
    fun writeAndReadTransaction() = runBlocking {
        val transactionSource = TransactionSourceEntity(
            abbreviation = "Bank A",
            name = "Bank A",
            ussd = "",
            senderId = "12345",
            isEnabled = true,
            lastUpdated = System.currentTimeMillis()
        )
        db.transactionSourceDao().insertOrUpdate(transactionSource)
        val source = db.transactionSourceDao().getAllSources().first().first()

        val transaction = TransactionEntity(
            id = "test-1",
            amount = 50.0,
            type = "INCOME",
            category = "TRANSFER",
            source = source.abbreviation,
            timestamp = System.currentTimeMillis(),
            reference = "Test Transaction",
            partyName = null,
            transactionSubType = null
        )
        transactionDao.insert(transaction)
        val allTransactions = transactionDao.getAllTransactions().first()
        assert(allTransactions.isNotEmpty())
        assert(allTransactions[0].reference == transaction.reference)
    }

    @Test
    @Throws(Exception::class)
    fun writeAndReadUssd() = runBlocking {
        val ussd = UssdEntity(
            request = "*804#",
            response = "Balance: 100 Birr",
            timestamp = System.currentTimeMillis(),
            simSlot = 0
        )
        ussdDao.insert(ussd)
        // UssdDao provides getAllUssdEvents() returning a List.
        val allUssd = ussdDao.getAllUssdEvents()
        assert(allUssd.isNotEmpty())
        assert(allUssd[0].request == ussd.request)
    }
}
