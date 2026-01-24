package com.isetr.cupcake.data.mapper

import android.content.Context
import com.isetr.cupcake.data.local.Pastry
import com.isetr.cupcake.data.network.PastryDto

class PastryMapper(private val context: Context) {

    fun toEntity(dto: PastryDto): Pastry {
        // Transforme le nom de l'image (ex: "capchocolat") en ID de ressource drawable
        val resId = context.resources.getIdentifier(dto.imageName, "drawable", context.packageName)
        
        return Pastry(
            id = dto.id,
            name = dto.name,
            price = dto.price,
            imageRes = if (resId != 0) resId else com.isetr.cupcake.R.drawable.ic_products,
            available = dto.available,
            description = dto.description,
            inPromotion = dto.inPromotion,
            discountRate = dto.discountRate,
            category = dto.category
        )
    }

    fun toEntityList(dtos: List<PastryDto>): List<Pastry> {
        return dtos.map { toEntity(it) }
    }
}
