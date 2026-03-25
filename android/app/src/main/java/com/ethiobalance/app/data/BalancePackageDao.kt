package com.ethiobalance.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface BalancePackageDao {
    @Query("SELECT * FROM balance_packages")
    fun getAllPackages(): Flow<List<BalancePackageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(balancePackage: BalancePackageEntity)

    @Query("SELECT * FROM balance_packages WHERE id = :id LIMIT 1")
    suspend fun getPackageById(id: String): BalancePackageEntity?

    @Query("DELETE FROM balance_packages")
    suspend fun deleteAll()
}
