package com.ethiobalance.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SimCardDao {
    @Query("SELECT * FROM sim_cards")
    fun getAllSimCards(): Flow<List<SimCardEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(sim: SimCardEntity)

    @Update
    suspend fun update(sim: SimCardEntity)

    @Query("DELETE FROM sim_cards WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("UPDATE sim_cards SET isPrimary = 0")
    suspend fun clearPrimary()

    @Query("UPDATE sim_cards SET isPrimary = 1 WHERE id = :id")
    suspend fun setPrimary(id: String)

    @Query("SELECT * FROM sim_cards WHERE isPrimary = 1 LIMIT 1")
    suspend fun getPrimarySim(): SimCardEntity?
}
