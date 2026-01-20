package com.isetr.cupcake.data.local

import androidx.room.*

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

    // Get all cart items for a specific user
    @Query("SELECT * FROM cart_items WHERE userId = :userId")
    suspend fun getCartItemsForUser(userId: Int): List<CartEntity>

    // Clear all cart items for a user (optional)
    @Query("DELETE FROM cart_items WHERE userId = :userId")
    suspend fun clearCart(userId: Int)
}
