package com.ethiobalance.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "transaction_sources")
data class TransactionSourceEntity(
    @PrimaryKey
    val abbreviation: String,
    val name: String,
    val ussd: String,
    val senderId: String, // The numeric or alpha sender ID (e.g., "847" or "Telebirr")
    val isEnabled: Boolean = true,
    val lastUpdated: Long = System.currentTimeMillis()
)

@Dao
interface TransactionSourceDao {
    @Query("SELECT * FROM transaction_sources")
    fun getAllSources(): Flow<List<TransactionSourceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(source: TransactionSourceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(sources: List<TransactionSourceEntity>)

    @Query("DELETE FROM transaction_sources WHERE abbreviation = :abbreviation")
    suspend fun deleteByAbbreviation(abbreviation: String)

    @Query("SELECT senderId FROM transaction_sources WHERE isEnabled = 1")
    fun getEnabledSenderIds(): List<String>
}
