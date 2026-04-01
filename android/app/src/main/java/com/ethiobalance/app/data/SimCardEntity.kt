package com.ethiobalance.app.data

import androidx.room.*

@Entity(tableName = "sim_cards")
data class SimCardEntity(
    @PrimaryKey
    val id: String,
    val slotIndex: Int,
    val carrierName: String,
    val phoneNumber: String,
    val isPrimary: Boolean = false,
    val lastUpdated: Long = System.currentTimeMillis()
)
