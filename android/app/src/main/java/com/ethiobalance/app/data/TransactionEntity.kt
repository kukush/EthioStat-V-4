package com.ethiobalance.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey val id: String,
    val type: String, // INCOME, EXPENSE
    val amount: Double,
    val category: String, // PURCHASE, GIFT_SENT, LOAN, REPAYMENT
    val source: String, // TELEBIRR, USSD, SMS
    val timestamp: Long,
    val reference: String?
)
