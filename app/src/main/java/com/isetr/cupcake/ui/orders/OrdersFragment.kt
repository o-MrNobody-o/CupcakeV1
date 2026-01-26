package com.isetr.cupcake.ui.orders

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.isetr.cupcake.R
import com.isetr.cupcake.viewmodel.OrderViewModel

/**
 * OrdersFragment: Displays the user's order history.
 * 
 * KEY FIX: Orders now update reactively with session changes.
 * 
 * Previous bugs:
 * - Orders from previous user appeared briefly after login switch
 * - Manual getOrdersByUser() call was needed but sometimes missed
 * - Stale order data persisted in memory
 * 
 * New behavior:
 * - orderViewModel.orders is reactive (Flow-backed)
 * - When session changes, orders update instantly to show new user's orders
 * - When new order is placed, list updates automatically
 * - No manual getOrdersByUser() needed
 */
class OrdersFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var ordersAdapter: OrdersAdapter

    // Order ViewModel - now reactive to session changes
    private val orderViewModel: OrderViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_orders, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.rvOrders)
        setupRecyclerView()
        observeOrders()
    }

    private fun setupRecyclerView() {
        ordersAdapter = OrdersAdapter()
        recyclerView.adapter = ordersAdapter
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
    }

    /**
     * KEY FIX: Orders observation is now fully reactive.
     * 
     * orderViewModel.orders is backed by a Flow that:
     * 1. Listens to SessionManager.activeUserIdFlow
     * 2. When userId changes, switches to new user's orders data
     * 3. Emits empty list when no user is logged in
     * 4. Auto-updates when orders change in Room
     * 
     * No manual getOrdersByUser() needed!
     */
    private fun observeOrders() {
        orderViewModel.orders.observe(viewLifecycleOwner) { orders ->
            // Orders auto-update when:
            // - User switches (different user's orders)
            // - New order is placed
            // - User logs out (empty orders)
            orders?.let { ordersAdapter.submitList(it) }
        }
    }
}
