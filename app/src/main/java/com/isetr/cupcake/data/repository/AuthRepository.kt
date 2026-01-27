package com.isetr.cupcake.data.repository

import android.content.Context
import android.util.Log
import com.isetr.cupcake.data.local.AppDatabase
import com.isetr.cupcake.data.local.UserEntity
import com.isetr.cupcake.data.network.RetrofitClient
import com.isetr.cupcake.data.network.UserDto
import org.json.JSONObject
import retrofit2.HttpException

class AuthRepository(context: Context) {
    private val userDao = AppDatabase.getInstance(context).userDao()
    private val api = RetrofitClient.api
    private val TAG = "AuthRepository"

    /**
     * Inscrit un utilisateur. Si le serveur est hors ligne, crée un compte local.
     */
    suspend fun registerUser(user: UserEntity, clearPassword: String): String {
        return try {
            val userDto = UserDto(
                nom = user.nom,
                prenom = user.prenom,
                email = user.email,
                adresse = user.adresse,
                telephone = user.telephone,
                password = clearPassword 
            )
            
            val response = api.register(userDto)
            val serverId = response.id ?: 0
            
            if (serverId > 0) {
                userDao.logoutAllUsers()
                val finalUser = user.copy(id = serverId, isLoggedIn = true, password = clearPassword)
                userDao.insertUser(finalUser)
                "success"
            } else "Erreur serveur"

        } catch (e: HttpException) {
            val errorBody = e.response()?.errorBody()?.string()
            val message = try { JSONObject(errorBody ?: "").getString("error") } catch (ex: Exception) { "Erreur 500" }
            if (message.contains("Duplicate entry")) "Cet email est déjà utilisé" else message

        } catch (e: Exception) {
            Log.w(TAG, "Serveur injoignable, création d'un compte local uniquement")
            val existingLocal = userDao.getUserByEmail(user.email)
            if (existingLocal != null) return "Cet email est déjà utilisé localement"

            userDao.logoutAllUsers()
            val localUser = user.copy(id = 0, isLoggedIn = true, password = clearPassword)
            userDao.insertUser(localUser)
            "success"
        }
    }

    suspend fun loginRemote(email: String, passwordEntered: String): UserEntity? {
        val credentials = mapOf("email" to email, "password" to passwordEntered)
        val remoteUser = api.login(credentials)
        
        val userEntity = UserEntity(
            id = remoteUser.id ?: 0,
            nom = remoteUser.nom,
            prenom = remoteUser.prenom,
            email = remoteUser.email,
            adresse = remoteUser.adresse ?: "",
            telephone = remoteUser.telephone ?: "",
            password = passwordEntered,
            isLoggedIn = true
        )
        userDao.logoutAllUsers()
        userDao.insertUser(userEntity)
        return userEntity
    }

    suspend fun getCurrentUser(): UserEntity? = userDao.getActiveUser()
    suspend fun logout() = userDao.logoutAllUsers()

    /**
     * Supprime le compte de l'utilisateur (Local + Serveur).
     */
    suspend fun deleteUser(user: UserEntity): Boolean {
        return try {
            // 1. Tenter la suppression sur le serveur Express (MySQL)
            // Si l'ID est > 0, c'est un compte synchronisé
            if (user.id > 0) {
                api.deleteAccountRemote(user.id)
            }
            
            // 2. Suppression locale (Room)
            userDao.deleteUser(user)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Erreur suppression distante : ${e.message}")
            // Même si le serveur échoue, on supprime au moins en local
            userDao.deleteUser(user)
            false 
        }
    }

    suspend fun getUserByEmail(email: String): UserEntity? = userDao.getUserByEmail(email)
    
    suspend fun loginUser(user: UserEntity) {
        userDao.logoutAllUsers()
        user.isLoggedIn = true
        userDao.updateUser(user)
    }

    suspend fun updateUser(user: UserEntity): Boolean {
        return try {
            val userDto = UserDto(id = user.id, nom = user.nom, prenom = user.prenom, email = user.email, adresse = user.adresse, telephone = user.telephone)
            api.updateProfile(user.id, userDto)
            userDao.updateUser(user)
            true
        } catch (e: Exception) { false }
    }
}
