package com.isetr.cupcake.ui.order

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.isetr.cupcake.R
import com.isetr.cupcake.data.local.OrderEntity
import com.isetr.cupcake.data.repository.OrderRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class OrderStatusActivity : AppCompatActivity() {

    private lateinit var tvCurrentStatus: TextView
    private lateinit var pbStatus: ProgressBar
    private lateinit var ivStatusIcon: ImageView
    private lateinit var btnRefresh: Button
    private lateinit var btnBackHome: Button

    private lateinit var orderRepository: OrderRepository
    private var currentOrder: OrderEntity? = null
    private val handler = Handler(Looper.getMainLooper())
    
    // Liste des statuts dans l'ordre
    private val statusList = listOf("En attente", "En préparation", "En livraison", "Livrée")
    
    // Délais de simulation (Passé à 5 secondes pour les tests)
    private val SIMULATION_DELAY = 5000L 

    private val statusUpdater = object : Runnable {
        override fun run() {
            currentOrder?.let { order ->
                val currentIndex = statusList.indexOf(order.status)
                if (currentIndex != -1 && currentIndex < statusList.size - 1) {
                    val nextStatus = statusList[currentIndex + 1]
                    updateOrderStatus(nextStatus)
                    
                    // Si ce n'est pas le dernier statut, on replanifie
                    if (currentIndex + 1 < statusList.size - 1) {
                        handler.postDelayed(this, SIMULATION_DELAY)
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_order_status)

        orderRepository = OrderRepository(this)
        
        initViews()
        loadLastOrder()
        
        btnRefresh.setOnClickListener {
            Toast.makeText(this, "Actualisation...", Toast.LENGTH_SHORT).show()
            loadLastOrder() // Rechargement manuel
        }

        btnBackHome.setOnClickListener {
            finish()
        }
    }

    private fun initViews() {
        tvCurrentStatus = findViewById(R.id.tvCurrentStatus)
        pbStatus = findViewById(R.id.pbStatus)
        ivStatusIcon = findViewById(R.id.ivStatusIcon)
        btnRefresh = findViewById(R.id.btnRefresh)
        btnBackHome = findViewById(R.id.btnBackHome)
    }

    private fun loadLastOrder() {
        lifecycleScope.launch(Dispatchers.IO) {
            // Simulation : On récupère la dernière commande de l'utilisateur (ID 1 par défaut)
            val order = orderRepository.getLastOrder(1)
            withContext(Dispatchers.Main) {
                if (order == null) {
                    tvCurrentStatus.text = "Aucune commande trouvée"
                } else {
                    currentOrder = order
                    displayStatus(order.status)
                    startSimulation()
                }
            }
        }
    }

    private fun startSimulation() {
        handler.removeCallbacks(statusUpdater)
        val currentIndex = statusList.indexOf(currentOrder?.status)
        if (currentIndex != -1 && currentIndex < statusList.size - 1) {
            Toast.makeText(this, "Suivi temps réel actif (5s)", Toast.LENGTH_SHORT).show()
            handler.postDelayed(statusUpdater, SIMULATION_DELAY)
        }
    }

    private fun updateOrderStatus(newStatus: String) {
        currentOrder?.let { order ->
            val updatedOrder = order.copy(status = newStatus)
            currentOrder = updatedOrder // Mise à jour locale immédiate
            
            // Mise à jour de l'UI immédiate pour la réactivité (approche senior)
            displayStatus(newStatus)
            Toast.makeText(this, "Nouveau statut : $newStatus", Toast.LENGTH_SHORT).show()
            
            lifecycleScope.launch(Dispatchers.IO) {
                // Sauvegarde asynchrone en base de données
                orderRepository.updateOrderStatus(updatedOrder)
            }
        }
    }

    private fun displayStatus(status: String) {
        tvCurrentStatus.text = status
        
        // Mise à jour visuelle (Progress bar et icône)
        when (status) {
            "En attente" -> {
                pbStatus.progress = 25
                ivStatusIcon.setImageResource(R.drawable.ic_orders)
            }
            "En préparation" -> {
                pbStatus.progress = 50
                ivStatusIcon.setImageResource(R.drawable.ic_products)
            }
            "En livraison" -> {
                pbStatus.progress = 75
                ivStatusIcon.setImageResource(R.drawable.ic_shopping_cart)
            }
            "Livrée" -> {
                pbStatus.progress = 100
                ivStatusIcon.setImageResource(R.drawable.ic_baseline_check_circle_24)
                handler.removeCallbacks(statusUpdater) // On arrête tout
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(statusUpdater)
    }
}
