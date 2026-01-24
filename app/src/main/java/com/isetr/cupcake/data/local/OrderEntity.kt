package com.isetr.cupcake.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "orders")
data class OrderEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: Int,
    val orderDate: Long, // timestamp in milliseconds
    val deliveryDate: Long, // timestamp in milliseconds
    val totalPrice: Double
)