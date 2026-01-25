package com.isetr.cupcake.data.network

import com.google.gson.annotations.SerializedName

data class UserDto(
    @SerializedName("id") val id: Int? = null,
    @SerializedName("nom") val nom: String,
    @SerializedName("prenom") val prenom: String,
    @SerializedName("email") val email: String,
    @SerializedName("adresse") val adresse: String?,
    @SerializedName("telephone") val telephone: String?,
    @SerializedName("password") val password: String? = null
)

data class LoginResponse(
    @SerializedName("status") val status: String,
    @SerializedName("user") val user: UserDto?,
    @SerializedName("message") val message: String?
)

data class GenericResponse(
    @SerializedName("status") val status: String,
    @SerializedName("message") val message: String?,
    @SerializedName("id") val id: Int? = null,
    @SerializedName("orderId") val orderId: Int? = null
)
