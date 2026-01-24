package com.isetr.cupcake.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update

@Dao
interface OrderDao {
    @Insert
    suspend fun insertOrder(order: OrderEntity): Long

    @Query("DELETE FROM cart_items WHERE userId = :userId")
    suspend fun clearCart(userId: Int)

    @Transaction
    suspend fun completeOrderProcess(order: OrderEntity): Long {
        val id = insertOrder(order)
        clearCart(order.userId)
        return id
    }

    @Query("SELECT * FROM orders WHERE orderId = :orderId")
    suspend fun getOrderById(orderId: Int): OrderEntity?

    @Query("SELECT * FROM orders WHERE userId = :userId ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLastOrderByUser(userId: Int): OrderEntity?

    @Query("SELECT * FROM orders WHERE userId = :userId ORDER BY timestamp DESC")
    suspend fun getAllOrdersByUser(userId: Int): List<OrderEntity>

    @Update
    suspend fun updateOrder(order: OrderEntity)
}
