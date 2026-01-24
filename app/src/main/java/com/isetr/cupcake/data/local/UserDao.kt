package com.isetr.cupcake.data.local

import androidx.room.*

@Dao
interface UserDao {

    @Insert
    suspend fun insertUser(user: UserEntity)

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): UserEntity?

    // Récupérer l'utilisateur qui a sa session active
    @Query("SELECT * FROM users WHERE isLoggedIn = 1 LIMIT 1")
    suspend fun getActiveUser(): UserEntity?

    // Mettre fin à toutes les sessions actives (déconnexion globale)
    @Query("UPDATE users SET isLoggedIn = 0")
    suspend fun logoutAllUsers()

    @Update
    suspend fun updateUser(user: UserEntity)

    @Delete
    suspend fun deleteUser(user: UserEntity)
}
