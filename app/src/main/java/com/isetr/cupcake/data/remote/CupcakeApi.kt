package com.isetr.cupcake.data.remote

import com.isetr.cupcake.data.model.Pastry
import com.isetr.cupcake.data.remote.dto.LoginRequest
import com.isetr.cupcake.data.remote.dto.OrderRequest
import com.isetr.cupcake.data.remote.dto.OrderResponse
import com.isetr.cupcake.data.remote.dto.UserResponse
import retrofit2.Response
import retrofit2.http.*

/**
 * Retrofit API interface for Cupcake backend endpoints.
 * All functions are suspend for use with Kotlin coroutines.
 */
interface CupcakeApi {
    
    // ==================== PASTRIES ENDPOINTS ====================
    
    /**
     * Get all pastries from the server.
     * @return List of Pastry objects
     */
    @GET("pastries")
    suspend fun getAllPastries(): Response<List<Pastry>>
    
    /**
     * Get a specific pastry by ID.
     * @param id The pastry ID
     * @return Single Pastry object
     */
    @GET("pastries/{id}")
    suspend fun getPastryById(@Path("id") id: String): Response<Pastry>
    
    /**
     * Get pastries filtered by category.
     * @param category The category name (e.g., "Cupcakes", "Gâteaux")
     * @return List of Pastry objects matching the category
     */
    @GET("pastries/category/{category}")
    suspend fun getPastriesByCategory(@Path("category") category: String): Response<List<Pastry>>
    
    /**
     * Get pastries that are currently in promotion.
     * @return List of Pastry objects with active promotions
     */
    @GET("pastries/promotions")
    suspend fun getPromotionalPastries(): Response<List<Pastry>>
    
    // ==================== USER ENDPOINTS ====================
    
    /**
     * Register a new user.
     * @param user User data for registration
     * @return UserResponse with user details and token
     */
    @POST("users/register")
    suspend fun registerUser(@Body user: UserResponse): Response<UserResponse>
    
    /**
     * Login a user.
     * @param credentials Login credentials (email and password)
     * @return UserResponse with user details and token
     */
    @POST("users/login")
    suspend fun loginUser(@Body credentials: LoginRequest): Response<UserResponse>
    
    /**
     * Get user profile by ID.
     * @param userId The user ID
     * @return UserResponse with user details
     */
    @GET("users/{userId}")
    suspend fun getUserProfile(@Path("userId") userId: Int): Response<UserResponse>
    
    /**
     * Update user profile.
     * @param userId The user ID
     * @param user Updated user data
     * @return UserResponse with updated user details
     */
    @PUT("users/{userId}")
    suspend fun updateUserProfile(
        @Path("userId") userId: Int,
        @Body user: UserResponse
    ): Response<UserResponse>
    
    /**
     * Delete user account.
     * @param userId The user ID
     * @return Response indicating success or failure
     */
    @DELETE("users/{userId}")
    suspend fun deleteUser(@Path("userId") userId: Int): Response<Unit>
    
    // ==================== ORDER ENDPOINTS ====================
    
    /**
     * Create a new order.
     * @param order Order request data
     * @return OrderResponse with created order details
     */
    @POST("orders")
    suspend fun createOrder(@Body order: OrderRequest): Response<OrderResponse>
    
    /**
     * Get all orders for a specific user.
     * @param userId The user ID
     * @return List of OrderResponse objects
     */
    @GET("orders/user/{userId}")
    suspend fun getUserOrders(@Path("userId") userId: Int): Response<List<OrderResponse>>
    
    /**
     * Get a specific order by ID.
     * @param orderId The order ID
     * @return OrderResponse with order details
     */
    @GET("orders/{orderId}")
    suspend fun getOrderById(@Path("orderId") orderId: Int): Response<OrderResponse>
    
    /**
     * Cancel an order.
     * @param orderId The order ID
     * @return Response indicating success or failure
     */
    @DELETE("orders/{orderId}")
    suspend fun cancelOrder(@Path("orderId") orderId: Int): Response<Unit>
    
    /**
     * Update order status (e.g., from "pending" to "delivered").
     * @param orderId The order ID
     * @param status The new status
     * @return OrderResponse with updated order details
     */
    @PATCH("orders/{orderId}/status")
    suspend fun updateOrderStatus(
        @Path("orderId") orderId: Int,
        @Body status: Map<String, String>
    ): Response<OrderResponse>
}
