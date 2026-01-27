package com.isetr.cupcake.ui.order

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.isetr.cupcake.R
import com.isetr.cupcake.data.local.OrderEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class OrderHistoryAdapter(private val onReviewSubmit: (Int, String) -> Unit) : 
    ListAdapter<OrderEntity, OrderHistoryAdapter.OrderViewHolder>(OrderDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_order_history, parent, false)
        return OrderViewHolder(view)
    }

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        holder.bind(getItem(position), onReviewSubmit)
    }

    class OrderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvDate: TextView = itemView.findViewById(R.id.tvOrderDate)
        private val tvAmount: TextView = itemView.findViewById(R.id.tvOrderAmount)
        private val tvStatus: TextView = itemView.findViewById(R.id.tvOrderStatus)
        private val layoutReview: LinearLayout = itemView.findViewById(R.id.layoutReview)
        private val etReview: EditText = itemView.findViewById(R.id.etReview)
        private val btnSubmitReview: Button = itemView.findViewById(R.id.btnSubmitReview)
        private val tvExistingReview: TextView = itemView.findViewById(R.id.tvExistingReview)

        fun bind(order: OrderEntity, onReviewSubmit: (Int, String) -> Unit) {
            val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            tvDate.text = "Commande du " + sdf.format(Date(order.timestamp))
            tvAmount.text = String.format("%.2f TND", order.totalAmount)
            tvStatus.text = order.status

            // LOGIQUE D'AFFICHAGE DE L'AVIS
            // L'avis ne s'affiche que si la commande est "Livrée"
            if (order.status.equals("Livrée", ignoreCase = true)) {
                if (order.review.isNullOrEmpty()) {
                    // Pas encore d'avis : on montre l'input
                    layoutReview.visibility = View.VISIBLE
                    tvExistingReview.visibility = View.GONE
                    btnSubmitReview.setOnClickListener {
                        val reviewText = etReview.text.toString().trim()
                        if (reviewText.isNotEmpty()) {
                            onReviewSubmit(order.orderId, reviewText)
                        }
                    }
                } else {
                    // Avis déjà existant : on montre le texte
                    layoutReview.visibility = View.GONE
                    tvExistingReview.visibility = View.VISIBLE
                    tvExistingReview.text = "Votre avis : ${order.review}"
                }
            } else {
                // Commande en cours : on cache tout le bloc avis
                layoutReview.visibility = View.GONE
                tvExistingReview.visibility = View.GONE
            }
        }
    }

    class OrderDiffCallback : DiffUtil.ItemCallback<OrderEntity>() {
        override fun areItemsTheSame(oldItem: OrderEntity, newItem: OrderEntity) = oldItem.orderId == newItem.orderId
        override fun areContentsTheSame(oldItem: OrderEntity, newItem: OrderEntity) = oldItem == newItem
    }
}
