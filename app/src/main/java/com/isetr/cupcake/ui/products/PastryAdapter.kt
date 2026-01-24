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
import com.isetr.cupcake.R
import com.isetr.cupcake.data.local.Pastry

private const val VIEW_TYPE_HEADER = 0
private const val VIEW_TYPE_PASTRY = 1

class PastryAdapter(private val onDetailClick: (Pastry) -> Unit,
                    private val onAddToCartClick: (Pastry) -> Unit) :
    ListAdapter<DataItem, RecyclerView.ViewHolder>(PastryDiffCallback) {

    class PastryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val pastryImage: ImageView = itemView.findViewById(R.id.pastry_image)
        private val pastryName: TextView = itemView.findViewById(R.id.pastry_name)
        private val pastryPrice: TextView = itemView.findViewById(R.id.pastry_price)
        private val pastryAvailability: TextView = itemView.findViewById(R.id.pastry_availability)
        private val pastryPromotionDetails: TextView = itemView.findViewById(R.id.pastry_promotion_details)
        private val detailButton: Button = itemView.findViewById(R.id.detail_button)
        private val addToCartButton: Button = itemView.findViewById(R.id.add_to_cart_button)

        fun bind(pastry: Pastry, onDetailClick: (Pastry) -> Unit ,onAddToCartClick: (Pastry) -> Unit) {
            val context = itemView.context

            pastryName.text = pastry.name
            pastryPrice.text = "${pastry.price} TND"
            pastryImage.setImageResource(pastry.imageRes)

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

    class CategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val categoryName: TextView = itemView.findViewById(R.id.category_name)
        fun bind(name: String) {
            categoryName.text = name
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is DataItem.HeaderItem -> VIEW_TYPE_HEADER
            is DataItem.PastryItem -> VIEW_TYPE_PASTRY
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_HEADER -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.category_header_item, parent, false)
                CategoryViewHolder(view)
            }
            VIEW_TYPE_PASTRY -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.pastry_item, parent, false)
                PastryViewHolder(view)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is DataItem.HeaderItem -> (holder as CategoryViewHolder).bind(item.categoryName)
            is DataItem.PastryItem -> (holder as PastryViewHolder).bind(item.pastry, onDetailClick, onAddToCartClick)
        }
    }
}

object PastryDiffCallback : DiffUtil.ItemCallback<DataItem>() {
    override fun areItemsTheSame(oldItem: DataItem, newItem: DataItem): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: DataItem, newItem: DataItem): Boolean {
        return oldItem == newItem
    }
}
