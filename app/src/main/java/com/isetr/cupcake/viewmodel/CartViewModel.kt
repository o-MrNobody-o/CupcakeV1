package com.isetr.cupcake.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.isetr.cupcake.data.local.CartEntity
import com.isetr.cupcake.data.local.OrderEntity
import com.isetr.cupcake.data.repository.CartRepository
import com.isetr.cupcake.data.repository.OrderRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

sealed class OrderState {
    object Idle : OrderState()
    object Loading : OrderState()
    object Success : OrderState()
    data class Error(val message: String) : OrderState()
}

class CartViewModel(application: Application) : AndroidViewModel(application) {
    private val orderRepository = OrderRepository(application)
    private val cartRepository = CartRepository(application.applicationContext)

    private val _orderState = MutableLiveData<OrderState>(OrderState.Idle)
    val orderState: LiveData<OrderState> = _orderState

    // LiveData for the user's cart items
    private val _cartItems = MutableLiveData<List<CartEntity>>()
    val cartItems: LiveData<List<CartEntity>> = _cartItems

    // Load cart items for a specific user
    fun loadCart(userId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val items = cartRepository.getCartItemsForUser(userId)
            _cartItems.postValue(items)
        }
    }

    // Update an existing cart item
    fun updateCartItem(cartItem: CartEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            cartRepository.updateCartItem(cartItem)
            loadCart(cartItem.userId)
        }
    }

    // Remove an item from the cart
    fun removeCartItem(cartItem: CartEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            cartRepository.deleteCartItem(cartItem)
            loadCart(cartItem.userId)
        }
    }

    // Clear entire cart for a user
    fun clearCart(userId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            cartRepository.clearCart(userId)
            loadCart(userId)
        }
    }

    fun checkout(userId: Int, total: Double, paymentMethod: String, cardNumber: String?, shippingAddress: String) {
        viewModelScope.launch {
            _orderState.value = OrderState.Loading
            try {
                val order = OrderEntity(
                    userId = userId,
                    totalAmount = total,
                    paymentMethod = paymentMethod,
                    cardNumber = cardNumber,
                    shippingAddress = shippingAddress
                )
                val result = orderRepository.placeOrder(order)
                if (result != -1L) {
                    _orderState.value = OrderState.Success
                    loadCart(userId) // Refresh cart (should be empty now)
                } else {
                    _orderState.value = OrderState.Error("Échec de la commande")
                }
            } catch (e: Exception) {
                _orderState.value = OrderState.Error(e.message ?: "Une erreur est survenue")
            }
        }
    }

    fun resetOrderState() {
        _orderState.value = OrderState.Idle
    }
}
