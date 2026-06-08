package com.ethiobalance.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sms_events")
data class SmsEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val sender: String,
    val body: String,
    val timestamp: Long,
    val simSlot: Int,
    val isSynced: Boolean = false
)
