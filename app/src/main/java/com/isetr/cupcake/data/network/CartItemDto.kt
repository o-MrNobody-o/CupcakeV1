package com.isetr.cupcake.data.network

import com.google.gson.annotations.SerializedName

data class CartItemDto(
    val id: Int? = null,
    @SerializedName("user_id") val userId: Int,
    @SerializedName("product_id") val productId: String,
    val name: String,
    val price: Double,
    @SerializedName("image_name") val imageName: String?,
    val quantity: Int
)
