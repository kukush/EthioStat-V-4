package com.ethiobalance.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UssdDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(ussd: UssdEntity)

    @Query("SELECT * FROM ussd_events ORDER BY timestamp DESC")
    suspend fun getAllUssdEvents(): List<UssdEntity>

    @Query("SELECT * FROM ussd_events ORDER BY timestamp DESC")
    fun getAllUssdEventsFlow(): Flow<List<UssdEntity>>
}
