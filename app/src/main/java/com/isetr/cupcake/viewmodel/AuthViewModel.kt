package com.isetr.cupcake.viewmodel

import android.content.Context
import android.util.Patterns
import androidx.lifecycle.*
import com.isetr.cupcake.data.repository.AuthRepository
import com.isetr.cupcake.data.local.UserEntity
import com.isetr.cupcake.utils.PasswordUtil
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
                val user = repository.loginRemote(email, passwordEntered)
                if (user != null) {
                    _currentUser.value = user
                    _success.value = true
                } else {
                    _error.value = "Email ou mot de passe incorrect"
                }
            } catch (e: Exception) {
                val localUser = repository.getUserByEmail(email)
                if (localUser != null && PasswordUtil.verify(passwordEntered, localUser.password)) {
                    repository.loginUser(localUser)
                    _currentUser.value = localUser
                    _success.value = true
                } else {
                    _error.value = "Serveur hors ligne et utilisateur inconnu en local"
                }
            } finally {
                _loading.value = false
            }
        }
    }

    fun onRegisterClicked(
        nom: String, prenom: String, email: String,
        adresse: String, telephone: String, passwordEntered: String, confirmPassword: String
    ) {
        // 1. Validation de tous les champs remplis
        if (nom.isBlank() || prenom.isBlank() || email.isBlank() || 
            adresse.isBlank() || telephone.isBlank() || passwordEntered.isBlank()) {
            _error.value = "Tous les champs sont obligatoires"
            return
        }

        // 2. Validation du format Email
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _error.value = "Format d'email invalide"
            return
        }

        // 3. Validation du téléphone (8 chiffres exactement)
        if (telephone.length != 8 || !telephone.all { it.isDigit() }) {
            _error.value = "Le numéro de téléphone doit contenir exactement 8 chiffres"
            return
        }

        // 4. Validation de la confirmation du mot de passe
        if (passwordEntered != confirmPassword) {
            _error.value = "Les mots de passe ne correspondent pas"
            return
        }

        _loading.value = true
        viewModelScope.launch {
            try {
                val user = UserEntity(
                    nom = nom, prenom = prenom, email = email,
                    adresse = adresse, telephone = telephone, password = passwordEntered
                )
                
                val result = repository.registerUser(user, passwordEntered)
                
                if (result == "success") {
                    val registeredUser = repository.getCurrentUser()
                    _currentUser.value = registeredUser
                    _success.value = true
                } else {
                    _error.value = result
                }
            } catch (e: Exception) {
                _error.value = "Impossible de joindre le serveur pour l'inscription"
            } finally {
                _loading.value = false
            }
        }
    }

    fun clearError() { _error.value = null }
}
