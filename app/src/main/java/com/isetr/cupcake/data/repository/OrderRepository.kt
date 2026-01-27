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

    /**
     * Envoie un avis. Sauvegarde localement même si le serveur est indisponible.
     */
    suspend fun submitOrderReview(orderId: Int, review: String): Boolean {
        // 1. MISE À JOUR LOCALE IMMÉDIATE (ROOM)
        // Cela garantit que l'utilisateur voit son avis même sans internet
        try {
            val order = orderDao.getOrderById(orderId)
            order?.let {
                val updatedOrder = it.copy(review = review)
                orderDao.updateOrder(updatedOrder)
            }
        } catch (e: Exception) {
            Log.e("OrderRepository", "Erreur Room Review: ${e.message}")
            return false
        }

        // 2. TENTATIVE DE SYNCHRONISATION AVEC LE SERVEUR
        return try {
            val reviewData = mapOf(
                "orderId" to orderId.toString(),
                "review" to review
            )
            val response = api.submitReview(reviewData)
            response.success == true
        } catch (e: Exception) {
            // Si le serveur est injoignable, on renvoie true quand même 
            // car la sauvegarde locale (Room) a réussi !
            Log.w("OrderRepository", "Serveur injoignable, avis gardé en local uniquement")
            true 
        }
    }

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
        return orderDao.getAllOrdersByUser(userId)
    }

    suspend fun updateOrderStatus(order: OrderEntity) {
        orderDao.updateOrder(order)
    }
}
