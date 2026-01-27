package com.isetr.cupcake.data.mapper

import android.content.Context
import com.isetr.cupcake.data.local.Pastry
import com.isetr.cupcake.data.network.PastryDto

class PastryMapper(private val context: Context) {

    fun toEntity(dto: PastryDto): Pastry {
        // IP mise à jour : 192.168.1.135
        val serverBase = "http://192.168.1.135:3000"
        
        val finalImageUrl = if (!dto.imageName.isNullOrEmpty()) {
            val name = dto.imageName!!
            when {
                name.startsWith("http") -> name
                name.startsWith("/images/") -> serverBase + name
                name.startsWith("images/") -> "$serverBase/$name"
                else -> "$serverBase/images/${name.trimStart('/')}"
            }
        } else null

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
