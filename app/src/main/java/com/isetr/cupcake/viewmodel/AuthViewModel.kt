package com.isetr.cupcake.viewmodel

import android.content.Context
import androidx.lifecycle.*
import com.isetr.cupcake.data.repository.AuthRepository
import com.isetr.cupcake.data.local.UserEntity
import com.isetr.cupcake.session.SessionManager
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * AuthViewModel: Manages login, register, and session state.
 * 
 * Key Responsibilities:
 * - Login: Validate input → Call repository → Update UI state
 * - Register: Validate input → Call repository → Auto-login → Update UI state
 * - Session State: Expose session info for UI decisions
 * 
 * Best Practices:
 * - All business logic in Repository
 * - ViewModel only manages UI state
 * - Use LiveData for UI binding
 * - Use Result<T> for error handling
 */
class AuthViewModel(private val context: Context) : ViewModel() {

    private val repository = AuthRepository(context)
    
    // ==================== UI STATE ====================
    
    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> = _loading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _loginSuccess = MutableLiveData<Boolean>()
    val loginSuccess: LiveData<Boolean> = _loginSuccess
    
    private val _registerSuccess = MutableLiveData<Boolean>()
    val registerSuccess: LiveData<Boolean> = _registerSuccess

    private val _currentUser = MutableLiveData<UserEntity?>()
    val currentUser: LiveData<UserEntity?> = _currentUser
    
    // For backward compatibility
    val success: LiveData<Boolean> = MediatorLiveData<Boolean>().apply {
        addSource(_loginSuccess) { value = it }
        addSource(_registerSuccess) { value = it }
    }
    
    // ==================== SESSION STATE ====================
    
    /**
     * Expose session state for UI logic.
     * Use this to check if user is logged in.
     */
    val sessionState: LiveData<SessionManager.SessionInfo> = 
        repository.getSessionManager().sessionFlow.asLiveData()
    
    /**
     * Simple boolean for "is logged in" checks.
     */
    val isLoggedIn: LiveData<Boolean> = 
        repository.getSessionManager().isLoggedInFlow.asLiveData()
    
    // ==================== LOGIN ====================
    
    /**
     * Login with email and password.
     * 
     * Flow:
     * 1. Validate input
     * 2. Call repository.login()
     * 3. On success: Update currentUser, set loginSuccess = true
     * 4. On failure: Set error message
     */
    fun onLoginClicked(email: String, password: String) {
        // Input validation
        if (email.isBlank()) {
            _error.value = "Please enter your email"
            return
        }
        if (password.isBlank()) {
            _error.value = "Please enter your password"
            return
        }
        if (!isValidEmail(email)) {
            _error.value = "Please enter a valid email"
            return
        }

        _loading.value = true
        _loginSuccess.value = false
        
        viewModelScope.launch {
            val result = repository.login(email.trim(), password)
            
            result.fold(
                onSuccess = { user ->
                    _currentUser.value = user
                    _loginSuccess.value = true
                    _error.value = null
                },
                onFailure = { exception ->
                    _error.value = exception.message ?: "Login failed"
                    _loginSuccess.value = false
                }
            )
            
            _loading.value = false
        }
    }
    
    // ==================== REGISTER ====================
    
    /**
     * Register a new user.
     * 
     * Flow:
     * 1. Validate all input fields
     * 2. Call repository.register()
     * 3. On success: Auto-login, update currentUser, set registerSuccess = true
     * 4. On failure: Set error message
     */
    fun onRegisterClicked(
        nom: String,
        prenom: String,
        email: String,
        adresse: String,
        telephone: String,
        password: String,
        confirmPassword: String
    ) {
        // Input validation
        val validationError = validateRegistrationInput(
            nom, prenom, email, adresse, telephone, password, confirmPassword
        )
        if (validationError != null) {
            _error.value = validationError
            return
        }

        _loading.value = true
        _registerSuccess.value = false
        
        viewModelScope.launch {
            val result = repository.register(
                nom = nom.trim(),
                prenom = prenom.trim(),
                email = email.trim().lowercase(),
                adresse = adresse.trim(),
                telephone = telephone.trim(),
                password = password
            )
            
            result.fold(
                onSuccess = { user ->
                    _currentUser.value = user
                    _registerSuccess.value = true
                    _error.value = null
                },
                onFailure = { exception ->
                    _error.value = exception.message ?: "Registration failed"
                    _registerSuccess.value = false
                }
            )
            
            _loading.value = false
        }
    }
    
    // ==================== LOGOUT ====================
    
    /**
     * Logout the current user.
     * Clears session but preserves Room data.
     */
    fun logout() {
        viewModelScope.launch {
            try {
                repository.logout()
                _currentUser.value = null
                _loginSuccess.value = false
                _registerSuccess.value = false
            } catch (e: Exception) {
                _error.value = "Logout failed: ${e.message}"
            }
        }
    }
    
    // ==================== SESSION RESTORATION ====================
    
    /**
     * Load current user from session.
     * Call this on app startup to restore session.
     */
    fun loadCurrentUser() {
        viewModelScope.launch {
            try {
                val user = repository.getCurrentUser()
                _currentUser.value = user
            } catch (e: Exception) {
                _error.value = "Failed to load user: ${e.message}"
            }
        }
    }
    
    /**
     * Check if there's a valid session and load user if so.
     * Returns true if session exists and is valid.
     */
    fun restoreSession(onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val isLoggedIn = repository.isLoggedIn()
                if (isLoggedIn) {
                    val user = repository.getCurrentUser()
                    _currentUser.value = user
                    onResult(user != null)
                } else {
                    onResult(false)
                }
            } catch (e: Exception) {
                _error.value = "Session restore failed: ${e.message}"
                onResult(false)
            }
        }
    }
    
    // ==================== UTILITY ====================
    
    fun clearError() {
        _error.value = null
    }
    
    fun clearSuccess() {
        _loginSuccess.value = false
        _registerSuccess.value = false
    }
    
    // ==================== VALIDATION ====================
    
    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }
    
    private fun validateRegistrationInput(
        nom: String,
        prenom: String,
        email: String,
        adresse: String,
        telephone: String,
        password: String,
        confirmPassword: String
    ): String? {
        return when {
            nom.isBlank() -> "Last name is required"
            prenom.isBlank() -> "First name is required"
            email.isBlank() -> "Email is required"
            !isValidEmail(email) -> "Please enter a valid email"
            adresse.isBlank() -> "Address is required"
            telephone.isBlank() -> "Phone number is required"
            password.isBlank() -> "Password is required"
            password.length < 6 -> "Password must be at least 6 characters"
            confirmPassword.isBlank() -> "Please confirm your password"
            password != confirmPassword -> "Passwords do not match"
            else -> null
        }
    }
    
    // ==================== FACTORY ====================
    
    companion object {
        class Factory(private val context: Context) : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
                    @Suppress("UNCHECKED_CAST")
                    return AuthViewModel(context) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
    }
}
