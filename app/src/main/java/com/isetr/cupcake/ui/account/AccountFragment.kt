package com.isetr.cupcake.ui.account

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.isetr.cupcake.R
import com.isetr.cupcake.data.local.UserEntity
import com.isetr.cupcake.databinding.ActivityAccountBinding
import com.isetr.cupcake.viewmodel.AccountViewModel
import com.isetr.cupcake.ui.FooterFragment

class AccountFragment : Fragment() {

    private lateinit var binding: ActivityAccountBinding
    private lateinit var viewModel: AccountViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.activity_account, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (activity as? AppCompatActivity)?.supportActionBar?.hide()


        viewModel = ViewModelProvider(this, AccountViewModel.Factory(requireActivity().application))
            .get(AccountViewModel::class.java)

        // Observe current user and bind
        viewModel.currentUser.observe(viewLifecycleOwner) { user ->
            user?.let {
                binding.user = it // Bind the whole UserEntity object
            }
        }

        // Observe messages (Toast)
        viewModel.message.observe(viewLifecycleOwner) { msg ->
            msg?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
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
            findNavController().navigate(R.id.action_accountFragment_to_authFragment)
        }

        binding.btnOrders.setOnClickListener {
            // Navigate to OrdersFragment
            findNavController().navigate(R.id.action_accountFragment_to_ordersFragment)
        }

        if (savedInstanceState == null) {
            childFragmentManager.beginTransaction()
                .replace(R.id.footer_container, FooterFragment())
                .commit()
        }

    }

    private fun showDeleteConfirmationDialog() {
        val passwordInput = EditText(requireContext())
        passwordInput.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        passwordInput.hint = getString(R.string.password)

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Confirm Deletion")
            .setMessage("Please enter your password to confirm account deletion.")
            .setView(passwordInput)
            .setPositiveButton("Delete") { _, _ ->
                val enteredPassword = passwordInput.text.toString()
                val currentUser = viewModel.currentUser.value
                if (currentUser != null && currentUser.password == enteredPassword) {
                    viewModel.deleteAccount {
                        findNavController().navigate(R.id.action_accountFragment_to_authFragment)
                    }
                } else {
                    Toast.makeText(requireContext(), "Incorrect password", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .create()

        dialog.show()
    }
}
