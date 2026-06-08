package com.ethiobalance.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "balance_packages")
data class BalancePackageEntity(
    @PrimaryKey val id: String,
    val simId: String,
    val type: String, // internet, voice, sms, bonus, airtime, bank_balance
    val subType: String = "", // Night, Free, Bonus, Monthly, Daily, Weekly, Recurring, Normal
    val totalAmount: Double,
    val remainingAmount: Double,
    val unit: String, // MB, GB, MIN, SMS, ETB
    val expiryDate: Long,
    val isActive: Boolean,
    val source: String, // SMS, USSD, SYSTEM
    val lastUpdated: Long
)
