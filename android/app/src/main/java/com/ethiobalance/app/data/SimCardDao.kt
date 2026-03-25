package com.ethiobalance.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SimCardDao {
    @Query("SELECT * FROM sim_cards")
    fun getAllSimCards(): Flow<List<SimCardEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(simCard: SimCardEntity)

    @Delete
    suspend fun delete(simCard: SimCardEntity)
}
