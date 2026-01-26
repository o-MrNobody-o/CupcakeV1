package com.isetr.cupcake.ui.cart

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.isetr.cupcake.R
import com.isetr.cupcake.viewmodel.AccountViewModel
import com.isetr.cupcake.viewmodel.AccountViewModelFactory
import com.isetr.cupcake.viewmodel.CartViewModel
import com.isetr.cupcake.viewmodel.OrderViewModel

/**
 * CartFragment: Displays the user's shopping cart.
 * 
 * KEY FIX: Cart now updates reactively with session changes.
 * 
 * Previous bugs:
 * - Cart showed wrong user's items after login switch
 * - Adding items didn't update cart immediately
 * - Manual loadCart() call was needed but sometimes missed
 * 
 * New behavior:
 * - cartViewModel.cartItems is reactive (Flow-backed)
 * - When session changes, cart updates instantly to show new user's items
 * - When items are added/removed, cart updates automatically
 * - No manual loadCart() needed
 */
class CartFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var totalPriceTextView: TextView
    private lateinit var btnValidateOrder: Button
    
    // Cart ViewModel - now reactive to session changes
    private val cartViewModel: CartViewModel by activityViewModels()
    
    // Order ViewModel - shared with activity
    private val orderViewModel: OrderViewModel by activityViewModels()

    private lateinit var accountViewModel: AccountViewModel
    private lateinit var cartAdapter: CartAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_cart, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.rvCart)
        totalPriceTextView = view.findViewById(R.id.tvTotalPrice)
        btnValidateOrder = view.findViewById(R.id.btnValidateOrder)

        // Account ViewModel for user info
        accountViewModel = ViewModelProvider(
            requireActivity(),
            AccountViewModelFactory(requireContext())
        ).get(AccountViewModel::class.java)

        setupRecyclerView()
        observeCart()
        observeOrderMessages()
        setupValidateButton()
    }

    private fun setupRecyclerView() {
        cartAdapter = CartAdapter(
            onQuantityChanged = { item, newQty ->
                val updatedItem = item.copy(quantity = newQty)
                cartViewModel.updateCartItem(updatedItem)
                // No reload needed - Flow auto-updates
            },
            onRemoveItem = { item ->
                cartViewModel.removeCartItem(item)
                // No reload needed - Flow auto-updates
            }
        )

        recyclerView.adapter = cartAdapter
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
    }

    /**
     * KEY FIX: Cart observation is now fully reactive.
     * 
     * cartViewModel.cartItems is backed by a Flow that:
     * 1. Listens to SessionManager.activeUserIdFlow
     * 2. When userId changes, switches to new user's cart data
     * 3. Emits empty list when no user is logged in
     * 4. Auto-updates when cart items change in Room
     * 
     * No manual loadCart() needed!
     */
    private fun observeCart() {
        cartViewModel.cartItems.observe(viewLifecycleOwner) { cartItems ->
            // Cart auto-updates when:
            // - User switches (different user's cart)
            // - Items are added/removed/updated
            // - User logs out (empty cart)
            cartAdapter.submitList(cartItems)
            
            val total = cartItems.sumOf { it.price * it.quantity }
            totalPriceTextView.text = "Total: $total TND"
        }
    }

    private fun observeOrderMessages() {
        orderViewModel.message.observe(viewLifecycleOwner) { msg ->
            msg?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                orderViewModel.clearMessage()
            }
        }
    }

    private fun setupValidateButton() {
        btnValidateOrder.setOnClickListener {
            // Use reactive currentUser to get userId
            val currentUser = accountViewModel.currentUser.value
            if (currentUser != null) {
                orderViewModel.placeOrder(currentUser.id)
                // Cart will auto-clear via Flow when order is placed
            } else {
                Toast.makeText(requireContext(), "Please log in first", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
