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
class BalancePackageDaoTest {

    private lateinit var balancePackageDao: BalancePackageDao
    private lateinit var db: AppDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(
            context, AppDatabase::class.java).allowMainThreadQueries().build()
        balancePackageDao = db.balancePackageDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    @Throws(Exception::class)
    fun insertAndGetAllPackages() = runBlocking {
        val package1 = BalancePackageEntity(id = "1", simId = "sim1", type = "voice", subType = "Normal", totalAmount = 100.0, remainingAmount = 100.0, unit = "MIN", expiryDate = 0L, isActive = true, source = "SMS", lastUpdated = 0L)
        val package2 = BalancePackageEntity(id = "2", simId = "sim1", type = "internet", subType = "Normal", totalAmount = 500.0, remainingAmount = 500.0, unit = "MB", expiryDate = 0L, isActive = true, source = "SMS", lastUpdated = 0L)
        balancePackageDao.insertOrUpdate(package1)
        balancePackageDao.insertOrUpdate(package2)

        val allPackages = balancePackageDao.getAllPackages().first()
        assert(allPackages.size == 2)
        assert(allPackages.contains(package1))
        assert(allPackages.contains(package2))
    }

    @Test
    @Throws(Exception::class)
    fun updatePackage() = runBlocking {
        val package1 = BalancePackageEntity(id = "1", simId = "sim1", type = "voice", subType = "Normal", totalAmount = 100.0, remainingAmount = 100.0, unit = "MIN", expiryDate = 0L, isActive = true, source = "SMS", lastUpdated = 0L)
        balancePackageDao.insertOrUpdate(package1)

        val updatedPackage = package1.copy(remainingAmount = 150.0)
        balancePackageDao.insertOrUpdate(updatedPackage)

        val retrievedPackage = balancePackageDao.getPackageById("1")
        assert(retrievedPackage?.remainingAmount == 150.0)
    }

    @Test
    @Throws(Exception::class)
    fun getPackageById() = runBlocking {
        val package1 = BalancePackageEntity(id = "1", simId = "sim1", type = "voice", subType = "Normal", totalAmount = 100.0, remainingAmount = 100.0, unit = "MIN", expiryDate = 0L, isActive = true, source = "SMS", lastUpdated = 0L)
        balancePackageDao.insertOrUpdate(package1)

        val retrievedPackage = balancePackageDao.getPackageById("1")
        assert(retrievedPackage == package1)
    }

    @Test
    @Throws(Exception::class)
    fun deleteTelecomPackages() = runBlocking {
        val voicePackage = BalancePackageEntity(id = "1", simId = "sim1", type = "voice", subType = "Normal", totalAmount = 100.0, remainingAmount = 100.0, unit = "MIN", expiryDate = 0L, isActive = true, source = "SMS", lastUpdated = 0L)
        val internetPackage = BalancePackageEntity(id = "2", simId = "sim1", type = "internet", subType = "Normal", totalAmount = 200.0, remainingAmount = 200.0, unit = "MB", expiryDate = 0L, isActive = true, source = "SMS", lastUpdated = 0L)
        val otherPackage = BalancePackageEntity(id = "3", simId = "sim1", type = "data", subType = "Normal", totalAmount = 50.0, remainingAmount = 50.0, unit = "MB", expiryDate = 0L, isActive = true, source = "SMS", lastUpdated = 0L)

        balancePackageDao.insertOrUpdate(voicePackage)
        balancePackageDao.insertOrUpdate(internetPackage)
        balancePackageDao.insertOrUpdate(otherPackage)

        balancePackageDao.deleteTelecomPackages()

        val remainingPackages = balancePackageDao.getAllPackages().first()
        assert(remainingPackages.size == 1)
        assert(remainingPackages.contains(otherPackage))
    }

    @Test
    @Throws(Exception::class)
    fun deleteAll() = runBlocking {
        val package1 = BalancePackageEntity(id = "1", simId = "sim1", type = "voice", subType = "Normal", totalAmount = 100.0, remainingAmount = 100.0, unit = "MIN", expiryDate = 0L, isActive = true, source = "SMS", lastUpdated = 0L)
        val package2 = BalancePackageEntity(id = "2", simId = "sim1", type = "internet", subType = "Normal", totalAmount = 500.0, remainingAmount = 500.0, unit = "MB", expiryDate = 0L, isActive = true, source = "SMS", lastUpdated = 0L)
        balancePackageDao.insertOrUpdate(package1)
        balancePackageDao.insertOrUpdate(package2)

        balancePackageDao.deleteAll()

        val allPackages = balancePackageDao.getAllPackages().first()
        assert(allPackages.isEmpty())
    }
}
