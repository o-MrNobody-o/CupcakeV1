package com.isetr.cupcake.ui.orders

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.isetr.cupcake.R
import com.isetr.cupcake.viewmodel.AccountViewModel
import com.isetr.cupcake.viewmodel.OrderViewModel

class OrdersFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var ordersAdapter: OrdersAdapter
    private lateinit var accountViewModel: AccountViewModel

    // ✅ Shared ViewModel with activity
    private val orderViewModel: OrderViewModel by activityViewModels()

    private var currentUserId: Int = 0

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

        accountViewModel = ViewModelProvider(
            requireActivity(),
            AccountViewModel.Factory(requireActivity().application)
        ).get(AccountViewModel::class.java)

        observeCurrentUser()
        observeOrders()
    }

    private fun setupRecyclerView() {
        ordersAdapter = OrdersAdapter()
        recyclerView.adapter = ordersAdapter
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
    }

    private fun observeCurrentUser() {
        accountViewModel.currentUser.observe(viewLifecycleOwner) { user ->
            user?.let {
                currentUserId = it.id
                orderViewModel.getOrdersByUser(currentUserId)
            }
        }
    }

    private fun observeOrders() {
        orderViewModel.orders.observe(viewLifecycleOwner) { orders ->
            orders?.let { ordersAdapter.submitList(it) }
        }
    }
}
