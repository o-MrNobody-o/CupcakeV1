package com.isetr.cupcake.ui.account

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.isetr.cupcake.R
import com.isetr.cupcake.data.local.UserEntity
import com.isetr.cupcake.databinding.ActivityAccountBinding
import com.isetr.cupcake.viewmodel.AccountViewModel
import com.isetr.cupcake.ui.FooterFragment
import com.isetr.cupcake.ui.order.OrderStatusActivity
import com.isetr.cupcake.ui.order.OrderHistoryActivity

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

        // Listener pour le bouton Suivre ma commande
        binding.btnTrackOrder.setOnClickListener {
            val intent = Intent(requireContext(), OrderStatusActivity::class.java)
            startActivity(intent)
        }

        // Listener pour l'historique des commandes
        binding.btnOrderHistory.setOnClickListener {
            val intent = Intent(requireContext(), OrderHistoryActivity::class.java)
            startActivity(intent)
        }

        binding.btnDeleteAccount.setOnClickListener {
            showDeleteConfirmationDialog()
        }

        binding.btnLogout.setOnClickListener {
            viewModel.logout()
            findNavController().navigate(R.id.action_accountFragment_to_authFragment)
        }
        
        if (savedInstanceState == null) {
            childFragmentManager.beginTransaction()
                .replace(R.id.footer_container, FooterFragment())
                .commit()
        }

    }

    private fun showDeleteConfirmationDialog() {
        val context = requireContext()
        
        // Conteneur avec marges
        val container = LinearLayout(context)
        container.orientation = LinearLayout.VERTICAL
        val layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        val margin = (24 * context.resources.displayMetrics.density).toInt()
        layoutParams.setMargins(margin, 8, margin, 0)

        // TextInputLayout moderne
        val textInputLayout = TextInputLayout(context, null, com.google.android.material.R.style.Widget_MaterialComponents_TextInputLayout_OutlinedBox)
        textInputLayout.hint = "Votre mot de passe"
        textInputLayout.setEndIconMode(TextInputLayout.END_ICON_PASSWORD_TOGGLE)
        textInputLayout.boxStrokeColor = Color.parseColor("#E91E63")
        textInputLayout.setHintTextColor(android.content.res.ColorStateList.valueOf(Color.parseColor("#E91E63")))
        textInputLayout.setEndIconTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#E91E63")))

        val passwordInput = TextInputEditText(textInputLayout.context)
        passwordInput.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        passwordInput.setTextColor(Color.BLACK)
        textInputLayout.addView(passwordInput)
        
        container.addView(textInputLayout, layoutParams)

        // Titre en Rose Foncé
        val title = SpannableString("Supprimer mon compte")
        title.setSpan(ForegroundColorSpan(Color.parseColor("#E91E63")), 0, title.length, 0)

        val dialog = AlertDialog.Builder(context)
            .setTitle(title)
            .setMessage("Attention : Cette action est irréversible. Pour confirmer la suppression définitive de vos données, veuillez saisir votre mot de passe.")
            .setView(container)
            .setPositiveButton("Supprimer") { _, _ ->
                val enteredPassword = passwordInput.text.toString()
                val currentUser = viewModel.currentUser.value
                if (currentUser != null && currentUser.password == enteredPassword) {
                    viewModel.deleteAccount {
                        findNavController().navigate(R.id.action_accountFragment_to_authFragment)
                    }
                } else {
                    Toast.makeText(context, "Mot de passe incorrect", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Retour", null)
            .create()

        dialog.show()

        // Style final
        dialog.window?.setBackgroundDrawableResource(android.R.color.white)
        val messageView = dialog.findViewById<TextView>(android.R.id.message)
        messageView?.setTextColor(Color.BLACK)
        
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.parseColor("#E91E63"))
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.GRAY)
    }
}
