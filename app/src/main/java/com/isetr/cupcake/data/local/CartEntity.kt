package com.isetr.cupcake.data.local

import androidx.annotation.DrawableRes
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cart_items")
data class CartEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: Int,
    val productId: String,
    val name: String,
    val price: Double,
    @DrawableRes val imageRes: Int,
    val quantity: Int = 1,
    val inPromotion: Boolean = false, // AJOUTÉ
    val discountRate: Int = 0         // AJOUTÉ
)
