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
import org.junit.Assert.assertEquals
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class TransactionSourceDaoTest {

    private lateinit var transactionSourceDao: TransactionSourceDao
    private lateinit var db: AppDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(
            context, AppDatabase::class.java).allowMainThreadQueries().build()
        transactionSourceDao = db.transactionSourceDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    @Throws(Exception::class)
    fun insertAndGetAllSources() = runBlocking {
        val source1 = TransactionSourceEntity(abbreviation = "CBE", name = "Commercial Bank", ussd = "*889#", senderId = "CBE")
        val source2 = TransactionSourceEntity(abbreviation = "TB", name = "Telebirr", ussd = "*127#", senderId = "Telebirr,127")
        transactionSourceDao.insertOrUpdate(source1)
        transactionSourceDao.insertOrUpdate(source2)

        val allSources = transactionSourceDao.getAllSources().first()
        assert(allSources.size == 2)
        assert(allSources.map { it.abbreviation }.containsAll(listOf("CBE", "TB")))
    }

    @Test
    @Throws(Exception::class)
    fun updateSource() = runBlocking {
        val source = TransactionSourceEntity(abbreviation = "CBE", name = "Commercial Bank", ussd = "*889#", senderId = "CBE")
        transactionSourceDao.insertOrUpdate(source)

        val updatedSource = source.copy(name = "Commercial Bank of Ethiopia")
        transactionSourceDao.insertOrUpdate(updatedSource)

        val allSources = transactionSourceDao.getAllSources().first()
        assert(allSources.size == 1)
        assert(allSources[0].name == "Commercial Bank of Ethiopia")
    }

    @Test
    @Throws(Exception::class)
    fun insertAllSources() = runBlocking {
        val source1 = TransactionSourceEntity(abbreviation = "AWASH", name = "Awash Bank", ussd = "*787#", senderId = "Awash")
        val source2 = TransactionSourceEntity(abbreviation = "HIBRET", name = "Hibret Bank", ussd = "*812#", senderId = "Hibret")
        transactionSourceDao.insertAll(listOf(source1, source2))

        val allSources = transactionSourceDao.getAllSources().first()
        assert(allSources.size == 2)
        assert(allSources.map { it.abbreviation }.containsAll(listOf("AWASH", "HIBRET")))
    }

    @Test
    @Throws(Exception::class)
    fun deleteByAbbreviation() = runBlocking {
        val source1 = TransactionSourceEntity(abbreviation = "CBE", name = "Commercial Bank", ussd = "*889#", senderId = "CBE")
        val source2 = TransactionSourceEntity(abbreviation = "TB", name = "Telebirr", ussd = "*127#", senderId = "Telebirr,127")
        transactionSourceDao.insertOrUpdate(source1)
        transactionSourceDao.insertOrUpdate(source2)

        transactionSourceDao.deleteByAbbreviation("CBE")

        val allSources = transactionSourceDao.getAllSources().first()
        assert(allSources.size == 1)
        assert(allSources[0].abbreviation == "TB")
    }

    @Test
    @Throws(Exception::class)
    fun getEnabledSenderIds() = runBlocking {
        val source1 = TransactionSourceEntity(abbreviation = "CBE", name = "Commercial Bank", ussd = "*889#", senderId = "CBE,889", isEnabled = true)
        val source2 = TransactionSourceEntity(abbreviation = "TB", name = "Telebirr", ussd = "*127#", senderId = "Telebirr", isEnabled = false)
        val source3 = TransactionSourceEntity(abbreviation = "AWASH", name = "Awash Bank", ussd = "*787#", senderId = "AWASHBANK", isEnabled = true)
        transactionSourceDao.insertAll(listOf(source1, source2, source3))

        val enabledSenderIds = transactionSourceDao.getEnabledSenderIds()
        assert(enabledSenderIds.size == 2)
        assert(enabledSenderIds.contains("CBE,889"))
        assert(enabledSenderIds.contains("AWASHBANK"))
    }

    @Test
    @Throws(Exception::class)
    fun getEnabledSenderIdsFlattened() = runBlocking {
        val source1 = TransactionSourceEntity(abbreviation = "CBE", name = "Commercial Bank", ussd = "*889#", senderId = "CBE,889", isEnabled = true)
        val source2 = TransactionSourceEntity(abbreviation = "TB", name = "Telebirr", ussd = "*127#", senderId = "Telebirr", isEnabled = false)
        val source3 = TransactionSourceEntity(abbreviation = "AWASH", name = "Awash Bank", ussd = "*787#", senderId = "AWASHBANK,787", isEnabled = true)
        transactionSourceDao.insertAll(listOf(source1, source2, source3))

        val enabledSenderIdsFlattened = transactionSourceDao.getEnabledSenderIdsFlattened()
        assertEquals(4, enabledSenderIdsFlattened.size)
        assert(enabledSenderIdsFlattened.containsAll(listOf("CBE", "889", "AWASHBANK", "787")))
    }
}
