package com.isetr.cupcake.data.network

import com.google.gson.annotations.SerializedName

data class OrderDto(
    @SerializedName("order_id") val orderId: Int? = null,
    @SerializedName("user_id") val userId: Int,
    @SerializedName("total_amount") val totalAmount: Double,
    @SerializedName("payment_method") val paymentMethod: String,
    @SerializedName("card_number") val cardNumber: String? = null,
    @SerializedName("shipping_address") val shippingAddress: String,
    @SerializedName("timestamp") val timestamp: Long,
    @SerializedName("status") val status: String? = "En attente"
)
