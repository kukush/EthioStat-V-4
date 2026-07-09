package com.ethiobalance.app.data

import androidx.sqlite.db.SupportSQLiteDatabase
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

class SourceMigrationTest {

    @Test
    fun migration_8_9_insertsDefaultSources() {
        val mockDb = mockk<SupportSQLiteDatabase>(relaxed = true)

        SourceMigration.MIGRATION_8_9.migrate(mockDb)

        verify {
            mockDb.execSQL(match { it.contains("INSERT OR IGNORE INTO transaction_sources") })
            mockDb.execSQL(match { it.contains("'CBE'") })
            mockDb.execSQL(match { it.contains("'Telebirr'") })
        }
    }
}
