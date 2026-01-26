package com.isetr.cupcake.data.network

import com.google.gson.annotations.SerializedName

data class PastryDto(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("price") val price: Double,
    @SerializedName("available") val available: Int,
    @SerializedName("description") val description: String?,
    @SerializedName("in_promotion") val inPromotion: Int, 
    @SerializedName("discount_rate") val discountRate: Int,
    @SerializedName("category") val category: String,
    @SerializedName("image_name") val imageName: String? // Doit correspondre à la colonne MySQL
)
