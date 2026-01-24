package com.isetr.cupcake.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "orders")
data class OrderEntity(
    @PrimaryKey(autoGenerate = true) val orderId: Int = 0,
    val userId: Int,
    val totalAmount: Double,
    val paymentMethod: String,
    val cardNumber: String? = null,
    val shippingAddress: String,
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = "En attente"
)
