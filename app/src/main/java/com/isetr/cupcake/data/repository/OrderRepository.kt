package com.isetr.cupcake.data.repository

import android.content.Context
import com.isetr.cupcake.data.local.AppDatabase
import com.isetr.cupcake.data.local.OrderEntity
import com.isetr.cupcake.session.SessionManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf

/**
 * OrderRepository: Manages order data with REACTIVE userId support.
 * 
 * KEY FIX: This repository now exposes a Flow that automatically
 * switches to the correct user's orders when the session changes.
 * 
 * When user X logs out and user Y logs in:
 * 1. SessionManager.activeUserIdFlow emits Y's userId
 * 2. flatMapLatest cancels X's orders query and starts Y's query
 * 3. UI instantly shows Y's orders (no stale data from X)
 * 
 * This fixes:
 * - Orders from previous user appearing briefly after login switch
 * - Stale order data persisting after logout
 */
@OptIn(ExperimentalCoroutinesApi::class)
class OrderRepository(context: Context) {
    
    private val orderDao = AppDatabase.getInstance(context).orderDao()
    private val sessionManager = SessionManager(context)
    
    /**
     * REACTIVE: Orders that automatically switch when user changes.
     * 
     * Uses flatMapLatest to:
     * 1. Observe activeUserIdFlow (who is logged in)
     * 2. When userId changes, cancel old query, start new query for new userId
     * 3. Emit empty list if no user is logged in
     * 
     * UI should collect this Flow to always show correct user's orders.
     */
    val currentUserOrdersFlow: Flow<List<OrderEntity>> = sessionManager.activeUserIdFlow
        .flatMapLatest { userId ->
            if (userId == SessionManager.NO_USER) {
                // No user logged in - emit empty orders
                flowOf(emptyList())
            } else {
                // Return Flow for this user's orders (auto-updates on changes)
                orderDao.getOrdersByUserFlow(userId)
            }
        }
    
    /**
     * Get current user ID from session.
     * Use this when performing order operations.
     */
    suspend fun getCurrentUserId(): Int {
        return sessionManager.getActiveUserId()
    }

    suspend fun insertOrder(order: OrderEntity) {
        orderDao.insertOrder(order)
        // No need to manually reload - Flow will auto-emit new data
    }

    /**
     * One-shot query for specific user's orders.
     * Prefer using currentUserOrdersFlow for reactive updates.
     */
    suspend fun getOrdersByUser(userId: Int): List<OrderEntity> {
        return orderDao.getOrdersByUser(userId)
    }
    
    /**
     * Get orders as Flow for a specific user.
     * Use currentUserOrdersFlow instead for session-aware queries.
     */
    fun getOrdersByUserFlow(userId: Int): Flow<List<OrderEntity>> {
        return orderDao.getOrdersByUserFlow(userId)
    }
}