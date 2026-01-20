package com.isetr.cupcake.data.local

import androidx.room.*

@Dao
interface UserDao {

    // Insert a new user
    @Insert
    suspend fun insertUser(user: UserEntity)

    // Get user by email (for login verification)
    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): UserEntity?

    // Get the last inserted user (current user session)
    @Query("SELECT * FROM users ORDER BY id DESC LIMIT 1")
    suspend fun getLastUser(): UserEntity?

    // Update user info
    @Update
    suspend fun updateUser(user: UserEntity)

    // Delete user
    @Delete
    suspend fun deleteUser(user: UserEntity)
}
