package com.isetr.cupcake.data.network

import com.google.gson.annotations.SerializedName

/**
 * DTO pour les articles du panier.
 * Correspond aux clés de cart_api.php ($item['productId'], etc.)
 */
data class CartItemDto(
    @SerializedName("id") val id: Int? = null,
    @SerializedName("userId") val userId: Int,
    @SerializedName("productId") val productId: String,
    @SerializedName("name") val name: String,
    @SerializedName("price") val price: Double,
    @SerializedName("imageName") val imageName: String,
    @SerializedName("quantity") val quantity: Int
)
