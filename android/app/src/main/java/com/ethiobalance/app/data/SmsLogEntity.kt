package com.ethiobalance.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sms_logs")
data class SmsLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sender: String,
    val message: String,
    val parsedType: String?, // FULL_BALANCE, DELTA, TRANSACTION, UNKNOWN
    val confidence: Float,
    val processed: Boolean,
    val timestamp: Long
)
