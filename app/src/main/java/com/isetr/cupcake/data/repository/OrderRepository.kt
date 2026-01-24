package com.isetr.cupcake.data.repository

import android.content.Context
import com.isetr.cupcake.data.local.AppDatabase
import com.isetr.cupcake.data.local.OrderEntity

class OrderRepository(context: Context) {
    private val database = AppDatabase.getInstance(context)
    private val orderDao = database.orderDao()
    private val userDao = database.userDao()

    suspend fun placeOrder(order: OrderEntity): Long {
        return try {
            orderDao.completeOrderProcess(order)
        } catch (e: Exception) {
            -1L
        }
    }

    /**
     * Récupère la dernière commande de l'utilisateur actuellement connecté.
     */
    suspend fun getLastOrderForActiveSession(): OrderEntity? {
        val activeUser = userDao.getActiveUser()
        return if (activeUser != null) {
            orderDao.getLastOrderByUser(activeUser.id)
        } else {
            null
        }
    }

    /**
     * Récupère toutes les commandes de l'utilisateur actuellement connecté.
     */
    suspend fun getOrdersForActiveSession(): List<OrderEntity> {
        val activeUser = userDao.getActiveUser()
        return if (activeUser != null) {
            orderDao.getAllOrdersByUser(activeUser.id)
        } else {
            emptyList()
        }
    }

    suspend fun getAllOrders(userId: Int): List<OrderEntity> {
        return orderDao.getAllOrdersByUser(userId)
    }

    suspend fun updateOrderStatus(order: OrderEntity) {
        orderDao.updateOrder(order)
    }
}
