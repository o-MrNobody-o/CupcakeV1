package com.isetr.cupcake.data.repository

import android.content.Context
import android.util.Log
import com.isetr.cupcake.data.local.AppDatabase
import com.isetr.cupcake.data.local.CartEntity
import com.isetr.cupcake.data.network.CartItemDto
import com.isetr.cupcake.data.network.RetrofitClient

class CartRepository(context: Context) {
    private val cartDao = AppDatabase.getInstance(context).cartDao()
    private val api = RetrofitClient.api
    private val TAG = "CartRepository"

    suspend fun addOrUpdateCartItem(cartItem: CartEntity) {
        // 1. Sauvegarde locale immédiate (Room)
        val existingItem = cartDao.getCartItem(cartItem.productId, cartItem.userId)
        if (existingItem != null) {
            val newQuantity = existingItem.quantity + cartItem.quantity
            cartDao.updateQuantity(cartItem.productId, cartItem.userId, newQuantity)
        } else {
            cartDao.insertCartItem(cartItem)
        }

        // 2. Synchronisation avec Express.js (MySQL)
        try {
            val dto = CartItemDto(
                userId = cartItem.userId,
                productId = cartItem.productId,
                name = cartItem.name,
                price = cartItem.price,
                imageName = "default_pastry", // On peut ajuster selon votre backend
                quantity = cartItem.quantity
            )
            api.addToCartRemote(dto)
            Log.d(TAG, "Produit synchronisé sur MySQL")
        } catch (e: Exception) {
            Log.e(TAG, "Erreur synchro MySQL : ${e.message}")
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
        // On tente de récupérer les données fraîches depuis MySQL
        return try {
            val remoteItems = api.getCart(userId)
            if (remoteItems.isNotEmpty()) {
                // Optionnel : Mettre à jour Room avec les données distantes
                Log.d(TAG, "Articles récupérés depuis MySQL")
            }
            cartDao.getCartItemsForUser(userId) // Retourne la source locale (plus rapide)
        } catch (e: Exception) {
            cartDao.getCartItemsForUser(userId)
        }
    }
}
