package com.isetr.cupcake.utils

import org.mindrot.jbcrypt.BCrypt

object PasswordUtil {

    // Hash a plain password
    fun hash(password: String): String {
        return BCrypt.hashpw(password, BCrypt.gensalt())
    }

    // Verify a plain password against the hashed password
    fun verify(password: String, hashedPassword: String): Boolean {
        return BCrypt.checkpw(password, hashedPassword)
    }
}
