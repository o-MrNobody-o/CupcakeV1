package com.isetr.cupcake.ui.products

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.isetr.cupcake.R
import com.isetr.cupcake.data.model.Pastry

class PastryAdapter(
    private val onDetailClick: (Pastry) -> Unit,
    private val onAddToCartClick: (Pastry) -> Unit
) : ListAdapter<Pastry, PastryAdapter.PastryViewHolder>(PastryDiffCallback) {

    class PastryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val pastryImage: ImageView = itemView.findViewById(R.id.pastry_image)
        private val pastryName: TextView = itemView.findViewById(R.id.pastry_name)
        private val pastryPrice: TextView = itemView.findViewById(R.id.pastry_price)
        private val pastryAvailability: TextView = itemView.findViewById(R.id.pastry_availability)
        private val pastryPromotionDetails: TextView = itemView.findViewById(R.id.pastry_promotion_details)
        private val detailButton: Button = itemView.findViewById(R.id.detail_button)
        private val addToCartButton: Button = itemView.findViewById(R.id.add_to_cart_button)

        fun bind(pastry: Pastry, onDetailClick: (Pastry) -> Unit, onAddToCartClick: (Pastry) -> Unit) {
            val context = itemView.context

            pastryName.text = pastry.name
            pastryPrice.text = "${pastry.price} TND"
            
            // Load image from URL using Glide; fall back to placeholder when blank
            val imageUrl = pastry.imageUrl.takeIf { it.isNotBlank() }
            Glide.with(context)
                .load(imageUrl)
                .placeholder(R.drawable.ic_launcher_foreground)
                .error(R.drawable.ic_launcher_foreground)
                .into(pastryImage)

            if (pastry.available) {
                pastryAvailability.text = "Disponible"
                val greenColor = ContextCompat.getColor(context, R.color.green)
                pastryAvailability.setTextColor(greenColor)
                pastryAvailability.compoundDrawableTintList = ColorStateList.valueOf(greenColor)
            } else {
                pastryAvailability.text = "Indisponible"
                val redColor = ContextCompat.getColor(context, R.color.red)
                pastryAvailability.setTextColor(redColor)
                pastryAvailability.compoundDrawableTintList = ColorStateList.valueOf(redColor)
            }

            if (pastry.inPromotion && pastry.discountRate > 0) {
                pastryPromotionDetails.visibility = View.VISIBLE
                pastryPromotionDetails.text = "-${pastry.discountRate}%"
            } else {
                pastryPromotionDetails.visibility = View.GONE
            }

            detailButton.setOnClickListener { onDetailClick(pastry) }
            addToCartButton.setOnClickListener { onAddToCartClick(pastry) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PastryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.pastry_item, parent, false)
        return PastryViewHolder(view)
    }

    override fun onBindViewHolder(holder: PastryViewHolder, position: Int) {
        holder.bind(getItem(position), onDetailClick, onAddToCartClick)
    }
}

object PastryDiffCallback : DiffUtil.ItemCallback<Pastry>() {
    override fun areItemsTheSame(oldItem: Pastry, newItem: Pastry): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Pastry, newItem: Pastry): Boolean {
        return oldItem == newItem
    }
}
