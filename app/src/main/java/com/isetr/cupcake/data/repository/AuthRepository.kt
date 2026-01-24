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
            // S'assurer qu'aucun autre utilisateur n'est connecté avant d'en créer un nouveau actif
            userDao.logoutAllUsers()
            user.isLoggedIn = true
            userDao.insertUser(user)
            true
        } else false
    }

    suspend fun getUserByEmail(email: String): UserEntity? {
        return userDao.getUserByEmail(email)
    }

    // Récupérer l'utilisateur réellement connecté (Session active)
    suspend fun getCurrentUser(): UserEntity? {
        return userDao.getActiveUser()
    }

    // Mettre à jour l'utilisateur et gérer sa session
    suspend fun loginUser(user: UserEntity) {
        userDao.logoutAllUsers() // Déconnecter tout le monde
        user.isLoggedIn = true   // Connecter cet utilisateur
        userDao.updateUser(user)
    }

    suspend fun updateUser(user: UserEntity) {
        userDao.updateUser(user)
    }

    suspend fun deleteUser(user: UserEntity) {
        userDao.deleteUser(user)
    }

    suspend fun logout() {
        userDao.logoutAllUsers()
    }
}
