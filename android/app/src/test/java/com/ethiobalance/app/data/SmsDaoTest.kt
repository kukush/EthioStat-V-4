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
class SmsDaoTest {

    private lateinit var smsDao: SmsDao
    private lateinit var db: AppDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(
            context, AppDatabase::class.java).allowMainThreadQueries().build()
        smsDao = db.smsDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    @Throws(Exception::class)
    fun insertAndGetAllSms() = runBlocking {
        val sms1 = SmsEntity(id = 1, sender = "SenderA", message = "Message A", timestamp = 1L, isRead = false, isProcessed = false, isSynced = false)
        val sms2 = SmsEntity(id = 2, sender = "SenderB", message = "Message B", timestamp = 2L, isRead = false, isProcessed = false, isSynced = false)
        smsDao.insert(sms1)
        smsDao.insert(sms2)

        val allSms = smsDao.getAllSmsFlow().first()
        assert(allSms.size == 2)
        assert(allSms.map { it.sender }.containsAll(listOf("SenderA", "SenderB")))
    }

    @Test
    @Throws(Exception::class)
    fun getUnsyncedSms() = runBlocking {
        val sms1 = SmsEntity(id = 1, sender = "SenderA", message = "Message A", timestamp = 1L, isRead = false, isProcessed = false, isSynced = false)
        val sms2 = SmsEntity(id = 2, sender = "SenderB", message = "Message B", timestamp = 2L, isRead = false, isProcessed = false, isSynced = true)
        val sms3 = SmsEntity(id = 3, sender = "SenderC", message = "Message C", timestamp = 3L, isRead = false, isProcessed = false, isSynced = false)
        smsDao.insert(sms1)
        smsDao.insert(sms2)
        smsDao.insert(sms3)

        val unsyncedSms = smsDao.getUnsyncedSms()
        assert(unsyncedSms.size == 2)
        assert(unsyncedSms.map { it.sender }.containsAll(listOf("SenderA", "SenderC")))
    }

    @Test
    @Throws(Exception::class)
    fun updateSms() = runBlocking {
        val sms = SmsEntity(id = 1, sender = "SenderA", message = "Message A", timestamp = 1L, isRead = false, isProcessed = false, isSynced = false)
        smsDao.insert(sms)

        val updatedSms = sms.copy(isRead = true, isProcessed = true)
        smsDao.update(updatedSms)

        val retrievedSms = smsDao.getAllSmsFlow().first().first { it.id == 1 }
        assert(retrievedSms.isRead)
        assert(retrievedSms.isProcessed)
    }

    @Test
    @Throws(Exception::class)
    fun markAsSynced() = runBlocking {
        val sms1 = SmsEntity(id = 1, sender = "SenderA", message = "Message A", timestamp = 1L, isRead = false, isProcessed = false, isSynced = false)
        val sms2 = SmsEntity(id = 2, sender = "SenderB", message = "Message B", timestamp = 2L, isRead = false, isProcessed = false, isSynced = false)
        smsDao.insert(sms1)
        smsDao.insert(sms2)

        smsDao.markAsSynced(listOf(1))

        val unsyncedSms = smsDao.getUnsyncedSms()
        assert(unsyncedSms.size == 1)
        assert(unsyncedSms[0].id == 2)

        val allSms = smsDao.getAllSmsFlow().first()
        assert(allSms.first { it.id == 1 }.isSynced)
        assert(!allSms.first { it.id == 2 }.isSynced)
    }
}
