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
    private val TAG = "RetrofitDebug"

    // --- INSCRIPTION ---
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
            // On lève l'exception pour que le ViewModel puisse décider du fallback
            throw e 
        }
    }

    // --- CONNEXION (Correction : Ne pas renvoyer null si erreur RÉSEAU) ---
    suspend fun loginRemote(email: String, passwordEntered: String): UserEntity? {
        // On ne met pas de try/catch ici, on laisse l'exception remonter au ViewModel
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
    suspend fun deleteUser(user: UserEntity) = userDao.deleteUser(user)
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
