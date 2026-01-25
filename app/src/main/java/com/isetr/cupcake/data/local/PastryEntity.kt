package com.isetr.cupcake.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pastries")
data class PastryEntity(
    @PrimaryKey
    val id: String = "",
    val name: String = "",
    val price: Double = 0.0,
    val imageUrl: String = "",
    val available: Boolean = true,
    val description: String = "",
    val inPromotion: Boolean = false,
    val discountRate: Int = 0,
    val category: String = ""
)
