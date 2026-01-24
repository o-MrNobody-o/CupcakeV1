package com.isetr.cupcake.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface PastryDao {

    /**
     * Retrieve all pastries from the database.
     */
    @Query("SELECT * FROM pastries")
    suspend fun getAllPastries(): List<PastryEntity>

    /**
     * Insert or replace multiple pastries in the database.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(pastries: List<PastryEntity>)

    /**
     * Delete all pastries from the database.
     */
    @Query("DELETE FROM pastries")
    suspend fun clearAll()
}
