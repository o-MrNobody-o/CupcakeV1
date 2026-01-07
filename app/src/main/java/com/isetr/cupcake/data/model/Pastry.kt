package com.isetr.cupcake.data.model

import androidx.annotation.DrawableRes

data class Pastry(
    val id: String = "",
    val name: String = "",
    val price: Double = 0.0,
    @DrawableRes val imageRes: Int, // Utiliser l'ID de la ressource drawable
    val available: Boolean = true,
    val description: String = "",
    val inPromotion: Boolean = false,
    val discountRate: Int = 0, // en pourcentage
    val category: String = ""
)
