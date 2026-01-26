package com.isetr.cupcake.data.mapper

import android.content.Context
import com.isetr.cupcake.data.local.Pastry
import com.isetr.cupcake.data.network.PastryDto

class PastryMapper(private val context: Context) {

    fun toEntity(dto: PastryDto): Pastry {
        // Base du serveur (IP + Port)
        val serverBase = "http://10.191.254.121:3000"
        
        // Construction robuste de l'URL
        val finalImageUrl = if (!dto.imageName.isNullOrEmpty()) {
            val name = dto.imageName!!
            when {
                // 1. Déjà une URL complète
                name.startsWith("http") -> name
                // 2. Le nom contient déjà /images/ (cas actuel détecté)
                name.startsWith("/images/") -> serverBase + name
                name.startsWith("images/") -> "$serverBase/$name"
                // 3. Juste le nom du fichier (ex: cup.jpg)
                else -> "$serverBase/images/${name.trimStart('/')}"
            }
        } else null

        // Secours local (recherche dans drawable)
        val resName = dto.imageName?.substringAfterLast("/")?.substringBefore(".") ?: ""
        val resId = context.resources.getIdentifier(resName, "drawable", context.packageName)

        return Pastry(
            id = dto.id,
            name = dto.name,
            price = dto.price,
            imageRes = if (resId != 0) resId else com.isetr.cupcake.R.drawable.ic_products,
            imageUrl = finalImageUrl,
            available = dto.available == 1,
            description = dto.description ?: "",
            inPromotion = dto.inPromotion == 1,
            discountRate = dto.discountRate,
            category = dto.category
        )
    }

    fun toEntityList(dtos: List<PastryDto>): List<Pastry> {
        return dtos.map { toEntity(it) }
    }
}
