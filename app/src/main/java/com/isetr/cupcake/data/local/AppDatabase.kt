package com.isetr.cupcake.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * AppDatabase: Room database for the Cupcake app.
 * 
 * Entities:
 * - UserEntity: User accounts with hashed passwords
 * - CartEntity: Shopping cart items (tied to userId)
 * - OrderEntity: Order history (tied to userId)
 * - PastryEntity: Cached pastry products
 * 
 * Version History:
 * - v6: Initial stable version
 * - v7: Added createdAt, updatedAt, isActive to UserEntity
 *       Added unique index on email
 */
@Database(
    entities = [UserEntity::class, CartEntity::class, OrderEntity::class, PastryEntity::class],
    version = 7,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun cartDao(): CartDao
    abstract fun orderDao(): OrderDao
    abstract fun pastryDao(): PastryDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: buildDatabase(context).also { instance = it }
            }

        private fun buildDatabase(context: Context) =
            Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "cupcake-db"
            )
                .fallbackToDestructiveMigration() // For development - clears DB on schema change
                .build()
    }
}
