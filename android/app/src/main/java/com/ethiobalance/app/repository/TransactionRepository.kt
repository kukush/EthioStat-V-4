package com.ethiobalance.app.repository

import com.ethiobalance.app.data.TransactionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import com.ethiobalance.app.data.TransactionDao
import com.ethiobalance.app.data.TransactionSourceDao
import com.ethiobalance.app.data.SmsLogDao
import javax.inject.Inject

class TransactionRepository @Inject constructor(
    private val transactionDao: TransactionDao,
    private val transactionSourceDao: TransactionSourceDao,
    private val smsLogDao: SmsLogDao,
    val smsRepo: SmsRepository
) {

    fun getAllTransactions(): Flow<List<TransactionEntity>> = transactionDao.getAllTransactions()

    suspend fun getTotalByType(type: String): Double = withContext(Dispatchers.IO) {
        transactionDao.getTotalByType(type) ?: 0.0
    }

    suspend fun deleteAll() = withContext(Dispatchers.IO) {
        transactionDao.deleteAll()
        smsLogDao.deleteAll()
    }

    suspend fun insert(transaction: TransactionEntity) = withContext(Dispatchers.IO) {
        transactionDao.insert(transaction)
    }
}
