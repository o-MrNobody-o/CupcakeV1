package com.isetr.cupcake.data.network

import retrofit2.http.GET

interface ApiService {
    // Exemple d'endpoint pour récupérer les cupcakes
    @GET("pastries")
    suspend fun getPastries(): List<PastryDto>
}