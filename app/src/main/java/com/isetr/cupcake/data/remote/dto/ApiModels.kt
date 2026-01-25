package com.isetr.cupcake.data.remote.dto

/**
 * Request model for user login.
 */
data class LoginRequest(
    val email: String,
    val password: String
)

/**
 * Response model for user data from API.
 * Can be used for registration, login, and profile responses.
 */
data class UserResponse(
    val id: Int = 0,
    val nom: String,
    val prenom: String,
    val email: String,
    val adresse: String = "",
    val telephone: String = "",
    val password: String = "", // Only needed for registration
    val token: String? = null // Authentication token from login/register
)

/**
 * Request model for creating an order.
 */
data class OrderRequest(
    val userId: Int,
    val items: List<OrderItem>,
    val totalPrice: Double,
    val deliveryDate: Long // timestamp in milliseconds
)

/**
 * Individual item in an order.
 */
data class OrderItem(
    val pastryId: String,
    val name: String,
    val price: Double,
    val quantity: Int
)

/**
 * Response model for order data from API.
 */
data class OrderResponse(
    val id: Int,
    val userId: Int,
    val items: List<OrderItem>,
    val orderDate: Long, // timestamp in milliseconds
    val deliveryDate: Long, // timestamp in milliseconds
    val totalPrice: Double,
    val status: String = "pending" // e.g., "pending", "processing", "delivered", "cancelled"
)
