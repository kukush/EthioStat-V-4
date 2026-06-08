package com.ethiobalance.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ussd_events")
data class UssdEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val request: String,
    val response: String,
    val timestamp: Long,
    val simSlot: Int
)
