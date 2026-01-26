package com.isetr.cupcake.data.repository

import android.content.Context
import com.isetr.cupcake.data.local.AppDatabase
import com.isetr.cupcake.data.local.CartEntity
import com.isetr.cupcake.session.SessionManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf

/**
 * CartRepository: Manages cart data with REACTIVE userId support.
 * 
 * KEY FIX: This repository now exposes a Flow that automatically
 * switches to the correct user's cart when the session changes.
 * 
 * When user X logs out and user Y logs in:
 * 1. SessionManager.activeUserIdFlow emits Y's userId
 * 2. flatMapLatest cancels X's cart query and starts Y's query
 * 3. UI instantly shows Y's cart (no stale data)
 * 
 * This fixes:
 * - Cart showing wrong user's items after login switch
 * - Cart not updating immediately when adding items
 * - Stale cart data persisting after logout
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CartRepository(context: Context) {
    
    private val cartDao = AppDatabase.getInstance(context).cartDao()
    private val sessionManager = SessionManager(context)
    
    /**
     * REACTIVE: Cart items that automatically switch when user changes.
     * 
     * Uses flatMapLatest to:
     * 1. Observe activeUserIdFlow (who is logged in)
     * 2. When userId changes, cancel old query, start new query for new userId
     * 3. Emit empty list if no user is logged in
     * 
     * UI should collect this Flow to always show correct user's cart.
     */
    val currentUserCartFlow: Flow<List<CartEntity>> = sessionManager.activeUserIdFlow
        .flatMapLatest { userId ->
            if (userId == SessionManager.NO_USER) {
                // No user logged in - emit empty cart
                flowOf(emptyList())
            } else {
                // Return Flow for this user's cart (auto-updates on changes)
                cartDao.getCartItemsForUserFlow(userId)
            }
        }
    
    /**
     * Get current user ID from session.
     * Use this when performing cart operations.
     */
    suspend fun getCurrentUserId(): Int {
        return sessionManager.getActiveUserId()
    }

    suspend fun addOrUpdateCartItem(cartItem: CartEntity) {
        val existingItem = cartDao.getCartItem(cartItem.productId, cartItem.userId)
        if (existingItem != null) {
            // Update quantity
            val newQuantity = existingItem.quantity + cartItem.quantity
            cartDao.updateQuantity(cartItem.productId, cartItem.userId, newQuantity)
        } else {
            // Insert new
            cartDao.insertCartItem(cartItem)
        }
        // No need to manually reload - Flow will auto-emit new data
    }

    suspend fun updateCartItem(cartItem: CartEntity) {
        cartDao.updateCartItem(cartItem)
    }

    suspend fun deleteCartItem(cartItem: CartEntity) {
        cartDao.deleteCartItem(cartItem)
    }

    suspend fun clearCart(userId: Int) {
        cartDao.clearCart(userId)
    }

    /**
     * One-shot query for specific user's cart.
     * Prefer using currentUserCartFlow for reactive updates.
     */
    suspend fun getCartItemsForUser(userId: Int): List<CartEntity> {
        return cartDao.getCartItemsForUser(userId)
    }
    
    /**
     * Get cart items as Flow for a specific user.
     * Use currentUserCartFlow instead for session-aware queries.
     */
    fun getCartItemsForUserFlow(userId: Int): Flow<List<CartEntity>> {
        return cartDao.getCartItemsForUserFlow(userId)
    }
}