package com.isetr.cupcake.ui.orders

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

class OrdersAdapter : ListAdapter<OrderEntity, OrdersAdapter.OrdersViewHolder>(OrdersDiffCallback) {

    inner class OrdersViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvOrderId: TextView = itemView.findViewById(R.id.tvOrderId)
        private val tvOrderDate: TextView = itemView.findViewById(R.id.tvOrderDate)
        private val tvDeliveryDate: TextView = itemView.findViewById(R.id.tvDeliveryDate)
        private val tvTotalPrice: TextView = itemView.findViewById(R.id.tvTotalPrice)

        fun bind(order: OrderEntity) {
            tvOrderId.text = "Commande #${order.id}"

            val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            tvOrderDate.text = "Date de commande: ${dateFormat.format(Date(order.orderDate))}"
            tvDeliveryDate.text = "Date de livraison: ${dateFormat.format(Date(order.deliveryDate))}"

            tvTotalPrice.text = "Total: ${order.totalPrice} TND"
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrdersViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_order, parent, false)
        return OrdersViewHolder(view)
    }

    override fun onBindViewHolder(holder: OrdersViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

object OrdersDiffCallback : DiffUtil.ItemCallback<OrderEntity>() {
    override fun areItemsTheSame(oldItem: OrderEntity, newItem: OrderEntity) =
        oldItem.id == newItem.id

    override fun areContentsTheSame(oldItem: OrderEntity, newItem: OrderEntity) =
        oldItem == newItem
}