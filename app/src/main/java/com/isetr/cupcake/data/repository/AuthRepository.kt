package com.isetr.cupcake.data.repository

import android.content.Context
import com.isetr.cupcake.data.local.AppDatabase
import com.isetr.cupcake.data.local.UserEntity

class AuthRepository(context: Context) {
    private val userDao = AppDatabase.getInstance(context).userDao()

    // Register a user (Room)
    suspend fun registerUser(user: UserEntity): Boolean {
        val existing = userDao.getUserByEmail(user.email)
        return if (existing == null) {
            userDao.insertUser(user)
            true
        } else false
    }
    // New method: get user by email only
    suspend fun getUserByEmail(email: String): UserEntity? {
        return userDao.getUserByEmail(email)
    }
    // Get current user (Room cache)
    suspend fun getCurrentUser(): UserEntity? {
        return userDao.getLastUser()
    }
    // Update user info
    suspend fun updateUser(user: UserEntity) {
        userDao.updateUser(user)
    }

    // Delete user
    suspend fun deleteUser(user: UserEntity) {
        userDao.deleteUser(user)
    }
}
