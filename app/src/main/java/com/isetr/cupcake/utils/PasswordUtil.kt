package com.isetr.cupcake.utils

import org.mindrot.jbcrypt.BCrypt

/**
 * PasswordUtil: Secure password hashing utility using BCrypt.
 * 
 * Why BCrypt?
 * - Industry standard for password hashing
 * - Built-in salt generation
 * - Configurable work factor (cost)
 * - Resistant to rainbow table attacks
 * 
 * Usage:
 * ```
 * // When registering a user
 * val hashedPassword = PasswordUtil.hash(plainPassword)
 * 
 * // When verifying login
 * val isValid = PasswordUtil.verify(inputPassword, storedHash)
 * ```
 * 
 * IMPORTANT: Never store plain text passwords!
 */
object PasswordUtil {
    
    // BCrypt cost factor (10-12 is typical for production)
    // Higher = more secure but slower
    private const val BCRYPT_COST = 12
    
    /**
     * Hash a plain text password using BCrypt.
     * 
     * @param password The plain text password to hash
     * @return The hashed password (includes salt)
     */
    fun hash(password: String): String {
        return BCrypt.hashpw(password, BCrypt.gensalt(BCRYPT_COST))
    }

    /**
     * Verify a plain text password against a stored hash.
     * 
     * @param password The plain text password to verify
     * @param hashedPassword The stored BCrypt hash
     * @return true if password matches, false otherwise
     */
    fun verify(password: String, hashedPassword: String): Boolean {
        return try {
            BCrypt.checkpw(password, hashedPassword)
        } catch (e: Exception) {
            // Invalid hash format or other error
            false
        }
    }
    
    /**
     * Check if a password meets minimum requirements.
     * 
     * @param password The password to validate
     * @return Error message if invalid, null if valid
     */
    fun validatePassword(password: String): String? {
        return when {
            password.length < 6 -> "Password must be at least 6 characters"
            password.length > 128 -> "Password is too long"
            else -> null
        }
    }
    
    /**
     * Check if a string looks like a valid BCrypt hash.
     * Useful for debugging/validation.
     */
    fun isValidBCryptHash(hash: String): Boolean {
        // BCrypt hashes start with $2a$, $2b$, or $2y$ and are 60 chars
        return hash.length == 60 && hash.startsWith("\$2")
    }
}
