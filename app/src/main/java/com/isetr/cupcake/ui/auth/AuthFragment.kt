package com.isetr.cupcake.ui.auth

import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.isetr.cupcake.R
import com.isetr.cupcake.databinding.ActivityAuthBinding
import com.isetr.cupcake.viewmodel.AuthViewModel
import com.isetr.cupcake.viewmodel.AuthViewModelFactory

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

        // Setup ViewModel
        viewModel = ViewModelProvider(
            this,
            AuthViewModelFactory(requireActivity().applicationContext)
        )[AuthViewModel::class.java]

        // Observe ViewModel LiveData
        observeViewModel()

        // Navigation: Login <-> Register
        binding.tvGoRegister.setOnClickListener {
            binding.layoutLogin.animate().alpha(0f).setDuration(300).withEndAction {
                binding.layoutLogin.visibility = View.GONE
                binding.layoutRegister.alpha = 0f
                binding.layoutRegister.visibility = View.VISIBLE
                binding.layoutRegister.animate().alpha(1f).setDuration(300).start()
            }.start()
        }

        binding.tvGoLogin.setOnClickListener {
            binding.layoutRegister.animate().alpha(0f).setDuration(300).withEndAction {
                binding.layoutRegister.visibility = View.GONE
                binding.layoutLogin.alpha = 0f
                binding.layoutLogin.visibility = View.VISIBLE
                binding.layoutLogin.animate().alpha(1f).setDuration(300).start()
            }.start()
        }

        // Show / hide password
        binding.cbShowPassword.setOnCheckedChangeListener { _, isChecked ->
            val type = if (isChecked)
                InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            else
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD

            binding.etRegPassword.inputType = type
            binding.etRegConfirmPassword.inputType = type

            binding.etRegPassword.setSelection(binding.etRegPassword.text.length)
            binding.etRegConfirmPassword.setSelection(binding.etRegConfirmPassword.text.length)
        }

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

    // LiveData Observers
    private fun observeViewModel() {
        // Show loading (optional: you can add a ProgressBar later)
        viewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            // For now, just disable buttons while loading
            binding.btnLogin.isEnabled = !isLoading
            binding.btnRegister.isEnabled = !isLoading
        }

        // Show error messages as Toast
        viewModel.error.observe(viewLifecycleOwner) { errorMsg ->
            errorMsg?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                viewModel.clearError() // Clear after showing
            }
        }

        // Success observer
        viewModel.success.observe(viewLifecycleOwner) { isSuccess ->
            if (isSuccess) {
                viewModel.currentUser.value?.let { user ->
                    val action = AuthFragmentDirections.actionAuthFragmentToIntroVideoFragment(
                        nom = user.nom,
                        prenom = user.prenom
                    )
                    findNavController().navigate(action)
                }
            }
        }
    }
}