package com.isetr.cupcake.ui.order

import android.os.Bundle
import android.util.Log
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
    private val TAG = "OrderHistory"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            setContentView(R.layout.activity_order_history)

            orderRepository = OrderRepository(this)
            btnBack = findViewById(R.id.btnBackFromHistory)

            setupRecyclerView()
            
            loadActiveSessionHistory()

            btnBack.setOnClickListener { finish() }
        } catch (e: Exception) {
            Log.e(TAG, "Erreur initialisation: ${e.message}")
            finish()
        }
    }

    private fun setupRecyclerView() {
        recyclerView = findViewById(R.id.rvOrderHistory)
        
        // CORRECTION : On passe la fonction de rappel (callback) pour l'avis
        adapter = OrderHistoryAdapter { orderId, reviewText ->
            submitReview(orderId, reviewText)
        }
        
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)
    }

    private fun submitReview(orderId: Int, review: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            val success = orderRepository.submitOrderReview(orderId, review)
            withContext(Dispatchers.Main) {
                if (success) {
                    Toast.makeText(this@OrderHistoryActivity, "Merci pour votre avis !", Toast.LENGTH_SHORT).show()
                    loadActiveSessionHistory() // Recharge la liste pour afficher l'avis
                } else {
                    Toast.makeText(this@OrderHistoryActivity, "Échec de l'envoi de l'avis", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun loadActiveSessionHistory() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val orders = orderRepository.getOrdersForActiveSession()
                withContext(Dispatchers.Main) {
                    if (orders.isEmpty()) {
                        Toast.makeText(this@OrderHistoryActivity, "Aucune commande pour ce compte", Toast.LENGTH_LONG).show()
                    }
                    adapter.submitList(orders)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Erreur Room: ${e.message}")
            }
        }
    }
}
