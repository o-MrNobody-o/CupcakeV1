package com.isetr.cupcake.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.isetr.cupcake.data.local.UserEntity
import com.isetr.cupcake.data.repository.AuthRepository
import kotlinx.coroutines.launch

class AccountViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AuthRepository(application.applicationContext)

    private val _currentUser = MutableLiveData<UserEntity?>()
    val currentUser: LiveData<UserEntity?> = _currentUser

    private val _message = MutableLiveData<String?>()
    val message: LiveData<String?> = _message

    fun loadCurrentUser() {
        viewModelScope.launch {
            val user = repository.getCurrentUser()
            _currentUser.value = user
        }
    }

    fun updateUserInfo(nom: String, prenom: String, email: String, adresse: String, telephone: String) {
        val user = _currentUser.value ?: return
        val updatedUser = user.copy(nom = nom, prenom = prenom, email = email, adresse = adresse, telephone = telephone)

        viewModelScope.launch {
            try {
                repository.updateUser(updatedUser)
                _currentUser.value = updatedUser
                _message.value = "Profil mis à jour"
            } catch (e: Exception) {
                _message.value = e.message
            }
        }
    }

    fun deleteAccount(onSuccess: () -> Unit) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            try {
                repository.deleteUser(user)
                _message.value = "Compte supprimé"
                onSuccess()
            } catch (e: Exception) {
                _message.value = e.message
            }
        }
    }

    // CORRECTION : Déconnexion réelle dans Room
    fun logout(onComplete: () -> Unit) {
        viewModelScope.launch {
            repository.logout() // Ferme la session dans Room (isLoggedIn = 0)
            _currentUser.value = null
            onComplete()
        }
    }

    fun clearMessage() {
        _message.value = null
    }

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
