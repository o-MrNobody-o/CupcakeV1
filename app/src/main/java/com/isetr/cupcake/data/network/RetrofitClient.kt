package com.isetr.cupcake.data.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    // Port 3000 pour Express et votre IP actuelle 10.191.254.121
    private const val BASE_URL = "http://10.191.254.121:3000/" 

    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(logging)
        .connectTimeout(5, TimeUnit.SECONDS) // Changé à 5 secondes
        .readTimeout(5, TimeUnit.SECONDS)    // Changé à 5 secondes
        .writeTimeout(5, TimeUnit.SECONDS)   // Ajouté timeout d'écriture
        .build()

    val api: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()
            .create(ApiService::class.java)
    }
}
