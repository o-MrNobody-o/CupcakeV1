package com.isetr.cupcake.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cart_items")
data class CartEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: Int,
    val productId: String,
    val name: String,
    val price: Double,
    val imageUrl: String = "",
    val quantity: Int = 1
)
