package com.isetr.cupcake.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface OrderDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: OrderEntity)

    @Query("SELECT * FROM orders WHERE userId = :userId ORDER BY orderDate DESC")
    suspend fun getOrdersByUser(userId: Int): List<OrderEntity>
    
    /**
     * Stream orders for a user in real-time (Flow).
     * Use this in ViewModels to auto-update UI when orders change.
     */
    @Query("SELECT * FROM orders WHERE userId = :userId ORDER BY orderDate DESC")
    fun getOrdersByUserFlow(userId: Int): Flow<List<OrderEntity>>
    
    /**
     * Delete all orders for a user (called when account is deleted).
     */
    @Query("DELETE FROM orders WHERE userId = :userId")
    suspend fun deleteOrdersForUser(userId: Int)
}