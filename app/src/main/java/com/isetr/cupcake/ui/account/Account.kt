package com.isetr.cupcake.ui.account

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.isetr.cupcake.R
import com.isetr.cupcake.data.local.UserEntity
import com.isetr.cupcake.databinding.ActivityAccountBinding
import com.isetr.cupcake.ui.auth.AuthActivity
import com.isetr.cupcake.viewmodel.AccountViewModel

class AccountActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAccountBinding
    private lateinit var viewModel: AccountViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_account)

        viewModel = ViewModelProvider(this, AccountViewModel.Factory(application))
            .get(AccountViewModel::class.java)

        // Observe current user and bind
        viewModel.currentUser.observe(this) { user ->
            user?.let {
                binding.user = it // Bind the whole UserEntity object
            }
        }


        // Observe messages (Toast)
        viewModel.message.observe(this) { msg ->
            msg?.let {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
                viewModel.clearMessage()
            }
        }

        // Load current user
        viewModel.loadCurrentUser()

        // Buttons
        binding.btnUpdateAccount.setOnClickListener {
            binding.user?.let { user ->
                viewModel.updateUserInfo(
                    nom = user.nom,
                    prenom = user.prenom,
                    email = user.email,
                    adresse = user.adresse,
                    telephone = user.telephone
                )
            }
        }

        binding.btnDeleteAccount.setOnClickListener {
            showDeleteConfirmationDialog()
        }

        binding.btnLogout.setOnClickListener {
            viewModel.logout()
            val intent = Intent(this, AuthActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    private fun showDeleteConfirmationDialog() {
        val passwordInput = EditText(this)
        passwordInput.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        passwordInput.hint = getString(R.string.password)

        val dialog = AlertDialog.Builder(this)
            .setTitle("Confirm Deletion")
            .setMessage("Please enter your password to confirm account deletion.")
            .setView(passwordInput)
            .setPositiveButton("Delete") { _, _ ->
                val enteredPassword = passwordInput.text.toString()
                val currentUser = viewModel.currentUser.value
                if (currentUser != null && currentUser.password == enteredPassword) {
                    viewModel.deleteAccount {
                        val intent = Intent(this, AuthActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
                    }
                } else {
                    Toast.makeText(this, "Incorrect password", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .create()

        dialog.show()
    }
}
