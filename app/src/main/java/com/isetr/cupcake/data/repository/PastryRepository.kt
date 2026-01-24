package com.isetr.cupcake.data.repository

import android.content.Context
import com.isetr.cupcake.data.local.AppDatabase
import com.isetr.cupcake.data.local.toPastry
import com.isetr.cupcake.data.local.toEntity
import com.isetr.cupcake.data.model.Pastry

class PastryRepository(context: Context) {
    private val pastryDao = AppDatabase.getInstance(context).pastryDao()

    /**
     * Retrieve all pastries from the database as domain models.
     */
    suspend fun getAllPastries(): List<Pastry> {
        return pastryDao.getAllPastries().map { it.toPastry() }
    }

    /**
     * Insert multiple pastries into the database.
     */
    suspend fun insertAll(pastries: List<Pastry>) {
        pastryDao.insertAll(pastries.map { it.toEntity() })
    }

    /**
     * Check if the pastries table is empty.
     */
    suspend fun isEmpty(): Boolean {
        return pastryDao.getAllPastries().isEmpty()
    }

    /**
     * Clear all pastries from the database.
     */
    suspend fun clearAll() {
        pastryDao.clearAll()
    }
}
