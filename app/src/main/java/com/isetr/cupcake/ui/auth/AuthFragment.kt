package com.isetr.cupcake.ui.auth

import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.isetr.cupcake.R
import com.isetr.cupcake.databinding.ActivityAuthBinding
import com.isetr.cupcake.viewmodel.AuthViewModel
import com.isetr.cupcake.viewmodel.AuthViewModelFactory

/**
 * AuthFragment: Handles login and registration UI.
 * 
 * Features:
 * - Toggle between login and register views with animation
 * - Input validation feedback
 * - Loading state management
 * - Session restoration check on start
 */
class AuthFragment : Fragment() {

    private lateinit var binding: ActivityAuthBinding
    private lateinit var viewModel: AuthViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.activity_auth, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Hide action bar for auth screens
        (activity as? AppCompatActivity)?.supportActionBar?.hide()

        // Initialize ViewModel
        viewModel = ViewModelProvider(
            this,
            AuthViewModelFactory(requireActivity().applicationContext)
        )[AuthViewModel::class.java]

        // Check for existing session
        checkExistingSession()

        // Setup UI
        setupViewToggle()
        setupPasswordVisibility()
        setupClickListeners()
        observeViewModel()
    }
    
    /**
     * Check if user already has a valid session.
     * If so, skip auth and navigate to main content.
     */
    private fun checkExistingSession() {
        viewModel.restoreSession { hasValidSession ->
            if (hasValidSession) {
                // User already logged in, navigate to main content
                viewModel.currentUser.value?.let { user ->
                    navigateToIntro(user.nom, user.prenom)
                }
            }
        }
    }

    /**
     * Setup toggle between login and register views with smooth animation.
     */
    private fun setupViewToggle() {
        binding.tvGoRegister.setOnClickListener {
            binding.layoutLogin.animate()
                .alpha(0f)
                .setDuration(300)
                .withEndAction {
                    binding.layoutLogin.visibility = View.GONE
                    binding.layoutRegister.alpha = 0f
                    binding.layoutRegister.visibility = View.VISIBLE
                    binding.layoutRegister.animate()
                        .alpha(1f)
                        .setDuration(300)
                        .start()
                    clearInputFields()
                    viewModel.clearError()
                }
                .start()
        }

        binding.tvGoLogin.setOnClickListener {
            binding.layoutRegister.animate()
                .alpha(0f)
                .setDuration(300)
                .withEndAction {
                    binding.layoutRegister.visibility = View.GONE
                    binding.layoutLogin.alpha = 0f
                    binding.layoutLogin.visibility = View.VISIBLE
                    binding.layoutLogin.animate()
                        .alpha(1f)
                        .setDuration(300)
                        .start()
                    clearInputFields()
                    viewModel.clearError()
                }
                .start()
        }
    }

    /**
     * Setup password visibility toggle.
     */
    private fun setupPasswordVisibility() {
        binding.cbShowPassword.setOnCheckedChangeListener { _, isChecked ->
            val inputType = if (isChecked) {
                InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            } else {
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            }

            binding.etRegPassword.inputType = inputType
            binding.etRegConfirmPassword.inputType = inputType

            // Maintain cursor position
            binding.etRegPassword.setSelection(binding.etRegPassword.text.length)
            binding.etRegConfirmPassword.setSelection(binding.etRegConfirmPassword.text.length)
        }
    }

    /**
     * Setup button click listeners.
     */
    private fun setupClickListeners() {
        // Login button
        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString()
            val password = binding.etPassword.text.toString()
            viewModel.onLoginClicked(email, password)
        }

        // Register button
        binding.btnRegister.setOnClickListener {
            val nom = binding.etRegNom.text.toString()
            val prenom = binding.etRegPrenom.text.toString()
            val email = binding.etRegEmail.text.toString()
            val adresse = binding.etRegAdresse.text.toString()
            val telephone = binding.etRegTelephone.text.toString()
            val password = binding.etRegPassword.text.toString()
            val confirmPassword = binding.etRegConfirmPassword.text.toString()

            viewModel.onRegisterClicked(
                nom = nom,
                prenom = prenom,
                email = email,
                adresse = adresse,
                telephone = telephone,
                password = password,
                confirmPassword = confirmPassword
            )
        }
    }

    /**
     * Observe ViewModel LiveData for UI updates.
     */
    private fun observeViewModel() {
        // Loading state
        viewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            binding.btnLogin.isEnabled = !isLoading
            binding.btnRegister.isEnabled = !isLoading
            
            // Optional: Show progress indicator
            // binding.progressBar.isVisible = isLoading
        }

        // Error messages
        viewModel.error.observe(viewLifecycleOwner) { errorMsg ->
            errorMsg?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                viewModel.clearError()
            }
        }

        // Login success
        viewModel.loginSuccess.observe(viewLifecycleOwner) { isSuccess ->
            if (isSuccess) {
                viewModel.currentUser.value?.let { user ->
                    navigateToIntro(user.nom, user.prenom)
                }
                viewModel.clearSuccess()
            }
        }
        
        // Register success
        viewModel.registerSuccess.observe(viewLifecycleOwner) { isSuccess ->
            if (isSuccess) {
                viewModel.currentUser.value?.let { user ->
                    Toast.makeText(
                        requireContext(),
                        "Welcome, ${user.prenom}!",
                        Toast.LENGTH_SHORT
                    ).show()
                    navigateToIntro(user.nom, user.prenom)
                }
                viewModel.clearSuccess()
            }
        }
    }
    
    /**
     * Navigate to intro screen after successful auth.
     */
    private fun navigateToIntro(nom: String, prenom: String) {
        val action = AuthFragmentDirections.actionAuthFragmentToIntroVideoFragment(
            nom = nom,
            prenom = prenom
        )
        findNavController().navigate(action)
    }
    
    /**
     * Clear all input fields.
     */
    private fun clearInputFields() {
        // Login fields
        binding.etEmail.text?.clear()
        binding.etPassword.text?.clear()
        
        // Register fields
        binding.etRegNom.text?.clear()
        binding.etRegPrenom.text?.clear()
        binding.etRegEmail.text?.clear()
        binding.etRegAdresse.text?.clear()
        binding.etRegTelephone.text?.clear()
        binding.etRegPassword.text?.clear()
        binding.etRegConfirmPassword.text?.clear()
        binding.cbShowPassword.isChecked = false
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        // Clear references to avoid memory leaks
    }
}