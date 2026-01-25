package com.isetr.cupcake.data.remote

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Singleton class for managing Retrofit instance.
 * Provides a configured Retrofit client for API calls.
 */
object ApiClient {
    
    /**
     * Base URL for API calls.
     * For physical device: use your host machine IP (e.g., 192.168.1.123)
     * For Android emulator (AVD): use 10.0.2.2 to refer to host machine
     * For testing without backend: fallback to Room is automatic
     */
    private const val BASE_URL = "http://192.168.1.123:3000/"
    // Alternative for emulator: "http://10.0.2.2:3000/"
    // Alternative for localhost: "http://localhost:3000/"
    
    /**
     * Lazy-initialized Retrofit instance.
     * Uses Gson converter for JSON serialization/deserialization.
     */
    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    
    /**
     * Provides a singleton instance of CupcakeApi.
     * Use this to make API calls throughout the app.
     */
    val api: CupcakeApi by lazy {
        retrofit.create(CupcakeApi::class.java)
    }
}
