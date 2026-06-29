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
        val package1 = BalancePackageEntity(id = "1", packageName = "Voice Package 1", balance = 100.0, expiryDate = 0L, packageType = "voice")
        val package2 = BalancePackageEntity(id = "2", packageName = "Internet Package 1", balance = 500.0, expiryDate = 0L, packageType = "internet")
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
        val package1 = BalancePackageEntity(id = "1", packageName = "Voice Package 1", balance = 100.0, expiryDate = 0L, packageType = "voice")
        balancePackageDao.insertOrUpdate(package1)

        val updatedPackage = package1.copy(balance = 150.0)
        balancePackageDao.insertOrUpdate(updatedPackage)

        val retrievedPackage = balancePackageDao.getPackageById("1")
        assert(retrievedPackage?.balance == 150.0)
    }

    @Test
    @Throws(Exception::class)
    fun getPackageById() = runBlocking {
        val package1 = BalancePackageEntity(id = "1", packageName = "Voice Package 1", balance = 100.0, expiryDate = 0L, packageType = "voice")
        balancePackageDao.insertOrUpdate(package1)

        val retrievedPackage = balancePackageDao.getPackageById("1")
        assert(retrievedPackage == package1)
    }

    @Test
    @Throws(Exception::class)
    fun deleteTelecomPackages() = runBlocking {
        val voicePackage = BalancePackageEntity(id = "1", packageName = "Voice Package", balance = 100.0, expiryDate = 0L, packageType = "voice")
        val internetPackage = BalancePackageEntity(id = "2", packageName = "Internet Package", balance = 200.0, expiryDate = 0L, packageType = "internet")
        val otherPackage = BalancePackageEntity(id = "3", packageName = "Other Package", balance = 50.0, expiryDate = 0L, packageType = "data") // 'data' is not a telecom package type for deletion here

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
        val package1 = BalancePackageEntity(id = "1", packageName = "Voice Package 1", balance = 100.0, expiryDate = 0L, packageType = "voice")
        val package2 = BalancePackageEntity(id = "2", packageName = "Internet Package 1", balance = 500.0, expiryDate = 0L, packageType = "internet")
        balancePackageDao.insertOrUpdate(package1)
        balancePackageDao.insertOrUpdate(package2)

        balancePackageDao.deleteAll()

        val allPackages = balancePackageDao.getAllPackages().first()
        assert(allPackages.isEmpty())
    }
}
