package com.isetr.cupcake.ui.welcome

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.isetr.cupcake.R
import com.isetr.cupcake.data.model.Pastry
import com.isetr.cupcake.databinding.ItemOnSaleProductBinding

class OnSaleProductAdapter(
    private val pastries: List<Pastry>
) : RecyclerView.Adapter<OnSaleProductAdapter.OnSaleProductViewHolder>() {

    inner class OnSaleProductViewHolder(private val binding: ItemOnSaleProductBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(pastry: Pastry) {
            binding.tvProductName.text = pastry.name
            binding.tvProductPrice.text = "$${"%.2f".format(pastry.price)}"
            binding.ivProductImage.setImageResource(pastry.imageRes)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OnSaleProductViewHolder {
        val binding = ItemOnSaleProductBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return OnSaleProductViewHolder(binding)
    }

    override fun onBindViewHolder(holder: OnSaleProductViewHolder, position: Int) {
        holder.bind(pastries[position])
    }

    override fun getItemCount(): Int = pastries.size
}