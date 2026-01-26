package com.isetr.cupcake.data.local

import androidx.annotation.DrawableRes
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pastries")
data class Pastry(
    @PrimaryKey val id: String,
    val name: String,
    val price: Double,
    @DrawableRes val imageRes: Int,
    val imageUrl: String? = null, // AJOUTÉ : Pour les images distantes (Express)
    val available: Boolean = true,
    val description: String,
    val inPromotion: Boolean = false,
    val discountRate: Int = 0,
    val category: String
)
