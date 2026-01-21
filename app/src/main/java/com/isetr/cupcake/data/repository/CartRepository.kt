package com.isetr.cupcake.data.repository

import android.content.Context
import com.isetr.cupcake.data.local.AppDatabase
import com.isetr.cupcake.data.local.CartEntity

class CartRepository(context: Context) {
    private val cartDao = AppDatabase.getInstance(context).cartDao()

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

    suspend fun getCartItemsForUser(userId: Int): List<CartEntity> {
        return cartDao.getCartItemsForUser(userId)
    }
}