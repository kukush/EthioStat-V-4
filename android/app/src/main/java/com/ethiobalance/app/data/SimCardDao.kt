package com.ethiobalance.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SimCardDao {
    @Query("SELECT * FROM sim_cards")
    fun getAllSims(): Flow<List<SimCardEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(sim: SimCardEntity)

    @Update
    suspend fun update(sim: SimCardEntity)

    @Query("UPDATE sim_cards SET isPrimary = 0")
    suspend fun clearPrimaryStatus()

    @Query("UPDATE sim_cards SET isPrimary = :isPrimary WHERE id = :id")
    suspend fun setPrimaryStatus(id: String, isPrimary: Boolean)

    @Query("SELECT * FROM sim_cards WHERE isPrimary = 1 LIMIT 1")
    fun getPrimarySim(): SimCardEntity?
}
