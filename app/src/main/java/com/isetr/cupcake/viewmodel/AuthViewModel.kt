package com.isetr.cupcake.viewmodel

import android.content.Context
import androidx.lifecycle.*
import com.isetr.cupcake.data.repository.AuthRepository
import com.isetr.cupcake.data.local.UserEntity
import kotlinx.coroutines.launch

class AuthViewModel(context: Context) : ViewModel() {

    private val repository = AuthRepository(context)
    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> = _loading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _success = MutableLiveData<Boolean>()
    val success: LiveData<Boolean> = _success

    private val _currentUser = MutableLiveData<UserEntity?>()
    val currentUser: LiveData<UserEntity?> = _currentUser

    fun onLoginClicked(email: String, passwordEntered: String) {
        if (email.isBlank() || passwordEntered.isBlank()) {
            _error.value = "Veuillez remplir tous les champs"
            return
        }

        _loading.value = true
        viewModelScope.launch {
            try {
                // On envoie le mot de passe tel quel, Express s'occupe de la comparaison BCrypt
                val user = repository.loginRemote(email, passwordEntered)

                if (user != null) {
                    _currentUser.value = user
                    _success.value = true
                } else {
                    _error.value = "Email ou mot de passe incorrect"
                }
            } catch (e: Exception) {
                _error.value = "Erreur de connexion : ${e.localizedMessage}"
            } finally {
                _loading.value = false
            }
        }
    }

    fun onRegisterClicked(
        nom: String, prenom: String, email: String,
        adresse: String, telephone: String, passwordEntered: String, confirmPassword: String
    ) {
        if (nom.isBlank() || prenom.isBlank() || email.isBlank() || passwordEntered.isBlank()) {
            _error.value = "Tous les champs sont obligatoires"
            return
        }

        if (passwordEntered != confirmPassword) {
            _error.value = "Les mots de passe ne correspondent pas"
            return
        }

        _loading.value = true
        viewModelScope.launch {
            try {
                // Création de l'entité locale
                val user = UserEntity(
                    nom = nom, prenom = prenom, email = email,
                    adresse = adresse, telephone = telephone, password = passwordEntered
                )
                
                // Envoi du mot de passe en clair au serveur Express
                val result = repository.registerUser(user, passwordEntered)
                
                if (result == "success") {
                    val registeredUser = repository.getCurrentUser()
                    _currentUser.value = registeredUser
                    _success.value = true
                } else {
                    _error.value = result
                }
            } catch (e: Exception) {
                _error.value = "Impossible de contacter le serveur Express"
            } finally {
                _loading.value = false
            }
        }
    }

    fun clearError() { _error.value = null }
}
