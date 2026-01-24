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
import com.isetr.cupcake.viewmodel.CartViewModel
import com.isetr.cupcake.viewmodel.OrderViewModel

class CartFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var totalPriceTextView: TextView
    private lateinit var btnValidateOrder: Button
    private val cartViewModel: CartViewModel by activityViewModels() // optional, can also be viewModels()
    
    // ✅ Shared ViewModel with activity
    private val orderViewModel: OrderViewModel by activityViewModels()

    private lateinit var accountViewModel: AccountViewModel
    private lateinit var cartAdapter: CartAdapter

    private var currentUserId: Int = 0

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

        accountViewModel = ViewModelProvider(
            requireActivity(),
            AccountViewModel.Factory(requireActivity().application)
        ).get(AccountViewModel::class.java)

        accountViewModel.loadCurrentUser()

        setupRecyclerView()
        observeCurrentUser()
        observeCart()
        observeOrderMessages()
        setupValidateButton()
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

    private fun observeCurrentUser() {
        accountViewModel.currentUser.observe(viewLifecycleOwner) { user ->
            user?.let {
                currentUserId = it.id
                cartViewModel.loadCart(currentUserId)
            }
        }
    }

    private fun observeCart() {
        cartViewModel.cartItems.observe(viewLifecycleOwner) { cartItems ->
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
            if (currentUserId != 0) {
                orderViewModel.placeOrder(currentUserId)
            } else {
                Toast.makeText(requireContext(), "User not loaded yet", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
