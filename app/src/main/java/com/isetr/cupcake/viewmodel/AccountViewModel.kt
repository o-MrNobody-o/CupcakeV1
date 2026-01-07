package com.isetr.cupcake.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.isetr.cupcake.data.local.UserEntity
import com.isetr.cupcake.data.repository.AuthRepository
import kotlinx.coroutines.launch

/**
 * ViewModel for managing account operations:
 * - Load current user
 * - Update user info
 * - Delete account
 * - Logout
 * All operations are now local-only using Room.
 */
class AccountViewModel(application: Application) : AndroidViewModel(application) {

    // Repository for data operations
    private val repository = AuthRepository(application.applicationContext)

    // LiveData holding current user
    private val _currentUser = MutableLiveData<UserEntity?>()
    val currentUser: LiveData<UserEntity?> = _currentUser

    // LiveData for showing messages (success/error)
    private val _message = MutableLiveData<String?>()
    val message: LiveData<String?> = _message

    // -----------------------
    // Load current user from local database
    // -----------------------
    fun loadCurrentUser() {
        viewModelScope.launch {
            val user = repository.getCurrentUser() // Room cache
            _currentUser.value = user
        }
    }

    // -----------------------
    // Update current user info
    // -----------------------
    fun updateUserInfo(
        nom: String,
        prenom: String,
        email: String,
        adresse: String,
        telephone: String
    ) {
        val user = _currentUser.value
        if (user == null) {
            _message.value = "No user found"
            return
        }

        // Create updated user object
        val updatedUser = user.copy(
            nom = nom,
            prenom = prenom,
            email = email,
            adresse = adresse,
            telephone = telephone
        )

        viewModelScope.launch {
            try {
                repository.updateUser(updatedUser) // Update in Room database
                _currentUser.value = updatedUser
                _message.value = "Account updated successfully"
            } catch (e: Exception) {
                _message.value = e.message
            }
        }
    }

    // -----------------------
    // Delete current user
    // -----------------------
    fun deleteAccount(onSuccess: () -> Unit) {
        val user = _currentUser.value
        if (user == null) {
            _message.value = "No user found"
            return
        }

        viewModelScope.launch {
            try {
                repository.deleteUser(user) // Delete from Room
                _message.value = "Account deleted"
                onSuccess()
            } catch (e: Exception) {
                _message.value = e.message
            }
        }
    }

    // -----------------------
    // Logout user
    // -----------------------
    fun logout() {
        // Clear current user from memory
        _currentUser.value = null
    }

    // -----------------------
    // Clear message LiveData after showing
    // -----------------------
    fun clearMessage() {
        _message.value = null
    }

    // -----------------------
    // Factory to create ViewModel with Application context
    // -----------------------
    class Factory(private val app: Application) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(AccountViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return AccountViewModel(app) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
