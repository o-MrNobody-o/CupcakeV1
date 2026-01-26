package com.isetr.cupcake.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * CartDao: Data Access Object for cart operations.
 * 
 * KEY FIX: Added Flow-based queries that automatically emit new data
 * when the underlying table changes. This ensures the UI updates
 * instantly when cart items are added/removed/modified.
 */
@Dao
interface CartDao {

    // Insert a new cart item
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCartItem(cartItem: CartEntity)

    // Update the quantity of a cart item
    @Update
    suspend fun updateCartItem(cartItem: CartEntity)

    // Delete a specific cart item
    @Delete
    suspend fun deleteCartItem(cartItem: CartEntity)

    // Get all cart items for a specific user (one-shot)
    @Query("SELECT * FROM cart_items WHERE userId = :userId")
    suspend fun getCartItemsForUser(userId: Int): List<CartEntity>
    
    /**
     * REACTIVE: Get all cart items for a specific user as Flow.
     * This automatically emits new values whenever cart data changes.
     * Use this in ViewModels for real-time UI updates.
     */
    @Query("SELECT * FROM cart_items WHERE userId = :userId")
    fun getCartItemsForUserFlow(userId: Int): Flow<List<CartEntity>>

    // Get a specific cart item by productId and userId
    @Query("SELECT * FROM cart_items WHERE productId = :productId AND userId = :userId")
    suspend fun getCartItem(productId: String, userId: Int): CartEntity?

    // Update quantity for a specific cart item
    @Query("UPDATE cart_items SET quantity = :newQuantity WHERE productId = :productId AND userId = :userId")
    suspend fun updateQuantity(productId: String, userId: Int, newQuantity: Int)

    // Clear all cart items for a specific user
    @Query("DELETE FROM cart_items WHERE userId = :userId")
    suspend fun clearCart(userId: Int)
}
