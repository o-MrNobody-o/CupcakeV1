package com.isetr.cupcake.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface OrderDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: OrderEntity)

    @Query("SELECT * FROM orders WHERE userId = :userId ORDER BY orderDate DESC")
    suspend fun getOrdersByUser(userId: Int): List<OrderEntity>
}