package com.isetr.cupcake.ui.order

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.isetr.cupcake.R
import com.isetr.cupcake.data.repository.OrderRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class OrderHistoryActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: OrderHistoryAdapter
    private lateinit var orderRepository: OrderRepository
    private lateinit var btnBack: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            setContentView(R.layout.activity_order_history)

            orderRepository = OrderRepository(this)
            btnBack = findViewById(R.id.btnBackFromHistory)
            
            setupRecyclerView()
            loadOrderHistory()

            btnBack.setOnClickListener { finish() }
        } catch (e: Exception) {
            Toast.makeText(this, "Erreur : ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun setupRecyclerView() {
        recyclerView = findViewById(R.id.rvOrderHistory)
        adapter = OrderHistoryAdapter()
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)
    }

    private fun loadOrderHistory() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Utilisateur 1 par défaut
                val orders = orderRepository.getAllOrders(1)
                withContext(Dispatchers.Main) {
                    if (orders.isEmpty()) {
                        Toast.makeText(this@OrderHistoryActivity, "Aucune commande trouvée", Toast.LENGTH_SHORT).show()
                    }
                    adapter.submitList(orders)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@OrderHistoryActivity, "Erreur de chargement", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
