package com.isetr.cupcake.ui.order

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.isetr.cupcake.R
import com.isetr.cupcake.data.local.OrderEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class OrderHistoryAdapter : ListAdapter<OrderEntity, OrderHistoryAdapter.OrderViewHolder>(OrderDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_order_history, parent, false)
        return OrderViewHolder(view)
    }

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class OrderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvDate: TextView = itemView.findViewById(R.id.tvOrderDate)
        private val tvAmount: TextView = itemView.findViewById(R.id.tvOrderAmount)
        private val tvStatus: TextView = itemView.findViewById(R.id.tvOrderStatus)
        private val tvPayment: TextView = itemView.findViewById(R.id.tvPaymentMethod)

        fun bind(order: OrderEntity) {
            val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            tvDate.text = "Commande du " + sdf.format(Date(order.timestamp))
            tvAmount.text = String.format("%.2f TND", order.totalAmount)
            tvStatus.text = order.status
            tvPayment.text = "Paiement: " + order.paymentMethod
        }
    }

    class OrderDiffCallback : DiffUtil.ItemCallback<OrderEntity>() {
        override fun areItemsTheSame(oldItem: OrderEntity, newItem: OrderEntity) = oldItem.orderId == newItem.orderId
        override fun areContentsTheSame(oldItem: OrderEntity, newItem: OrderEntity) = oldItem == newItem
    }
}
