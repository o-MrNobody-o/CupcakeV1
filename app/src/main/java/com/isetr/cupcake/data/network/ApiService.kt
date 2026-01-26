package com.isetr.cupcake.data.network

import retrofit2.http.*

interface ApiService {

    // --- UTILISATEURS (user.js) ---
    // Correction : Le serveur renvoie un objet avec un ID, pas un UserDto complet
    @POST("users")
    suspend fun register(@Body user: UserDto): GenericResponse 

    @POST("users/login")
    suspend fun login(@Body credentials: Map<String, String>): UserDto 

    @PUT("users/{id}")
    suspend fun updateProfile(@Path("id") id: Int, @Body user: UserDto): GenericResponse

    // --- PRODUITS (produit.js) ---
    @GET("produits")
    suspend fun getPastries(): List<PastryDto>

    // --- PANIER (cart.js) ---
    @GET("cart/{userId}")
    suspend fun getCart(@Path("userId") userId: Int): List<CartItemDto>

    @POST("cart/add")
    suspend fun addToCartRemote(@Body item: CartItemDto): GenericResponse

    @DELETE("cart/remove/{id}")
    suspend fun removeFromCartRemote(@Path("id") id: Int): GenericResponse

    // --- COMMANDES (order.js) ---
    @POST("orders")
    suspend fun placeOrder(@Body order: OrderDto): GenericResponse

    @GET("orders/user/{userId}")
    suspend fun getOrderHistory(@Path("userId") userId: Int): List<OrderDto>
}
