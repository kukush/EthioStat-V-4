package com.ethiobalance.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SmsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(sms: SmsEntity)

    @Query("SELECT * FROM sms_events WHERE isSynced = 0 ORDER BY timestamp ASC")
    suspend fun getUnsyncedSms(): List<SmsEntity>

    @Update
    suspend fun update(sms: SmsEntity)

    @Query("UPDATE sms_events SET isSynced = 1 WHERE id IN (:ids)")
    suspend fun markAsSynced(ids: List<Int>)

    @Query("SELECT * FROM sms_events ORDER BY timestamp DESC")
    fun getAllSmsFlow(): Flow<List<SmsEntity>>
}
