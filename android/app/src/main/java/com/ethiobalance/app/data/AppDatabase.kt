package com.ethiobalance.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

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
    version = 7,
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

    companion object {
        /**
         * Migration from version 6 to 7:
         * Replaces the old 4-bank default set (CBE, AWASH, DASHEN, BOA) with CBE + TELEBIRR only.
         * Only applies if the table contains exactly those 4 banks and nothing else.
         */
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Check current sources
                val cursor = database.query("SELECT abbreviation FROM transaction_sources")
                val currentSources = mutableSetOf<String>()
                while (cursor.moveToNext()) {
                    currentSources.add(cursor.getString(0))
                }
                cursor.close()

                // Old default set that should be replaced
                val oldDefaultSet = setOf("CBE", "AWASH", "DASHEN", "BOA")

                // Only migrate if user has exactly the old default set (hasn't customized)
                if (currentSources == oldDefaultSet) {
                    // Delete old defaults except CBE (which we'll keep and update)
                    database.execSQL("DELETE FROM transaction_sources WHERE abbreviation IN ('AWASH', 'DASHEN', 'BOA')")

                    // Update CBE with all sender variants
                    database.execSQL("""
                        UPDATE transaction_sources
                        SET senderId = '889,847,CBE,CBEBirr,CBEBIRR',
                            name = 'Commercial Bank of Ethiopia',
                            lastUpdated = ${System.currentTimeMillis()}
                        WHERE abbreviation = 'CBE'
                    """.trimIndent())

                    // Insert Telebirr with all sender variants
                    database.execSQL("""
                        INSERT INTO transaction_sources (abbreviation, name, ussd, senderId, isEnabled, lastUpdated)
                        VALUES ('TELEBIRR', 'Telebirr', '', '127,TELEBIRR,Telebirr', 1, ${System.currentTimeMillis()})
                    """.trimIndent())
                }
            }
        }
    }
}
