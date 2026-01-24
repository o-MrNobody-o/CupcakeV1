package com.isetr.cupcake.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [UserEntity::class, CartEntity::class, OrderEntity::class], // <-- added OrderEntity
    version = 4 // <-- incremented version
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun cartDao(): CartDao // <-- added CartDao
    abstract fun orderDao(): OrderDao // <-- added OrderDao

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
                .fallbackToDestructiveMigration() // optional for dev
                .build()
    }
}
