package com.ethiobalance.app.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class TransactionDaoTest {

    private lateinit var transactionDao: TransactionDao
    private lateinit var db: AppDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(
            context, AppDatabase::class.java).allowMainThreadQueries().build()
        transactionDao = db.transactionDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    @Throws(Exception::class)
    fun insertAndGetAllTransactions() = runBlocking {
        val transactionSource = TransactionSourceEntity(abbreviation = "Bank", name = "Bank", ussd = "", senderId = "123", isEnabled = true, lastUpdated = System.currentTimeMillis())
        db.transactionSourceDao().insertOrUpdate(transactionSource)
        val source = db.transactionSourceDao().getAllSources().first().first()

        val transaction1 = TransactionEntity(id = "1", amount = 100.0, type = "Credit", timestamp = 1L, reference = "Deposit", source = source.abbreviation, category = "TRANSFER", partyName = null, transactionSubType = null)
        val transaction2 = TransactionEntity(id = "2", amount = 50.0, type = "Debit", timestamp = 2L, reference = "Withdrawal", source = source.abbreviation, category = "TRANSFER", partyName = null, transactionSubType = null)
        transactionDao.insert(transaction1)
        transactionDao.insert(transaction2)

        val allTransactions = transactionDao.getAllTransactions().first()
        assert(allTransactions.size == 2)
        assert(allTransactions[0].id == transaction2.id) // Ordered by timestamp DESC
        assert(allTransactions[1].id == transaction1.id)
    }

    @Test
    @Throws(Exception::class)
    fun existsById() = runBlocking {
        val transactionSource = TransactionSourceEntity(abbreviation = "Bank", name = "Bank", ussd = "", senderId = "123", isEnabled = true, lastUpdated = System.currentTimeMillis())
        db.transactionSourceDao().insertOrUpdate(transactionSource)
        val source = db.transactionSourceDao().getAllSources().first().first()

        val transaction = TransactionEntity(id = "3", amount = 200.0, type = "Credit", timestamp = 3L, reference = "Transfer In", source = source.abbreviation, category = "TRANSFER", partyName = null, transactionSubType = null)
        transactionDao.insert(transaction)

        assert(transactionDao.existsById("3"))
        assert(!transactionDao.existsById("999"))
    }

    @Test
    @Throws(Exception::class)
    fun existsNearDuplicate() = runBlocking {
        val transactionSource = TransactionSourceEntity(abbreviation = "Bank", name = "Bank", ussd = "", senderId = "123", isEnabled = true, lastUpdated = System.currentTimeMillis())
        db.transactionSourceDao().insertOrUpdate(transactionSource)
        val source = db.transactionSourceDao().getAllSources().first().first()

        val ts = System.currentTimeMillis()
        val transaction = TransactionEntity(id = "4", amount = 123.45, type = "Debit", timestamp = ts, reference = "Payment", source = source.abbreviation, category = "TRANSFER", partyName = null, transactionSubType = null)
        transactionDao.insert(transaction)

        // Exact duplicate within window
        assert(transactionDao.existsNearDuplicate(source.abbreviation, "Debit", 123.45, ts, 1000L))
        // Same transaction, different timestamp but outside window
        assert(!transactionDao.existsNearDuplicate(source.abbreviation, "Debit", 123.45, ts + 2000L, 1000L))
        // Different amount
        assert(!transactionDao.existsNearDuplicate(source.abbreviation, "Debit", 123.46, ts, 1000L))
    }

    @Test
    @Throws(Exception::class)
    fun getTotalByType() = runBlocking {
        val transactionSource = TransactionSourceEntity(abbreviation = "Bank", name = "Bank", ussd = "", senderId = "123", isEnabled = true, lastUpdated = System.currentTimeMillis())
        db.transactionSourceDao().insertOrUpdate(transactionSource)
        val source = db.transactionSourceDao().getAllSources().first().first()

        transactionDao.insert(TransactionEntity(id = "5", amount = 100.0, type = "Credit", timestamp = 5L, reference = "", source = source.abbreviation, category = "TRANSFER", partyName = null, transactionSubType = null))
        transactionDao.insert(TransactionEntity(id = "6", amount = 200.0, type = "Debit", timestamp = 6L, reference = "", source = source.abbreviation, category = "TRANSFER", partyName = null, transactionSubType = null))
        transactionDao.insert(TransactionEntity(id = "7", amount = 50.0, type = "Credit", timestamp = 7L, reference = "", source = source.abbreviation, category = "TRANSFER", partyName = null, transactionSubType = null))

        val totalCredit = transactionDao.getTotalByType("Credit")
        org.junit.Assert.assertEquals(150.0, totalCredit ?: 0.0, 0.0)

        val totalDebit = transactionDao.getTotalByType("Debit")
        org.junit.Assert.assertEquals(200.0, totalDebit ?: 0.0, 0.0)

        val totalUnknown = transactionDao.getTotalByType("Unknown")
        org.junit.Assert.assertEquals(null, totalUnknown)
    }

    @Test
    @Throws(Exception::class)
    fun countBySource() = runBlocking {
        val transactionSource1 = TransactionSourceEntity(abbreviation = "Bank A", name = "Bank A", ussd = "", senderId = "123", isEnabled = true, lastUpdated = System.currentTimeMillis())
        val transactionSource2 = TransactionSourceEntity(abbreviation = "Bank B", name = "Bank B", ussd = "", senderId = "456", isEnabled = true, lastUpdated = System.currentTimeMillis())
        db.transactionSourceDao().insertOrUpdate(transactionSource1)
        db.transactionSourceDao().insertOrUpdate(transactionSource2)
        val sourceA = db.transactionSourceDao().getAllSources().first().first { it.abbreviation == "Bank A" }
        val sourceB = db.transactionSourceDao().getAllSources().first().first { it.abbreviation == "Bank B" }

        transactionDao.insert(TransactionEntity(id = "8", amount = 10.0, type = "Credit", timestamp = 8L, reference = "", source = sourceA.abbreviation, category = "TRANSFER", partyName = null, transactionSubType = null))
        transactionDao.insert(TransactionEntity(id = "9", amount = 20.0, type = "Debit", timestamp = 9L, reference = "", source = sourceA.abbreviation, category = "TRANSFER", partyName = null, transactionSubType = null))
        transactionDao.insert(TransactionEntity(id = "10", amount = 30.0, type = "Credit", timestamp = 10L, reference = "", source = sourceB.abbreviation, category = "TRANSFER", partyName = null, transactionSubType = null))

        org.junit.Assert.assertEquals(2, transactionDao.countBySource(sourceA.abbreviation))
        org.junit.Assert.assertEquals(1, transactionDao.countBySource(sourceB.abbreviation))
        org.junit.Assert.assertEquals(0, transactionDao.countBySource("Bank C"))
    }

    @Test
    @Throws(Exception::class)
    fun deleteAll() = runBlocking {
        val transactionSource = TransactionSourceEntity(abbreviation = "Bank", name = "Bank", ussd = "", senderId = "123", isEnabled = true, lastUpdated = System.currentTimeMillis())
        db.transactionSourceDao().insertOrUpdate(transactionSource)
        val source = db.transactionSourceDao().getAllSources().first().first()

        val transaction1 = TransactionEntity(id = "11", amount = 100.0, type = "Credit", timestamp = 11L, reference = "", source = source.abbreviation, category = "TRANSFER", partyName = null, transactionSubType = null)
        transactionDao.insert(transaction1)
        
        transactionDao.deleteAll()

        val allTransactions = transactionDao.getAllTransactions().first()
        assert(allTransactions.isEmpty())
    }
}
