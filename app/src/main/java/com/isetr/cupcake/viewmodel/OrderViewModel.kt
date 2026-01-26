package com.isetr.cupcake.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.isetr.cupcake.data.local.OrderEntity
import com.isetr.cupcake.data.repository.CartRepository
import com.isetr.cupcake.data.repository.OrderRepository
import com.isetr.cupcake.session.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * OrderViewModel: Manages order state with REACTIVE user session support.
 * 
 * KEY FIX: Orders data is now reactive to session changes.
 * 
 * Previous bug:
 * - getOrdersByUser(userId) was called once, data stayed stale when user switched
 * - OrdersFragment showed previous user's orders briefly after login switch
 * 
 * New behavior:
 * - orders is a LiveData backed by Flow that auto-switches when user changes
 * - No manual getOrdersByUser() needed for display - data updates automatically
 * - When user logs out, orders shows empty instantly
 * - When new user logs in, their orders appear instantly
 */
class OrderViewModel(application: Application) : AndroidViewModel(application) {

    private val orderRepository = OrderRepository(application.applicationContext)
    private val cartRepository = CartRepository(application.applicationContext)
    private val sessionManager = SessionManager(application.applicationContext)

    /**
     * REACTIVE: Orders that automatically update when:
     * 1. New order is placed
     * 2. User logs out (shows empty)
     * 3. Different user logs in (shows their orders)
     * 
     * No need to call getOrdersByUser() - this auto-updates!
     */
    val orders: LiveData<List<OrderEntity>> = orderRepository.currentUserOrdersFlow.asLiveData()
    
    /**
     * Current user ID from session.
     * Use this to get the userId for order operations.
     */
    val currentUserId: LiveData<Int> = sessionManager.activeUserIdFlow.asLiveData()

    // LiveData for messages (e.g., success/error toasts)
    private val _message = MutableLiveData<String?>()
    val message: LiveData<String?> = _message

    /**
     * Place order for current user.
     * Uses session userId automatically.
     */
    fun placeOrderForCurrentUser() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val userId = orderRepository.getCurrentUserId()
                if (userId == SessionManager.NO_USER) {
                    _message.postValue("Please log in to place an order")
                    return@launch
                }
                
                placeOrderInternal(userId)
            } catch (e: Exception) {
                _message.postValue("Failed to place order: ${e.message}")
            }
        }
    }

    /**
     * Place order for a specific user.
     * @deprecated Prefer placeOrderForCurrentUser() which uses session userId
     */
    fun placeOrder(userId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            placeOrderInternal(userId)
        }
    }
    
    private suspend fun placeOrderInternal(userId: Int) {
        try {
            val cartItems = cartRepository.getCartItemsForUser(userId)
            if (cartItems.isEmpty()) {
                _message.postValue("Cart is empty")
                return
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
            
            // No manual reload needed - Flow auto-emits new data

        } catch (e: Exception) {
            _message.postValue("Failed to place order: ${e.message}")
        }
    }

    /**
     * @deprecated No longer needed - orders auto-updates via Flow.
     * Kept for backward compatibility but does nothing.
     */
    @Deprecated("Orders now auto-update via Flow. This method is no longer needed.")
    fun getOrdersByUser(userId: Int) {
        // No-op: Orders is now reactive via Flow
        // This method is kept for backward compatibility with existing code
    }

    fun clearMessage() {
        _message.value = null
    }
}