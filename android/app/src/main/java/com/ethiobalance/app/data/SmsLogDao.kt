package com.ethiobalance.app.data

import androidx.room.*

@Dao
interface SmsLogDao {
    @Insert
    suspend fun insert(log: SmsLogEntity)

    @Query("SELECT * FROM sms_logs ORDER BY timestamp ASC")
    suspend fun getAllLogs(): List<SmsLogEntity>

    @Update
    suspend fun update(log: SmsLogEntity)

    @Query("SELECT EXISTS(SELECT 1 FROM sms_logs WHERE sender = :sender AND timestamp = :timestamp AND bodyHash = :bodyHash LIMIT 1)")
    suspend fun existsByHash(sender: String, timestamp: Long, bodyHash: Int): Boolean

    @Query("SELECT MAX(timestamp) FROM sms_logs WHERE sender = :sender")
    suspend fun getLastTimestampForSender(sender: String): Long?
}
