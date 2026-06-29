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
        val transactionSource = TransactionSourceEntity(sourceName = "Bank", accountNumber = "123")
        db.transactionSourceDao().insert(transactionSource)
        val source = db.transactionSourceDao().getAllTransactionSources().first().first()

        val transaction1 = TransactionEntity(id = "1", amount = 100.0, type = "Credit", date = 1L, description = "Deposit", sourceId = source.id)
        val transaction2 = TransactionEntity(id = "2", amount = 50.0, type = "Debit", date = 2L, description = "Withdrawal", sourceId = source.id)
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
        val transactionSource = TransactionSourceEntity(sourceName = "Bank", accountNumber = "123")
        db.transactionSourceDao().insert(transactionSource)
        val source = db.transactionSourceDao().getAllTransactionSources().first().first()

        val transaction = TransactionEntity(id = "3", amount = 200.0, type = "Credit", date = 3L, description = "Transfer In", sourceId = source.id)
        transactionDao.insert(transaction)

        assert(transactionDao.existsById("3"))
        assert(!transactionDao.existsById("999"))
    }

    @Test
    @Throws(Exception::class)
    fun existsNearDuplicate() = runBlocking {
        val transactionSource = TransactionSourceEntity(sourceName = "Bank", accountNumber = "123")
        db.transactionSourceDao().insert(transactionSource)
        val source = db.transactionSourceDao().getAllTransactionSources().first().first()

        val timestamp = System.currentTimeMillis()
        val transaction = TransactionEntity(id = "4", amount = 123.45, type = "Debit", date = timestamp, description = "Payment", sourceId = source.id)
        transactionDao.insert(transaction)

        // Exact duplicate within window
        assert(transactionDao.existsNearDuplicate(source.sourceName, "Debit", 123.45, timestamp, 1000L))
        // Same transaction, different timestamp but outside window
        assert(!transactionDao.existsNearDuplicate(source.sourceName, "Debit", 123.45, timestamp + 2000L, 1000L))
        // Different amount
        assert(!transactionDao.existsNearDuplicate(source.sourceName, "Debit", 123.46, timestamp, 1000L))
    }

    @Test
    @Throws(Exception::class)
    fun getTotalByType() = runBlocking {
        val transactionSource = TransactionSourceEntity(sourceName = "Bank", accountNumber = "123")
        db.transactionSourceDao().insert(transactionSource)
        val source = db.transactionSourceDao().getAllTransactionSources().first().first()

        transactionDao.insert(TransactionEntity(id = "5", amount = 100.0, type = "Credit", date = 5L, description = "", sourceId = source.id))
        transactionDao.insert(TransactionEntity(id = "6", amount = 200.0, type = "Debit", date = 6L, description = "", sourceId = source.id))
        transactionDao.insert(TransactionEntity(id = "7", amount = 50.0, type = "Credit", date = 7L, description = "", sourceId = source.id))

        val totalCredit = transactionDao.getTotalByType("Credit")
        assertEquals(150.0, totalCredit, 0.0)

        val totalDebit = transactionDao.getTotalByType("Debit")
        assertEquals(200.0, totalDebit, 0.0)

        val totalUnknown = transactionDao.getTotalByType("Unknown")
        assertEquals(null, totalUnknown)
    }

    @Test
    @Throws(Exception::class)
    fun countBySource() = runBlocking {
        val transactionSource1 = TransactionSourceEntity(sourceName = "Bank A", accountNumber = "123")
        val transactionSource2 = TransactionSourceEntity(sourceName = "Bank B", accountNumber = "456")
        db.transactionSourceDao().insert(transactionSource1)
        db.transactionSourceDao().insert(transactionSource2)
        val sourceA = db.transactionSourceDao().getAllTransactionSources().first().first { it.sourceName == "Bank A" }
        val sourceB = db.transactionSourceDao().getAllTransactionSources().first().first { it.sourceName == "Bank B" }

        transactionDao.insert(TransactionEntity(id = "8", amount = 10.0, type = "Credit", date = 8L, description = "", sourceId = sourceA.id))
        transactionDao.insert(TransactionEntity(id = "9", amount = 20.0, type = "Debit", date = 9L, description = "", sourceId = sourceA.id))
        transactionDao.insert(TransactionEntity(id = "10", amount = 30.0, type = "Credit", date = 10L, description = "", sourceId = sourceB.id))

        assertEquals(2, transactionDao.countBySource(sourceA.sourceName))
        assertEquals(1, transactionDao.countBySource(sourceB.sourceName))
        assertEquals(0, transactionDao.countBySource("Bank C"))
    }

    @Test
    @Throws(Exception::class)
    fun deleteAll() = runBlocking {
        val transactionSource = TransactionSourceEntity(sourceName = "Bank", accountNumber = "123")
        db.transactionSourceDao().insert(transactionSource)
        val source = db.transactionSourceDao().getAllTransactionSources().first().first()

        val transaction1 = TransactionEntity(id = "11", amount = 100.0, type = "Credit", date = 11L, description = "", sourceId = source.id)
        transactionDao.insert(transaction1)
        
        transactionDao.deleteAll()

        val allTransactions = transactionDao.getAllTransactions().first()
        assert(allTransactions.isEmpty())
    }
}
