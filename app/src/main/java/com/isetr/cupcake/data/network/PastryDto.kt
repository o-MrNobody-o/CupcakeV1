package com.isetr.cupcake.data.network

import com.google.gson.annotations.SerializedName

data class PastryDto(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("price") val price: Double,
    @SerializedName("image_name") val imageName: String, // String pour le nom de la ressource ou URL
    @SerializedName("description") val description: String,
    @SerializedName("category") val category: String,
    @SerializedName("discount_rate") val discountRate: Int,
    @SerializedName("in_promotion") val inPromotion: Boolean,
    @SerializedName("available") val available: Boolean
)
