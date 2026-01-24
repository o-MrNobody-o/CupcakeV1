package com.isetr.cupcake.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PastryDao {
    @Query("SELECT * FROM pastries")
    fun getAllPastries(): Flow<List<Pastry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPastries(pastries: List<Pastry>)

    @Query("SELECT * FROM pastries WHERE id = :pastryId")
    suspend fun getPastryById(pastryId: String): Pastry?
}
