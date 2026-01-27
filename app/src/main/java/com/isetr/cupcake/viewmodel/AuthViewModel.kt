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
                // TENTATIVE VIA SERVEUR
                val user = repository.loginRemote(email, passwordEntered)
                if (user != null) {
                    _currentUser.value = user
                    _success.value = true
                } else {
                    _error.value = "Identifiants incorrects"
                }
            } catch (e: Exception) {
                // TENTATIVE HORS LIGNE (ROOM)
                val localUser = repository.getUserByEmail(email)
                if (localUser != null && PasswordUtil.verify(passwordEntered, localUser.password)) {
                    repository.loginUser(localUser)
                    _currentUser.value = localUser
                    _success.value = true
                } else {
                    // MESSAGE PERSONNALISÉ DEMANDÉ
                    _error.value = "Compte introuvable ou mot de passe incorrect"
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
        if (nom.isBlank() || prenom.isBlank() || email.isBlank() || 
            adresse.isBlank() || telephone.isBlank() || passwordEntered.isBlank()) {
            _error.value = "Tous les champs sont obligatoires"
            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _error.value = "Format d'email invalide"
            return
        }

        if (telephone.length != 8 || !telephone.all { it.isDigit() }) {
            _error.value = "Le téléphone doit contenir 8 chiffres"
            return
        }

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
                _error.value = "Erreur serveur, inscription impossible"
            } finally {
                _loading.value = false
            }
        }
    }

    fun clearError() { _error.value = null }
}
