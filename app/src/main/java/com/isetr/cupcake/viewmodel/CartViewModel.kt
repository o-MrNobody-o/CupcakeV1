package com.isetr.cupcake.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.isetr.cupcake.data.local.AppDatabase
import com.isetr.cupcake.data.local.CartEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CartViewModel(application: Application) : AndroidViewModel(application) {

    private val cartDao = AppDatabase.getInstance(application).cartDao()

    // LiveData for the user's cart items
    private val _cartItems = MutableLiveData<List<CartEntity>>()
    val cartItems: LiveData<List<CartEntity>> = _cartItems

    // Load cart items for a specific user
    fun loadCart(userId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val items = cartDao.getCartItemsForUser(userId)
            _cartItems.postValue(items)
        }
    }

    // Add a new item to the cart
    fun addToCart(cartItem: CartEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            cartDao.insertCartItem(cartItem)
            // Reload cart for the same user
            loadCart(cartItem.userId)
        }
    }

    // Update an existing cart item
    fun updateCartItem(cartItem: CartEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            cartDao.updateCartItem(cartItem)
            loadCart(cartItem.userId)
        }
    }

    // Remove an item from the cart
    fun removeCartItem(cartItem: CartEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            cartDao.deleteCartItem(cartItem)
            loadCart(cartItem.userId)
        }
    }

    // Clear entire cart for a user
    fun clearCart(userId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            cartDao.clearCart(userId)
            loadCart(userId)
        }
    }
}
