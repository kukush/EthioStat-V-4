package com.ethiobalance.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: TransactionEntity)
    
    @Query("SELECT EXISTS(SELECT 1 FROM transactions WHERE id = :id)")
    suspend fun existsById(id: String): Boolean

    @Query("SELECT EXISTS(SELECT 1 FROM transactions WHERE source = :source AND type = :type AND amount = :amount AND ABS(timestamp - :timestamp) < :windowMs)")
    suspend fun existsNearDuplicate(source: String, type: String, amount: Double, timestamp: Long, windowMs: Long): Boolean

    @Query("SELECT SUM(amount) FROM transactions WHERE type = :type")
    suspend fun getTotalByType(type: String): Double?

    @Query("SELECT COUNT(*) FROM transactions WHERE source = :source")
    suspend fun countBySource(source: String): Int

    @Query("DELETE FROM transactions")
    suspend fun deleteAll()
}
