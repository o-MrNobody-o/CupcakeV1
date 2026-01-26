package com.isetr.cupcake.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * UserDao: Data Access Object for user operations.
 * 
 * Multi-User Support:
 * - Each query that needs the "current" user should receive userId as parameter
 * - SessionManager determines which userId to pass
 * - Never rely on "last inserted" or "first user" logic
 * 
 * Best Practices:
 * - Use suspend functions for one-shot queries
 * - Use Flow for reactive/observable queries
 * - Use transactions for multi-step operations
 */
@Dao
interface UserDao {

    // ==================== INSERT ====================
    
    /**
     * Insert a new user.
     * Returns the auto-generated ID of the inserted user.
     * Throws SQLiteConstraintException if email already exists.
     */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertUser(user: UserEntity): Long

    // ==================== QUERY ====================
    
    /**
     * Get user by email (for login verification).
     * Only returns active users.
     */
    @Query("SELECT * FROM users WHERE email = :email AND is_active = 1 LIMIT 1")
    suspend fun getUserByEmail(email: String): UserEntity?
    
    /**
     * Get user by ID (to load full user info after session restore).
     * Only returns active users.
     */
    @Query("SELECT * FROM users WHERE id = :userId AND is_active = 1")
    suspend fun getUserById(userId: Int): UserEntity?
    
    /**
     * Get user by ID as Flow (for reactive UI updates).
     * Only returns active users.
     */
    @Query("SELECT * FROM users WHERE id = :userId AND is_active = 1")
    fun getUserByIdFlow(userId: Int): Flow<UserEntity?>
    
    /**
     * Check if email is already registered.
     * Returns true if email exists (even for inactive users).
     */
    @Query("SELECT EXISTS(SELECT 1 FROM users WHERE email = :email)")
    suspend fun isEmailRegistered(email: String): Boolean
    
    /**
     * Get all active users (for admin/debug purposes).
     */
    @Query("SELECT * FROM users WHERE is_active = 1 ORDER BY created_at DESC")
    suspend fun getAllActiveUsers(): List<UserEntity>
    
    /**
     * Get all users including inactive (for admin/debug purposes).
     */
    @Query("SELECT * FROM users ORDER BY created_at DESC")
    suspend fun getAllUsers(): List<UserEntity>

    // ==================== UPDATE ====================
    
    /**
     * Update user info.
     * Make sure to update the updatedAt timestamp before calling.
     */
    @Update
    suspend fun updateUser(user: UserEntity)
    
    /**
     * Update password for a specific user.
     * Password should be hashed before calling this.
     */
    @Query("UPDATE users SET password = :hashedPassword, updated_at = :updatedAt WHERE id = :userId")
    suspend fun updatePassword(userId: Int, hashedPassword: String, updatedAt: Long = System.currentTimeMillis())

    // ==================== DELETE ====================
    
    /**
     * Soft delete: Mark user as inactive.
     * Preferred over hard delete to preserve data integrity.
     */
    @Query("UPDATE users SET is_active = 0, updated_at = :updatedAt WHERE id = :userId")
    suspend fun softDeleteUser(userId: Int, updatedAt: Long = System.currentTimeMillis())
    
    /**
     * Hard delete: Permanently remove user.
     * Use with caution - this cannot be undone.
     */
    @Query("DELETE FROM users WHERE id = :userId")
    suspend fun deleteUserById(userId: Int)

    /**
     * Delete user by entity.
     */
    @Delete
    suspend fun deleteUser(user: UserEntity)
    
    /**
     * Clear all users (WARNING: use only in testing or complete reset).
     */
    @Query("DELETE FROM users")
    suspend fun clearAllUsers()
}
