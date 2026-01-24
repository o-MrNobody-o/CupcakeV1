package com.isetr.cupcake.data.repository

import android.content.Context
import com.isetr.cupcake.data.local.AppDatabase
import com.isetr.cupcake.data.local.OrderEntity

class OrderRepository(context: Context) {
    private val database = AppDatabase.getInstance(context)
    private val orderDao = database.orderDao()

    suspend fun placeOrder(order: OrderEntity): Long {
        return try {
            orderDao.completeOrderProcess(order)
        } catch (e: Exception) {
            -1L
        }
    }

    suspend fun getLastOrder(userId: Int): OrderEntity? {
        return orderDao.getLastOrderByUser(userId)
    }

    suspend fun getAllOrders(userId: Int): List<OrderEntity> {
        return orderDao.getAllOrdersByUser(userId)
    }

    suspend fun updateOrderStatus(order: OrderEntity) {
        orderDao.updateOrder(order)
    }
}
