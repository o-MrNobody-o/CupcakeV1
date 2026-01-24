package com.isetr.cupcake.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.isetr.cupcake.data.local.OrderEntity
import com.isetr.cupcake.data.repository.CartRepository
import com.isetr.cupcake.data.repository.OrderRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class OrderViewModel(application: Application) : AndroidViewModel(application) {

    private val orderRepository = OrderRepository(application.applicationContext)
    private val cartRepository = CartRepository(application.applicationContext)

    // LiveData for the user's orders
    private val _orders = MutableLiveData<List<OrderEntity>>()
    val orders: LiveData<List<OrderEntity>> = _orders

    // LiveData for messages (e.g., success/error toasts)
    private val _message = MutableLiveData<String?>()
    val message: LiveData<String?> = _message

    fun placeOrder(userId: Int) {
    viewModelScope.launch(Dispatchers.IO) {
        try {
            val cartItems = cartRepository.getCartItemsForUser(userId)
            if (cartItems.isEmpty()) {
                _message.postValue("Cart is empty")
                return@launch
            }

            val totalPrice = cartItems.sumOf { it.price * it.quantity }
            val orderDate = System.currentTimeMillis()
            val deliveryDate = orderDate + (2 * 24 * 60 * 60 * 1000L)

            val order = OrderEntity(
                userId = userId,
                orderDate = orderDate,
                deliveryDate = deliveryDate,
                totalPrice = totalPrice
            )

            orderRepository.insertOrder(order)
            cartRepository.clearCart(userId)

            _message.postValue("Order placed successfully")

            // 🔹 Reload orders for this user
            val updatedOrders = orderRepository.getOrdersByUser(userId)
            _orders.postValue(updatedOrders)

        } catch (e: Exception) {
            _message.postValue("Failed to place order: ${e.message}")
        }
    }
}


    fun getOrdersByUser(userId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val userOrders = orderRepository.getOrdersByUser(userId)
                _orders.postValue(userOrders)
            } catch (e: Exception) {
                _message.postValue("Failed to load orders: ${e.message}")
            }
        }
    }

    fun clearMessage() {
        _message.value = null
    }
}