package com.isetr.cupcake.data.repository

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import com.isetr.cupcake.data.local.AppDatabase
import com.isetr.cupcake.data.local.UserEntity
import com.isetr.cupcake.session.SessionManager
import com.isetr.cupcake.utils.PasswordUtil
import kotlinx.coroutines.flow.Flow

/**
 * AuthRepository: Central point for authentication and user management.
 * 
 * Responsibilities:
 * 1. Login: Verify credentials → Update SessionManager
 * 2. Register: Validate → Hash password → Insert to Room → Auto-login
 * 3. Logout: Clear SessionManager (NOT Room data)
 * 4. Delete Account: Delete from Room → Clear SessionManager
 * 
 * Key Principles:
 * - SessionManager tracks WHO is logged in (session state)
 * - Room stores WHAT data exists (persistent data)
 * - Repository coordinates between them
 * - ViewModel should not access SessionManager/Room directly
 */
class AuthRepository(context: Context) {
    
    private val userDao = AppDatabase.getInstance(context).userDao()
    private val cartDao = AppDatabase.getInstance(context).cartDao()
    private val orderDao = AppDatabase.getInstance(context).orderDao()
    private val sessionManager = SessionManager(context)
    
    // ==================== AUTHENTICATION ====================
    
    /**
     * Login with email and password.
     * Returns the user if credentials are valid, null otherwise.
     * Automatically updates SessionManager on success.
     */
    suspend fun login(email: String, password: String): Result<UserEntity> {
        return try {
            val user = userDao.getUserByEmail(email)
                ?: return Result.failure(Exception("User not found"))
            
            if (!PasswordUtil.verify(password, user.password)) {
                return Result.failure(Exception("Invalid password"))
            }
            
            // Update session
            sessionManager.login(user.id, user.email, user.fullName)
            
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Register a new user.
     * Password is hashed before storage.
     * Auto-logs in the user on success.
     */
    suspend fun register(
        nom: String,
        prenom: String,
        email: String,
        adresse: String,
        telephone: String,
        password: String
    ): Result<UserEntity> {
        return try {
            // Check if email already exists
            if (userDao.isEmailRegistered(email)) {
                return Result.failure(Exception("Email already registered"))
            }
            
            // Hash password
            val hashedPassword = PasswordUtil.hash(password)
            
            // Create user entity
            val user = UserEntity(
                nom = nom,
                prenom = prenom,
                email = email,
                adresse = adresse,
                telephone = telephone,
                password = hashedPassword
            )
            
            // Insert and get the auto-generated ID
            val userId = userDao.insertUser(user)
            
            // Retrieve the inserted user with the correct ID
            val insertedUser = userDao.getUserById(userId.toInt())
                ?: return Result.failure(Exception("Failed to retrieve user after registration"))
            
            // Auto-login
            sessionManager.login(insertedUser.id, insertedUser.email, insertedUser.fullName)
            
            Result.success(insertedUser)
        } catch (e: SQLiteConstraintException) {
            Result.failure(Exception("Email already registered"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Logout the current user.
     * Clears session but preserves Room data.
     */
    suspend fun logout() {
        sessionManager.logout()
    }
    
    // ==================== USER QUERIES ====================
    
    /**
     * Get user by email (for login verification).
     */
    suspend fun getUserByEmail(email: String): UserEntity? {
        return userDao.getUserByEmail(email)
    }
    
    /**
     * Get user by ID.
     */
    suspend fun getUserById(userId: Int): UserEntity? {
        return userDao.getUserById(userId)
    }
    
    /**
     * Get user by ID as Flow (reactive).
     */
    fun getUserByIdFlow(userId: Int): Flow<UserEntity?> {
        return userDao.getUserByIdFlow(userId)
    }
    
    /**
     * Get the currently logged-in user.
     * Uses SessionManager to find active userId, then loads from Room.
     */
    suspend fun getCurrentUser(): UserEntity? {
        val userId = sessionManager.getActiveUserId()
        if (userId == SessionManager.NO_USER) return null
        return userDao.getUserById(userId)
    }
    
    /**
     * Check if there's a valid session.
     */
    suspend fun isLoggedIn(): Boolean {
        return sessionManager.isLoggedIn()
    }
    
    // ==================== USER UPDATES ====================
    
    /**
     * Update user profile information.
     */
    suspend fun updateUser(user: UserEntity): Result<UserEntity> {
        return try {
            val updatedUser = user.copy(updatedAt = System.currentTimeMillis())
            userDao.updateUser(updatedUser)
            
            // Update session with new name if changed
            sessionManager.login(updatedUser.id, updatedUser.email, updatedUser.fullName)
            
            Result.success(updatedUser)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Update user password.
     * Requires current password verification.
     */
    suspend fun updatePassword(
        userId: Int,
        currentPassword: String,
        newPassword: String
    ): Result<Unit> {
        return try {
            val user = userDao.getUserById(userId)
                ?: return Result.failure(Exception("User not found"))
            
            // Verify current password
            if (!PasswordUtil.verify(currentPassword, user.password)) {
                return Result.failure(Exception("Current password is incorrect"))
            }
            
            // Hash and save new password
            val hashedPassword = PasswordUtil.hash(newPassword)
            userDao.updatePassword(userId, hashedPassword)
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // ==================== ACCOUNT DELETION ====================
    
    /**
     * Delete account with password verification.
     * Deletes user data from Room and clears session.
     */
    suspend fun deleteAccount(password: String): Result<Unit> {
        return try {
            val userId = sessionManager.getActiveUserId()
            if (userId == SessionManager.NO_USER) {
                return Result.failure(Exception("Not logged in"))
            }
            
            val user = userDao.getUserById(userId)
                ?: return Result.failure(Exception("User not found"))
            
            // Verify password
            if (!PasswordUtil.verify(password, user.password)) {
                return Result.failure(Exception("Incorrect password"))
            }
            
            // Delete user's cart items
            cartDao.clearCart(userId)
            
            // Delete user's orders
            orderDao.deleteOrdersForUser(userId)
            
            // Delete user (hard delete)
            userDao.deleteUserById(userId)
            
            // Clear session
            sessionManager.onAccountDeleted()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Soft delete account (mark as inactive).
     * User data is preserved but account cannot be used.
     */
    suspend fun softDeleteAccount(password: String): Result<Unit> {
        return try {
            val userId = sessionManager.getActiveUserId()
            if (userId == SessionManager.NO_USER) {
                return Result.failure(Exception("Not logged in"))
            }
            
            val user = userDao.getUserById(userId)
                ?: return Result.failure(Exception("User not found"))
            
            // Verify password
            if (!PasswordUtil.verify(password, user.password)) {
                return Result.failure(Exception("Incorrect password"))
            }
            
            // Soft delete user
            userDao.softDeleteUser(userId)
            
            // Clear session
            sessionManager.onAccountDeleted()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // ==================== SESSION ACCESS ====================
    
    /**
     * Get SessionManager for ViewModels that need session state.
     */
    fun getSessionManager() = sessionManager
    
    /**
     * Get active user ID.
     */
    suspend fun getActiveUserId(): Int = sessionManager.getActiveUserId()
    
    /**
     * Get session info.
     */
    suspend fun getSessionInfo() = sessionManager.getSessionInfo()
}
