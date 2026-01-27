package com.isetr.cupcake.data.network

import retrofit2.http.*

interface ApiService {

    @POST("users")
    suspend fun register(@Body user: UserDto): GenericResponse
    // 3. @Body : transforme automatiquement l'objet UserDto en JSON
    @POST("users/login")
    suspend fun login(@Body credentials: Map<String, String>): UserDto

    @PUT("users/{id}")
    suspend fun updateProfile(@Path("id") id: Int, @Body user: UserDto): GenericResponse

    // --- NOUVEAU : SUPPRESSION DE COMPTE DISTANTE ---
    @DELETE("users/{id}")
    suspend fun deleteAccountRemote(@Path("id") id: Int): GenericResponse

    @GET("produits")
    suspend fun getPastries(): List<PastryDto>

    @GET("cart/{userId}")
    suspend fun getCart(@Path("userId") userId: Int): List<CartItemDto>

    @POST("cart/add")
    suspend fun addToCartRemote(@Body item: CartItemDto): GenericResponse

    @DELETE("cart/remove/{id}")
    suspend fun removeFromCartRemote(@Path("id") id: Int): GenericResponse

    @POST("orders")
    suspend fun placeOrder(@Body order: OrderDto): GenericResponse

    @GET("orders/user/{userId}")
    suspend fun getOrderHistory(@Path("userId") userId: Int): List<OrderDto>

    @POST("orders/review")
    suspend fun submitReview(@Body reviewData: Map<String, String>): GenericResponse
}
