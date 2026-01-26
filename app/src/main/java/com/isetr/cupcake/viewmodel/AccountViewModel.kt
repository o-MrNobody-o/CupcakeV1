package com.isetr.cupcake.viewmodel

import android.content.Context
import androidx.lifecycle.*
import com.isetr.cupcake.data.repository.AuthRepository
import com.isetr.cupcake.data.local.UserEntity
import com.isetr.cupcake.session.SessionManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

/**
 * AccountViewModel: Manages user profile and account operations.
 * 
 * KEY FIX: currentUser is now REACTIVE to session changes.
 * 
 * Previous bug:
 * - currentUser was set once via loadCurrentUser()
 * - When user switched, WelcomeFragment showed old user's name
 * - AccountFragment showed stale data after login switch
 * 
 * New behavior:
 * - currentUser automatically updates when session changes
 * - When user logs out, currentUser becomes null instantly
 * - When new user logs in, currentUser shows their data instantly
 * 
 * Responsibilities:
 * - Load and display current user profile (REACTIVE)
 * - Update user information
 * - Change password
 * - Delete account (with verification)
 * - Logout
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AccountViewModel(private val context: Context) : ViewModel() {

    private val repository = AuthRepository(context)

    // ==================== UI STATE ====================
    
    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> = _loading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _success = MutableLiveData<Boolean>()
    val success: LiveData<Boolean> = _success

    private val _message = MutableLiveData<String?>()
    val message: LiveData<String?> = _message
    
    // ==================== SESSION STATE (REACTIVE) ====================
    
    /**
     * Expose session state for UI decisions.
     * Automatically updates when session changes.
     */
    val sessionState: LiveData<SessionManager.SessionInfo> = 
        repository.getSessionManager().sessionFlow.asLiveData()
    
    /**
     * Simple boolean for "is logged in" checks.
     * Automatically updates when session changes.
     */
    val isLoggedIn: LiveData<Boolean> = 
        repository.getSessionManager().isLoggedInFlow.asLiveData()
    
    /**
     * REACTIVE: Current user that automatically updates when session changes.
     * 
     * Uses flatMapLatest to:
     * 1. Observe activeUserIdFlow (who is logged in)
     * 2. When userId changes, cancel old query, load new user from Room
     * 3. Emit null if no user is logged in
     * 
     * This fixes:
     * - WelcomeFragment showing old user's name after login switch
     * - AccountFragment showing stale profile data
     */
    val currentUser: LiveData<UserEntity?> = repository.getSessionManager().activeUserIdFlow
        .flatMapLatest { userId ->
            if (userId == SessionManager.NO_USER) {
                // No user logged in - emit null
                flowOf(null)
            } else {
                // Load user from Room as Flow (auto-updates if user data changes)
                repository.getUserByIdFlow(userId)
            }
        }
        .asLiveData()

    // ==================== LOAD USER ====================
    
    /**
     * @deprecated No longer needed - currentUser auto-updates via Flow.
     * Kept for backward compatibility.
     */
    @Deprecated("currentUser now auto-updates via Flow. This method is no longer needed.")
    fun loadCurrentUser() {
        // No-op: currentUser is now reactive via Flow
        // This method is kept for backward compatibility with existing code
    }

    // ==================== UPDATE PROFILE ====================
    
    /**
     * Update user profile information.
     */
    fun updateUserInfo(
        nom: String,
        prenom: String,
        email: String,
        adresse: String,
        telephone: String = ""
    ) {
        viewModelScope.launch {
            try {
                _loading.value = true
                
                val user = currentUser.value
                    ?: throw Exception("No user logged in")
                
                // Validate input
                if (nom.isBlank() || prenom.isBlank() || email.isBlank() || adresse.isBlank()) {
                    _error.value = "All fields are required"
                    return@launch
                }
                
                val updatedUser = user.copy(
                    nom = nom.trim(),
                    prenom = prenom.trim(),
                    email = email.trim().lowercase(),
                    adresse = adresse.trim(),
                    telephone = telephone.trim().ifBlank { user.telephone }
                )
                
                val result = repository.updateUser(updatedUser)
                
                result.fold(
                    onSuccess = {
                        // currentUser will auto-update via Flow
                        _message.value = "Profile updated successfully"
                        _success.value = true
                    },
                    onFailure = { exception ->
                        _error.value = "Update failed: ${exception.message}"
                    }
                )
            } catch (e: Exception) {
                _error.value = "Update failed: ${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }
    
    // ==================== CHANGE PASSWORD ====================
    
    /**
     * Change user password.
     * Requires current password verification.
     */
    fun changePassword(
        currentPassword: String,
        newPassword: String,
        confirmPassword: String
    ) {
        viewModelScope.launch {
            try {
                _loading.value = true
                
                // Validate input
                when {
                    currentPassword.isBlank() -> {
                        _error.value = "Current password is required"
                        return@launch
                    }
                    newPassword.isBlank() -> {
                        _error.value = "New password is required"
                        return@launch
                    }
                    newPassword.length < 6 -> {
                        _error.value = "New password must be at least 6 characters"
                        return@launch
                    }
                    newPassword != confirmPassword -> {
                        _error.value = "New passwords do not match"
                        return@launch
                    }
                }
                
                val userId = repository.getActiveUserId()
                if (userId == SessionManager.NO_USER) {
                    _error.value = "Not logged in"
                    return@launch
                }
                
                val result = repository.updatePassword(userId, currentPassword, newPassword)
                
                result.fold(
                    onSuccess = {
                        _message.value = "Password changed successfully"
                        _success.value = true
                    },
                    onFailure = { exception ->
                        _error.value = exception.message ?: "Password change failed"
                    }
                )
            } catch (e: Exception) {
                _error.value = "Password change failed: ${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }

    // ==================== DELETE ACCOUNT ====================
    
    /**
     * Delete account with password verification.
     * Removes all user data and clears session.
     */
    fun deleteAccount(password: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                _loading.value = true
                
                if (password.isBlank()) {
                    _error.value = "Password is required"
                    return@launch
                }
                
                val result = repository.deleteAccount(password)
                
                result.fold(
                    onSuccess = {
                        // currentUser will become null via Flow automatically
                        _message.value = "Account deleted successfully"
                        _success.value = true
                        onSuccess()
                    },
                    onFailure = { exception ->
                        _error.value = exception.message ?: "Account deletion failed"
                    }
                )
            } catch (e: Exception) {
                _error.value = "Account deletion failed: ${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }
    
    /**
     * Soft delete account (deactivate without removing data).
     */
    fun deactivateAccount(password: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                _loading.value = true
                
                if (password.isBlank()) {
                    _error.value = "Password is required"
                    return@launch
                }
                
                val result = repository.softDeleteAccount(password)
                
                result.fold(
                    onSuccess = {
                        // currentUser will become null via Flow automatically
                        _message.value = "Account deactivated successfully"
                        _success.value = true
                        onSuccess()
                    },
                    onFailure = { exception ->
                        _error.value = exception.message ?: "Account deactivation failed"
                    }
                )
            } catch (e: Exception) {
                _error.value = "Account deactivation failed: ${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }

    // ==================== LOGOUT ====================
    
    /**
     * Logout: Clear session without deleting account.
     * currentUser will become null automatically via Flow.
     */
    fun logout() {
        viewModelScope.launch {
            try {
                repository.logout()
                // currentUser will become null via Flow automatically
            } catch (e: Exception) {
                _error.value = "Logout failed: ${e.message}"
            }
        }
    }

    // ==================== UTILITY ====================
    
    fun clearError() {
        _error.value = null
    }

    fun clearMessage() {
        _message.value = null
    }
    
    fun clearSuccess() {
        _success.value = false
    }
}
