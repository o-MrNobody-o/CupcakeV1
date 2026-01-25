package com.isetr.cupcake.data.remote

import android.util.Log
import com.isetr.cupcake.data.model.Pastry
import com.isetr.cupcake.data.remote.dto.LoginRequest
import com.isetr.cupcake.data.remote.dto.OrderRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Example repository demonstrating how to use the Retrofit API client.
 * This shows best practices for making API calls with error handling.
 */
class RemotePastryRepository {
    
    private val api = ApiClient.api
    
    /**
     * Fetch all pastries from the remote server.
     * @return List of Pastry objects or empty list if error occurs
     */
    suspend fun fetchPastries(): List<Pastry> = withContext(Dispatchers.IO) {
        try {
            val response = api.getAllPastries()
            if (response.isSuccessful) {
                response.body() ?: emptyList()
            } else {
                Log.e("RemotePastryRepo", "Error: ${response.code()} - ${response.message()}")
                emptyList()
            }
        } catch (e: Exception) {
            Log.e("RemotePastryRepo", "Exception: ${e.message}", e)
            emptyList()
        }
    }
    
    /**
     * Fetch pastries by category.
     * @param category The category name
     * @return List of Pastry objects or empty list if error occurs
     */
    suspend fun fetchPastriesByCategory(category: String): List<Pastry> = withContext(Dispatchers.IO) {
        try {
            val response = api.getPastriesByCategory(category)
            if (response.isSuccessful) {
                response.body() ?: emptyList()
            } else {
                Log.e("RemotePastryRepo", "Error: ${response.code()}")
                emptyList()
            }
        } catch (e: Exception) {
            Log.e("RemotePastryRepo", "Exception: ${e.message}", e)
            emptyList()
        }
    }
    
    /**
     * Fetch promotional pastries.
     * @return List of Pastry objects on promotion or empty list if error occurs
     */
    suspend fun fetchPromotionalPastries(): List<Pastry> = withContext(Dispatchers.IO) {
        try {
            val response = api.getPromotionalPastries()
            if (response.isSuccessful) {
                response.body() ?: emptyList()
            } else {
                Log.e("RemotePastryRepo", "Error: ${response.code()}")
                emptyList()
            }
        } catch (e: Exception) {
            Log.e("RemotePastryRepo", "Exception: ${e.message}", e)
            emptyList()
        }
    }
}

/**
 * Example showing how to use the API in a ViewModel or repository.
 */
object ApiUsageExamples {
    
    /**
     * Example: Fetch all pastries
     */
    suspend fun exampleFetchPastries() {
        val response = ApiClient.api.getAllPastries()
        if (response.isSuccessful) {
            val pastries = response.body() ?: emptyList()
            // Use pastries...
            Log.d("API", "Fetched ${pastries.size} pastries")
        } else {
            Log.e("API", "Error: ${response.code()}")
        }
    }
    
    /**
     * Example: User login
     */
    suspend fun exampleLogin(email: String, password: String) {
        val loginRequest = LoginRequest(email, password)
        val response = ApiClient.api.loginUser(loginRequest)
        if (response.isSuccessful) {
            val user = response.body()
            val token = user?.token
            // Save token for future authenticated requests
            Log.d("API", "Login successful, token: $token")
        } else {
            Log.e("API", "Login failed: ${response.code()}")
        }
    }
    
    /**
     * Example: Create an order
     */
    suspend fun exampleCreateOrder(orderRequest: OrderRequest) {
        val response = ApiClient.api.createOrder(orderRequest)
        if (response.isSuccessful) {
            val order = response.body()
            Log.d("API", "Order created with ID: ${order?.id}")
        } else {
            Log.e("API", "Order creation failed: ${response.code()}")
        }
    }
}
