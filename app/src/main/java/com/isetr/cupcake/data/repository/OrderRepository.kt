package com.isetr.cupcake.data.repository

import android.content.Context
import android.util.Log
import com.isetr.cupcake.data.local.AppDatabase
import com.isetr.cupcake.data.local.OrderEntity
import com.isetr.cupcake.data.network.RetrofitClient

class OrderRepository(context: Context) {
    private val database = AppDatabase.getInstance(context)
    private val orderDao = database.orderDao()
    private val userDao = database.userDao()
    private val api = RetrofitClient.api

    suspend fun placeOrder(order: OrderEntity): Long {
        return try {
            orderDao.completeOrderProcess(order)
        } catch (e: Exception) {
            -1L
        }
    }

    // --- NOUVEAU : ENVOYER UN AVIS ---
    suspend fun submitOrderReview(orderId: Int, review: String): Boolean {
        return try {
            // 1. Envoyer au serveur Express
            val reviewData = mapOf(
                "orderId" to orderId.toString(),
                "review" to review
            )
            val response = api.submitReview(reviewData)
            
            if (response.success == true) {
                // 2. Mettre à jour localement dans Room
                val order = orderDao.getOrderById(orderId)
                order?.let {
                    val updatedOrder = it.copy(review = review)
                    orderDao.updateOrder(updatedOrder)
                }
                true
            } else false
        } catch (e: Exception) {
            Log.e("OrderRepository", "Erreur avis: ${e.message}")
            false
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

    suspend fun getLastOrderForActiveSession(): OrderEntity? {
        val activeUser = userDao.getActiveUser()
        return if (activeUser != null) {
            orderDao.getLastOrderByUser(activeUser.id)
        } else null
    }

    suspend fun getAllOrders(userId: Int): List<OrderEntity> {
        return try {
            // Optionnel : on pourrait synchroniser avec le serveur ici
            orderDao.getAllOrdersByUser(userId)
        } catch (e: Exception) {
            orderDao.getAllOrdersByUser(userId)
        }
    }

    suspend fun updateOrderStatus(order: OrderEntity) {
        orderDao.updateOrder(order)
    }
}
