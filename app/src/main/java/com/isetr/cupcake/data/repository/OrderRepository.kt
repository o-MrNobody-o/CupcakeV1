package com.isetr.cupcake.data.repository

import android.content.Context
import com.isetr.cupcake.data.local.AppDatabase
import com.isetr.cupcake.data.local.OrderEntity

class OrderRepository(context: Context) {
    private val orderDao = AppDatabase.getInstance(context).orderDao()

    suspend fun insertOrder(order: OrderEntity) {
        orderDao.insertOrder(order)
    }

    suspend fun getOrdersByUser(userId: Int): List<OrderEntity> {
        return orderDao.getOrdersByUser(userId)
    }
}