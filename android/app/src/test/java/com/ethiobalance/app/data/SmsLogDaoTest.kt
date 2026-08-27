package com.ethiobalance.app.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.assertEquals
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class SmsLogDaoTest {

    private lateinit var smsLogDao: SmsLogDao
    private lateinit var db: AppDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(
            context, AppDatabase::class.java).allowMainThreadQueries().build()
        smsLogDao = db.smsLogDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    @Throws(Exception::class)
    fun insertAndGetAllLogs() = runBlocking {
        val log1 = SmsLogEntity(id = 1, sender = "SenderA", message = "MsgA", parsedType = "TRANSACTION", confidence = 1.0f, processed = true, bodyHash = 123, timestamp = 1L)
        val log2 = SmsLogEntity(id = 2, sender = "SenderB", message = "MsgB", parsedType = "UNKNOWN", confidence = 0.5f, processed = false, bodyHash = 456, timestamp = 2L)
        smsLogDao.insert(log1)
        smsLogDao.insert(log2)

        val allLogs = smsLogDao.getAllLogs()
        assert(allLogs.size == 2)
        assert(allLogs[0].sender == log1.sender)
        assert(allLogs[1].sender == log2.sender)
    }

    @Test
    @Throws(Exception::class)
    fun updateLog() = runBlocking {
        val log = SmsLogEntity(id = 1, sender = "SenderA", message = "Msg", parsedType = "TRANSACTION", confidence = 1.0f, processed = true, bodyHash = 123, timestamp = 1L)
        smsLogDao.insert(log)

        val updatedLog = log.copy(processed = false)
        smsLogDao.update(updatedLog)

        val retrievedLog = smsLogDao.getAllLogs().first { it.id == log.id }
        assert(!retrievedLog.processed)
    }

    @Test
    @Throws(Exception::class)
    fun existsByHash() = runBlocking {
        val log = SmsLogEntity(id = 1, sender = "SenderA", message = "Msg", parsedType = "TRANSACTION", confidence = 1.0f, processed = true, bodyHash = 123, timestamp = 1L)
        smsLogDao.insert(log)

        val exists = smsLogDao.existsByHash("SenderA", 1L, 123)
        assert(exists)

        val notExists = smsLogDao.existsByHash("SenderA", 1L, 999)
        assert(!notExists)
    }

    @Test
    @Throws(Exception::class)
    fun getLastTimestampForSender() = runBlocking {
        val log1 = SmsLogEntity(id = 1, sender = "SenderX", message = "Msg", parsedType = "TRANSACTION", confidence = 1.0f, processed = true, bodyHash = 101, timestamp = 100L)
        val log2 = SmsLogEntity(id = 2, sender = "SenderY", message = "Msg", parsedType = "TRANSACTION", confidence = 1.0f, processed = true, bodyHash = 202, timestamp = 200L)
        val log3 = SmsLogEntity(id = 3, sender = "SenderX", message = "Msg", parsedType = "TRANSACTION", confidence = 1.0f, processed = true, bodyHash = 303, timestamp = 300L)

        smsLogDao.insert(log1)
        smsLogDao.insert(log2)
        smsLogDao.insert(log3)

        val lastTimestamp = smsLogDao.getLastTimestampForSender("SenderX")
        assertEquals(300L, lastTimestamp)

        val noTimestamp = smsLogDao.getLastTimestampForSender("NonExistentSender")
        assert(noTimestamp == null)
    }

    @Test
    @Throws(Exception::class)
    fun deleteAll() = runBlocking {
        val log1 = SmsLogEntity(id = 1, sender = "SenderA", message = "MsgA", parsedType = "TRANSACTION", confidence = 1.0f, processed = true, bodyHash = 123, timestamp = 1L)
        val log2 = SmsLogEntity(id = 2, sender = "SenderB", message = "MsgB", parsedType = "UNKNOWN", confidence = 0.5f, processed = false, bodyHash = 456, timestamp = 2L)
        smsLogDao.insert(log1)
        smsLogDao.insert(log2)

        smsLogDao.deleteAll()

        val allLogs = smsLogDao.getAllLogs()
        assert(allLogs.isEmpty())
    }
}
