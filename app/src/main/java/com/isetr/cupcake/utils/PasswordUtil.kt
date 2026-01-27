package com.isetr.cupcake.utils

import android.util.Log
import org.mindrot.jbcrypt.BCrypt

object PasswordUtil {

    // Hash a plain password (utilisé si besoin de hacher localement)
    fun hash(password: String): String {
        return BCrypt.hashpw(password, BCrypt.gensalt())
    }

    // Verify a plain password against the stored password
    fun verify(passwordEntered: String, storedPassword: String): Boolean {
        return try {
            if (storedPassword.startsWith("$2a$") || storedPassword.startsWith("$2b$")) {
                // Si c'est un hash BCrypt, on vérifie normalement
                BCrypt.checkpw(passwordEntered, storedPassword)
            } else {
                // Sinon (Mode Fallback Room), on compare en clair
                passwordEntered == storedPassword
            }
        } catch (e: Exception) {
            Log.e("PasswordUtil", "Erreur verification: ${e.message}")
            // En cas d'erreur de format, on tente la comparaison directe
            passwordEntered == storedPassword
        }
    }
}
