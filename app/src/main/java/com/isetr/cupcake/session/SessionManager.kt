package com.isetr.cupcake.session

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * SessionManager: Single source of truth for current user session.
 * 
 * Uses DataStore (not SharedPreferences) because:
 * - Type-safe with Kotlin coroutines
 * - Non-blocking I/O operations
 * - Thread-safe by design
 * - Easy migration from SharedPreferences
 * 
 * Architecture:
 * - SessionManager only tracks WHO is logged in
 * - Room stores WHAT data belongs to each user
 * - Never mix session state with persistent data
 * 
 * Multi-User Support:
 * - Stores the currently active user's ID
 * - When user logs out, only clears session (not Room data)
 * - Another user can log in, and their userId becomes active
 * - Each user's cart/orders remain separate in Room (tied to userId)
 */

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_session")

class SessionManager(private val context: Context) {
    
    private val dataStore = context.dataStore
    
    companion object {
        // Session keys
        private val KEY_USER_ID = intPreferencesKey("user_id")
        private val KEY_USER_EMAIL = stringPreferencesKey("user_email")
        private val KEY_USER_NAME = stringPreferencesKey("user_name")
        private val KEY_IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")
        private val KEY_LOGIN_TIMESTAMP = longPreferencesKey("login_timestamp")
        private val KEY_LAST_ACTIVE = longPreferencesKey("last_active")
        
        // Sentinel value indicating no user is logged in
        const val NO_USER = -1
        
        // Session timeout (30 days in milliseconds)
        private const val SESSION_TIMEOUT_MS = 30L * 24 * 60 * 60 * 1000
    }
    
    // ==================== DATA CLASS ====================
    
    /**
     * SessionInfo: Represents the current session state.
     * Use this for UI binding and state checks.
     */
    data class SessionInfo(
        val userId: Int,
        val email: String,
        val name: String,
        val isLoggedIn: Boolean,
        val loginTimestamp: Long,
        val lastActive: Long
    ) {
        /**
         * Check if session is valid (not expired).
         */
        fun isSessionValid(): Boolean {
            if (!isLoggedIn || userId == NO_USER) return false
            val now = System.currentTimeMillis()
            return (now - loginTimestamp) < SESSION_TIMEOUT_MS
        }
        
        companion object {
            val EMPTY = SessionInfo(
                userId = NO_USER,
                email = "",
                name = "",
                isLoggedIn = false,
                loginTimestamp = 0,
                lastActive = 0
            )
        }
    }
    
    // ==================== FLOWS (Reactive) ====================
    
    /**
     * Observe the currently logged-in user's ID.
     * Returns NO_USER (-1) if not logged in.
     */
    val activeUserIdFlow: Flow<Int> = dataStore.data.map { preferences ->
        preferences[KEY_USER_ID] ?: NO_USER
    }
    
    /**
     * Observe the currently logged-in user's email.
     */
    val activeUserEmailFlow: Flow<String> = dataStore.data.map { preferences ->
        preferences[KEY_USER_EMAIL] ?: ""
    }
    
    /**
     * Observe the currently logged-in user's name.
     */
    val activeUserNameFlow: Flow<String> = dataStore.data.map { preferences ->
        preferences[KEY_USER_NAME] ?: ""
    }
    
    /**
     * Observe login state (simple boolean).
     */
    val isLoggedInFlow: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[KEY_IS_LOGGED_IN] == true && (preferences[KEY_USER_ID] ?: NO_USER) != NO_USER
    }
    
    /**
     * Observe complete session info (userId, email, name, timestamps).
     * Use this for comprehensive session state observation.
     */
    val sessionFlow: Flow<SessionInfo> = dataStore.data.map { preferences ->
        SessionInfo(
            userId = preferences[KEY_USER_ID] ?: NO_USER,
            email = preferences[KEY_USER_EMAIL] ?: "",
            name = preferences[KEY_USER_NAME] ?: "",
            isLoggedIn = preferences[KEY_IS_LOGGED_IN] == true,
            loginTimestamp = preferences[KEY_LOGIN_TIMESTAMP] ?: 0,
            lastActive = preferences[KEY_LAST_ACTIVE] ?: 0
        )
    }
    
    // ==================== ACTIONS ====================
    
    /**
     * Login: Save the active user session.
     * Call this after successful authentication.
     * Does NOT modify Room data.
     */
    suspend fun login(userId: Int, email: String, name: String) {
        val now = System.currentTimeMillis()
        dataStore.edit { preferences ->
            preferences[KEY_USER_ID] = userId
            preferences[KEY_USER_EMAIL] = email
            preferences[KEY_USER_NAME] = name
            preferences[KEY_IS_LOGGED_IN] = true
            preferences[KEY_LOGIN_TIMESTAMP] = now
            preferences[KEY_LAST_ACTIVE] = now
        }
    }
    
    /**
     * Logout: Clear the session.
     * Call this when user explicitly logs out.
     * Does NOT delete Room data - user can log back in.
     */
    suspend fun logout() {
        dataStore.edit { preferences ->
            preferences.remove(KEY_USER_ID)
            preferences.remove(KEY_USER_EMAIL)
            preferences.remove(KEY_USER_NAME)
            preferences[KEY_IS_LOGGED_IN] = false
            preferences.remove(KEY_LOGIN_TIMESTAMP)
            preferences.remove(KEY_LAST_ACTIVE)
        }
    }
    
    /**
     * Update last active timestamp.
     * Call this periodically to track user activity.
     */
    suspend fun updateLastActive() {
        dataStore.edit { preferences ->
            preferences[KEY_LAST_ACTIVE] = System.currentTimeMillis()
        }
    }
    
    /**
     * Called when account is deleted.
     * Clears session completely (user no longer exists).
     */
    suspend fun onAccountDeleted() {
        logout()
    }
    
    // ==================== SYNCHRONOUS GETTERS ====================
    
    /**
     * Get active user ID synchronously.
     * Prefer using activeUserIdFlow for reactive code.
     */
    suspend fun getActiveUserId(): Int {
        return activeUserIdFlow.first()
    }
    
    /**
     * Get complete session info synchronously.
     */
    suspend fun getSessionInfo(): SessionInfo {
        return sessionFlow.first()
    }
    
    /**
     * Check if user is logged in synchronously.
     */
    suspend fun isLoggedIn(): Boolean {
        return isLoggedInFlow.first()
    }
    
    /**
     * Check if current session is valid (not expired).
     */
    suspend fun isSessionValid(): Boolean {
        val session = getSessionInfo()
        return session.isSessionValid()
    }
}
