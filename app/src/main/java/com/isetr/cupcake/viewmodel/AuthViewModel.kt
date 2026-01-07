package com.isetr.cupcake.viewmodel

import android.content.Context
import androidx.lifecycle.*
import com.isetr.cupcake.data.repository.AuthRepository
import com.isetr.cupcake.data.local.UserEntity
import kotlinx.coroutines.launch

class AuthViewModel(context: Context) : ViewModel() {

    private val repository = AuthRepository(context)

    // -----------------------
    // LiveData for UI updates
    // -----------------------
    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> = _loading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _success = MutableLiveData<Boolean>()
    val success: LiveData<Boolean> = _success

    private val _currentUser = MutableLiveData<UserEntity?>()
    val currentUser: LiveData<UserEntity?> = _currentUser

    // -----------------------
    // Login
    // -----------------------
    fun onLoginClicked(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _error.value = "Please enter email and password"
            return
        }

        _loading.value = true
        viewModelScope.launch {
            try {
                // ----------- Room login -----------
                var user: UserEntity? = repository.loginUser(email, password)

                // ----------- Firebase login -----------
                /*
                if (user == null) {
                    val firebaseData = repository.loginUserFirebase(email, password)
                    if (firebaseData != null) {
                        user = UserEntity(
                            nom = firebaseData["nom"] ?: "",
                            prenom = firebaseData["prenom"] ?: "",
                            email = firebaseData["email"] ?: "",
                            adresse = firebaseData["adresse"] ?: "",
                            telephone = firebaseData["telephone"] ?: "",
                            password = password
                        )
                        // Save to local Room as cache
                        repository.registerUser(user)
                    }
                }
                */
                if (user != null) {
                    _currentUser.value = user
                    _success.value = true
                } else {
                    _error.value = "Invalid email or password"
                }
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _loading.value = false
            }
        }
    }

    // -----------------------
    // Register
    // -----------------------
    fun onRegisterClicked(
        nom: String,
        prenom: String,
        email: String,
        adresse: String,
        telephone: String,
        password: String,
        confirmPassword: String
    ) {
        if (nom.isBlank() || prenom.isBlank() || email.isBlank() ||
            adresse.isBlank() || telephone.isBlank() || password.isBlank() || confirmPassword.isBlank()
        ) {
            _error.value = "All fields are required"
            return
        }

        if (password != confirmPassword) {
            _error.value = "Passwords do not match"
            return
        }

        if (password.length < 6) {
            _error.value = "Password must be at least 6 characters"
            return
        }

        _loading.value = true
        viewModelScope.launch {
            try {
                // ----------- Firebase registration -----------
                //repository.registerUserFirebase(nom, prenom, email, adresse, telephone, password)

                // ----------- Room registration (local cache) -----------
                val user = UserEntity(
                    nom = nom,
                    prenom = prenom,
                    email = email,
                    adresse = adresse,
                    telephone = telephone,
                    password = password
                )
                repository.registerUser(user)

                _currentUser.value = user
                _success.value = true
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _loading.value = false
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}
