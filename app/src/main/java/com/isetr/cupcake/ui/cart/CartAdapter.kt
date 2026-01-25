package com.isetr.cupcake.ui.cart

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.isetr.cupcake.R
import com.isetr.cupcake.data.local.CartEntity

class CartAdapter(
    private val onQuantityChanged: (CartEntity, Int) -> Unit,
    private val onRemoveItem: (CartEntity) -> Unit
) : ListAdapter<CartEntity, CartAdapter.CartViewHolder>(CartDiffCallback) {

    inner class CartViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvName: TextView = itemView.findViewById(R.id.tvCartItemName)
        private val tvPrice: TextView = itemView.findViewById(R.id.tvCartItemPrice)
        private val tvQuantity: TextView = itemView.findViewById(R.id.tvCartItemQuantity)
        private val btnIncrease: Button = itemView.findViewById(R.id.btnIncrease)
        private val btnDecrease: Button = itemView.findViewById(R.id.btnDecrease)
        private val btnRemove: Button = itemView.findViewById(R.id.btnRemove)
        private val ivImage: ImageView = itemView.findViewById(R.id.ivCartItemImage)

        fun bind(item: CartEntity) {
            tvName.text = item.name
            tvPrice.text = "${item.price} TND"
            tvQuantity.text = item.quantity.toString()

            btnIncrease.setOnClickListener {
                onQuantityChanged(item, item.quantity + 1)
            }
            btnDecrease.setOnClickListener {
                if (item.quantity > 1) onQuantityChanged(item, item.quantity - 1)
            }
            btnRemove.setOnClickListener {
                onRemoveItem(item)
            }

            // Load image from URL using Glide
            Glide.with(itemView.context)
                .load(item.imageUrl)
                .placeholder(R.drawable.ic_launcher_foreground)
                .error(R.drawable.ic_launcher_foreground)
                .into(ivImage)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CartViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_cart, parent, false)
        return CartViewHolder(view)
    }

    override fun onBindViewHolder(holder: CartViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

object CartDiffCallback : DiffUtil.ItemCallback<CartEntity>() {
    override fun areItemsTheSame(oldItem: CartEntity, newItem: CartEntity) =
        oldItem.productId == newItem.productId

    override fun areContentsTheSame(oldItem: CartEntity, newItem: CartEntity) =
        oldItem == newItem
}
