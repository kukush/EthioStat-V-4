package com.ethiobalance.app.di

import android.content.Context
import androidx.room.Room
import com.ethiobalance.app.data.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "ethio_balance_db"
        )
        .addMigrations(AppDatabase.MIGRATION_6_7)
        .build()
    }

    @Provides
    fun provideSmsDao(db: AppDatabase): SmsDao = db.smsDao()

    @Provides
    fun provideUssdDao(db: AppDatabase): UssdDao = db.ussdDao()

    @Provides
    fun provideBalancePackageDao(db: AppDatabase): BalancePackageDao = db.balancePackageDao()

    @Provides
    fun provideTransactionDao(db: AppDatabase): TransactionDao = db.transactionDao()

    @Provides
    fun provideSmsLogDao(db: AppDatabase): SmsLogDao = db.smsLogDao()

    @Provides
    fun provideSimCardDao(db: AppDatabase): SimCardDao = db.simCardDao()

    @Provides
    fun provideTransactionSourceDao(db: AppDatabase): TransactionSourceDao = db.transactionSourceDao()
}
