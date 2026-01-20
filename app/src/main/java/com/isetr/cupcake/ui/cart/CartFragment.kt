package com.isetr.cupcake.ui.cart

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.isetr.cupcake.R
import com.isetr.cupcake.data.local.CartEntity
import com.isetr.cupcake.viewmodel.CartViewModel

class CartFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var totalPriceTextView: TextView
    private val cartViewModel: CartViewModel by viewModels()
    private lateinit var cartAdapter: CartAdapter

    private var currentUserId: Int = 0 // We'll set this dynamically

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

        // TODO: Replace this with your actual logged-in user ID
        currentUserId = 1

        setupRecyclerView()
        observeCart()
    }

    private fun setupRecyclerView() {
        cartAdapter = CartAdapter(
            onQuantityChanged = { item, newQty ->
                val updatedItem = item.copy(quantity = newQty)
                cartViewModel.updateCartItem(updatedItem)
            },
            onRemoveItem = { item ->
                cartViewModel.removeCartItem(item)
            }
        )

        recyclerView.adapter = cartAdapter
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
    }

    private fun observeCart() {
        cartViewModel.cartItems.observe(viewLifecycleOwner) { cartItems ->
            cartAdapter.submitList(cartItems)
            val total = cartItems.sumOf { it.price * it.quantity }
            totalPriceTextView.text = "Total: $total TND"
        }

        // Load user's cart
        cartViewModel.loadCart(currentUserId)
    }
}
