package com.isetr.cupcake.data.repository

import android.content.Context
import com.isetr.cupcake.data.local.AppDatabase
import com.isetr.cupcake.data.local.UserEntity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class AuthRepository(context: Context) {

    private val userDao = AppDatabase.getInstance(context).userDao()

    // Firebase
    /*
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    */
    // -----------------------
    // Register a user (Room)
    // -----------------------
    suspend fun registerUser(user: UserEntity): Boolean {
        val existing = userDao.getUserByEmail(user.email)
        return if (existing == null) {
            userDao.insertUser(user)
            true
        } else false
    }

    // -----------------------
    // Login a user (Room)
    // -----------------------
    suspend fun loginUser(email: String, password: String): UserEntity? {
        return userDao.login(email, password)
    }

    // -----------------------
    // Firebase registration
    // -----------------------
    /*
    suspend fun registerUserFirebase(
        nom: String,
        prenom: String,
        email: String,
        adresse: String,
        telephone: String,
        password: String
    ): Boolean {
        return try {
            // Create account in Firebase Auth
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val uid = result.user?.uid ?: return false

            // Store additional user info in Firestore
            val userMap = mapOf(
                "nom" to nom,
                "prenom" to prenom,
                "email" to email,
                "adresse" to adresse,
                "telephone" to telephone
            )

            db.collection("users").document(uid).set(userMap).await()
            true
        } catch (e: Exception) {
            throw e
        }
    }

     */

    // -----------------------
    // Firebase login
    // -----------------------
    /*
    suspend fun loginUserFirebase(email: String, password: String): Map<String, String>? {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val uid = result.user?.uid ?: return null

            val doc = db.collection("users").document(uid).get().await()
            if (doc.exists()) {
                mapOf(
                    "nom" to doc.getString("nom").orEmpty(),
                    "prenom" to doc.getString("prenom").orEmpty(),
                    "email" to doc.getString("email").orEmpty(),
                    "adresse" to doc.getString("adresse").orEmpty(),
                    "telephone" to doc.getString("telephone").orEmpty()
                )
            } else null
        } catch (e: Exception) {
            throw e
        }
    }

     */

    // -----------------------
    // Get current user (Room cache)
    // -----------------------
    suspend fun getCurrentUser(): UserEntity? {
        return userDao.getLastUser()
    }

    // -----------------------
    // Update user info
    // -----------------------
    suspend fun updateUser(user: UserEntity) {
        userDao.updateUser(user)
    }
    /*
    suspend fun updateUserFirebase(user: UserEntity) {
        try {
            val usersQuery = db.collection("users").whereEqualTo("email", user.email).get().await()
            if (usersQuery.documents.isNotEmpty()) {
                val docId = usersQuery.documents[0].id
                val userMap = mapOf(
                    "nom" to user.nom,
                    "prenom" to user.prenom,
                    "email" to user.email,
                    "adresse" to user.adresse,
                    "telephone" to user.telephone
                )
                db.collection("users").document(docId).set(userMap).await()
            }
        } catch (e: Exception) {
            throw e
        }
    }

     */

    // -----------------------
    // Delete user
    // -----------------------
    suspend fun deleteUser(user: UserEntity) {
        userDao.deleteUser(user)
    }
    /*
    suspend fun deleteUserFirebase(user: UserEntity) {
        try {
            val usersQuery = db.collection("users").whereEqualTo("email", user.email).get().await()
            if (usersQuery.documents.isNotEmpty()) {
                val docId = usersQuery.documents[0].id
                db.collection("users").document(docId).delete().await()
            }

            // Delete from FirebaseAuth if logged in
            val firebaseUser = auth.currentUser
            if (firebaseUser != null && firebaseUser.email == user.email) {
                firebaseUser.delete().await()
            }
        } catch (e: Exception) {
            throw e
        }
    }
     */
}
