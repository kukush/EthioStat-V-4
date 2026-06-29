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
        val balancePackage = BalancePackageEntity(
            packageName = "Test Package",
            balance = 100.0,
            expiryDate = System.currentTimeMillis() + 100000,
            packageType = "Voice"
        )
        balancePackageDao.insertAll(balancePackage)
        val byName = balancePackageDao.getBalancePackageByName("Test Package").first()
        assert(byName?.packageName == balancePackage.packageName)
    }

    @Test
    @Throws(Exception::class)
    fun writeAndReadSms() = runBlocking {
        val sms = SmsEntity(
            sender = "Sender1",
            message = "Message1",
            timestamp = System.currentTimeMillis(),
            isRead = false,
            isProcessed = false
        )
        smsDao.insert(sms)
        val allSms = smsDao.getAllSms().first()
        assert(allSms.isNotEmpty())
        assert(allSms[0].sender == sms.sender)
    }

    @Test
    @Throws(Exception::class)
    fun writeAndReadSmsLog() = runBlocking {
        val smsLog = SmsLogEntity(
            smsId = 1,
            processingTimestamp = System.currentTimeMillis(),
            status = "Processed"
        )
        smsLogDao.insert(smsLog)
        val allSmsLogs = smsLogDao.getAllSmsLogs().first()
        assert(allSmsLogs.isNotEmpty())
        assert(allSmsLogs[0].status == smsLog.status)
    }

    @Test
    @Throws(Exception::class)
    fun writeAndReadTransaction() = runBlocking {
        val transactionSource = TransactionSourceEntity(
            sourceName = "Bank A",
            accountNumber = "12345"
        )
        transactionDao.insertTransactionSource(transactionSource)
        val source = transactionDao.getAllTransactionSources().first().first()

        val transaction = TransactionEntity(
            amount = 50.0,
            type = "Credit",
            date = System.currentTimeMillis(),
            description = "Test Transaction",
            sourceId = source.id
        )
        transactionDao.insertTransaction(transaction)
        val allTransactions = transactionDao.getAllTransactions().first()
        assert(allTransactions.isNotEmpty())
        assert(allTransactions[0].description == transaction.description)
    }

    @Test
    @Throws(Exception::class)
    fun writeAndReadUssd() = runBlocking {
        val ussd = UssdEntity(
            ussdCode = "*804#",
            response = "Balance: 100 Birr",
            timestamp = System.currentTimeMillis()
        )
        ussdDao.insert(ussd)
        val allUssd = ussdDao.getAllUssd().first()
        assert(allUssd.isNotEmpty())
        assert(allUssd[0].ussdCode == ussd.ussdCode)
    }
}
