package com.isetr.cupcake.data.network

import com.google.gson.annotations.SerializedName

data class UserDto(
    val id: Int? = null,
    val nom: String,
    val prenom: String,
    val email: String,
    val adresse: String?,
    val telephone: String?,
    val password: String? = null
)

data class GenericResponse(
    val id: Int? = null,
    @SerializedName("order_id") val orderId: Int? = null,
    val success: Boolean? = null,
    val error: String? = null
)
