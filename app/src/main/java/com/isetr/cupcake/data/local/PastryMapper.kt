package com.isetr.cupcake.data.local

import com.isetr.cupcake.data.model.Pastry

/**
 * Converts a PastryEntity from the Room database to a Pastry domain model.
 */
fun PastryEntity.toPastry(): Pastry {
    return Pastry(
        id = this.id,
        name = this.name,
        price = this.price,
        imageUrl = this.imageUrl,
        available = this.available,
        description = this.description,
        inPromotion = this.inPromotion,
        discountRate = this.discountRate,
        category = this.category
    )
}

/**
 * Converts a Pastry domain model to a PastryEntity for storing in the Room database.
 */
fun Pastry.toEntity(): PastryEntity {
    return PastryEntity(
        id = this.id,
        name = this.name,
        price = this.price,
        imageUrl = this.imageUrl,
        available = this.available,
        description = this.description,
        inPromotion = this.inPromotion,
        discountRate = this.discountRate,
        category = this.category
    )
}
