package com.ethiobalance.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        SmsEntity::class, 
        UssdEntity::class,
        BalancePackageEntity::class,
        TransactionEntity::class,
        SmsLogEntity::class,
        SimCardEntity::class,
        TransactionSourceEntity::class
    ], 
    version = 4, 
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun smsDao(): SmsDao
    abstract fun ussdDao(): UssdDao
    abstract fun balancePackageDao(): BalancePackageDao
    abstract fun transactionDao(): TransactionDao
    abstract fun smsLogDao(): SmsLogDao
    abstract fun simCardDao(): SimCardDao
    abstract fun transactionSourceDao(): TransactionSourceDao
}
