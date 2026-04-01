package com.ethiobalance.app.repository

import android.content.Context
import com.ethiobalance.app.data.AppDatabase
import com.ethiobalance.app.data.TransactionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class TransactionRepository(private val context: Context) {

    private val db = AppDatabase.getDatabase(context)
    private val dao = db.transactionDao()
    val smsRepo = SmsRepository(context)

    fun getAllTransactions(): Flow<List<TransactionEntity>> = dao.getAllTransactions()

    suspend fun getTotalByType(type: String): Double = withContext(Dispatchers.IO) {
        dao.getTotalByType(type) ?: 0.0
    }

    suspend fun deleteAll() = withContext(Dispatchers.IO) {
        dao.deleteAll()
    }

    suspend fun insert(transaction: TransactionEntity) = withContext(Dispatchers.IO) {
        dao.insert(transaction)
    }
}
