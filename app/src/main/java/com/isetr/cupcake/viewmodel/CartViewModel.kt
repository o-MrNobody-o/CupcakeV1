package com.isetr.cupcake.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.isetr.cupcake.data.local.CartEntity
import com.isetr.cupcake.data.repository.CartRepository
import com.isetr.cupcake.session.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * CartViewModel: Manages cart state with REACTIVE user session support.
 * 
 * KEY FIX: Cart data is now reactive to session changes.
 * 
 * Previous bug:
 * - loadCart(userId) was called once, data stayed stale when user switched
 * - CartFragment showed wrong user's cart after login switch
 * 
 * New behavior:
 * - cartItems is a LiveData backed by Flow that auto-switches when user changes
 * - No manual loadCart() needed - data updates automatically
 * - When user logs out, cart shows empty instantly
 * - When new user logs in, their cart appears instantly
 */
class CartViewModel(application: Application) : AndroidViewModel(application) {

    private val cartRepository = CartRepository(application.applicationContext)
    private val sessionManager = SessionManager(application.applicationContext)

    /**
     * REACTIVE: Cart items that automatically update when:
     * 1. User adds/removes/updates cart items
     * 2. User logs out (shows empty)
     * 3. Different user logs in (shows their cart)
     * 
     * No need to call loadCart() - this auto-updates!
     */
    val cartItems: LiveData<List<CartEntity>> = cartRepository.currentUserCartFlow.asLiveData()
    
    /**
     * Current user ID from session.
     * Use this to get the userId for cart operations.
     */
    val currentUserId: LiveData<Int> = sessionManager.activeUserIdFlow.asLiveData()

    /**
     * Add a new item to the cart (or update if exists).
     * Cart will auto-refresh via Flow - no manual reload needed.
     */
    fun addToCart(cartItem: CartEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            cartRepository.addOrUpdateCartItem(cartItem)
            // No reload needed - Flow auto-emits new data
        }
    }

    /**
     * Update an existing cart item.
     * Cart will auto-refresh via Flow - no manual reload needed.
     */
    fun updateCartItem(cartItem: CartEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            cartRepository.updateCartItem(cartItem)
        }
    }

    /**
     * Remove an item from the cart.
     * Cart will auto-refresh via Flow - no manual reload needed.
     */
    fun removeCartItem(cartItem: CartEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            cartRepository.deleteCartItem(cartItem)
        }
    }

    /**
     * Clear entire cart for current user.
     */
    fun clearCurrentUserCart() {
        viewModelScope.launch(Dispatchers.IO) {
            val userId = cartRepository.getCurrentUserId()
            if (userId != SessionManager.NO_USER) {
                cartRepository.clearCart(userId)
            }
        }
    }
    
    /**
     * Clear entire cart for a specific user.
     * @deprecated Prefer clearCurrentUserCart() which uses session userId
     */
    fun clearCart(userId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            cartRepository.clearCart(userId)
        }
    }
    
    /**
     * @deprecated No longer needed - cartItems auto-updates via Flow.
     * Kept for backward compatibility but does nothing.
     */
    @Deprecated("Cart now auto-updates via Flow. This method is no longer needed.")
    fun loadCart(userId: Int) {
        // No-op: Cart is now reactive via Flow
        // This method is kept for backward compatibility with existing code
    }
}
