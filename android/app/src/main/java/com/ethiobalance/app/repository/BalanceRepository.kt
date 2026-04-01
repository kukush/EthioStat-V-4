package com.ethiobalance.app.repository

import android.content.Context
import com.ethiobalance.app.data.AppDatabase
import com.ethiobalance.app.data.BalancePackageEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class BalanceRepository(context: Context) {

    private val dao = AppDatabase.getDatabase(context).balancePackageDao()

    fun getAllPackages(): Flow<List<BalancePackageEntity>> = dao.getAllPackages()

    suspend fun insertOrUpdate(pkg: BalancePackageEntity) = withContext(Dispatchers.IO) {
        dao.insertOrUpdate(pkg)
    }

    suspend fun deleteAll() = withContext(Dispatchers.IO) {
        dao.deleteAll()
    }
}
