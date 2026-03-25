package com.ethiobalance.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sim_cards")
data class SimCardEntity(
    @PrimaryKey val id: String,
    val phoneNumber: String,
    val carrier: String,
    val isActive: Boolean
)
