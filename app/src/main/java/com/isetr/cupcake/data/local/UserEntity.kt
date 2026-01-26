package com.isetr.cupcake.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * UserEntity represents a user in the local Room database.
 * 
 * Key Design Decisions:
 * - Email is unique (indexed) to prevent duplicate accounts
 * - Password is stored as a BCrypt hash (never plain text)
 * - createdAt/updatedAt for audit trail
 * - isActive flag for soft-delete capability
 * 
 * Multi-User Support:
 * - Each user has a unique ID (auto-generated)
 * - SessionManager tracks which user is currently logged in
 * - Cart and Orders are tied to userId (foreign key concept)
 */
@Entity(
    tableName = "users",
    indices = [Index(value = ["email"], unique = true)]
)
data class UserEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    
    @ColumnInfo(name = "nom")
    var nom: String,
    
    @ColumnInfo(name = "prenom")
    var prenom: String,
    
    @ColumnInfo(name = "email")
    var email: String,
    
    @ColumnInfo(name = "adresse")
    var adresse: String,
    
    @ColumnInfo(name = "telephone")
    var telephone: String,
    
    @ColumnInfo(name = "password")
    var password: String, // BCrypt hashed password
    
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    
    @ColumnInfo(name = "updated_at")
    var updatedAt: Long = System.currentTimeMillis(),
    
    @ColumnInfo(name = "is_active")
    var isActive: Boolean = true // Soft delete flag
) {
    /**
     * Full name for display purposes
     */
    val fullName: String
        get() = "$prenom $nom"
    
    /**
     * Check if this is the same user (by ID)
     */
    fun isSameUser(other: UserEntity?): Boolean = other?.id == this.id
}
