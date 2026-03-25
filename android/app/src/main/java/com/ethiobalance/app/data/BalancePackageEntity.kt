package com.ethiobalance.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "balance_packages")
data class BalancePackageEntity(
    @PrimaryKey val id: String,
    val simId: String,
    val type: String, // DATA_AIRTIME, DATA_NIGHT, VOICE, SMS, BONUS
    val totalAmount: Double,
    val remainingAmount: Double,
    val unit: String, // MB, GB, MIN, SMS, BIRR
    val expiryDate: Long,
    val isActive: Boolean,
    val source: String, // SMS, USSD, SYSTEM
    val lastUpdated: Long
)
