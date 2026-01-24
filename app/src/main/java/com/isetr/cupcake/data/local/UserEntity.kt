package com.isetr.cupcake.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    var nom: String,
    var prenom: String,
    var email: String,
    var adresse: String,
    var telephone: String,
    var password: String,
    var isLoggedIn: Boolean = false // Nouveau champ pour gérer la session
)
