package com.isetr.cupcake.ui.welcome

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.isetr.cupcake.R
import com.isetr.cupcake.data.local.CartEntity
import com.isetr.cupcake.data.local.Pastry
import com.isetr.cupcake.databinding.ActivityWelcomeBinding
import com.isetr.cupcake.ui.FooterFragment
import com.isetr.cupcake.ui.products.DataItem
import com.isetr.cupcake.ui.products.PastryAdapter
import com.isetr.cupcake.viewmodel.AccountViewModel
import com.isetr.cupcake.viewmodel.PastryListState
import com.isetr.cupcake.viewmodel.PastryProductsViewModel

class WelcomeFragment : Fragment() {

    private lateinit var binding: ActivityWelcomeBinding
    private val viewModel: PastryProductsViewModel by viewModels()
    private lateinit var accountViewModel: AccountViewModel
    private var currentUserId: Int = -1

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.activity_welcome, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        accountViewModel = ViewModelProvider(this, AccountViewModel.Factory(requireActivity().application))
            .get(AccountViewModel::class.java)

        accountViewModel.currentUser.observe(viewLifecycleOwner) { user ->
            if (user != null) {
                currentUserId = user.id
            }
        }
        accountViewModel.loadCurrentUser()

        val nom = arguments?.getString("nom") ?: ""
        val prenom = arguments?.getString("prenom") ?: ""
        binding.userName = "$prenom $nom"

        setupOnSaleProductsRecyclerView()

        binding.cardAllProducts.setOnClickListener {
            findNavController().navigate(R.id.action_welcomeFragment_to_pastryProductsFragment)
        }

        if (savedInstanceState == null) {
            childFragmentManager.beginTransaction()
                .replace(R.id.footer_container, FooterFragment())
                .commit()
        }
    }

    private fun setupOnSaleProductsRecyclerView() {
        val layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.rvOnSaleProducts.layoutManager = layoutManager

        val adapter = PastryAdapter(
            onDetailClick = { pastry -> showPastryDescription(pastry) },
            onAddToCartClick = { pastry ->
                if (currentUserId > 0) {
                    // --- CORRECTION : AJOUT DES INFOS DE REMISE ---
                    val cartItem = CartEntity(
                        productId = pastry.id,
                        userId = currentUserId,
                        name = pastry.name,
                        price = pastry.price,
                        quantity = 1,
                        imageRes = pastry.imageRes,
                        inPromotion = pastry.inPromotion,
                        discountRate = pastry.discountRate
                    )
                    viewModel.addToCart(cartItem)
                    Toast.makeText(requireContext(), "${pastry.name} ajouté au panier", Toast.LENGTH_SHORT).show()
                }
            }
        )
        binding.rvOnSaleProducts.adapter = adapter

        viewModel.pastriesState.observe(viewLifecycleOwner) { state ->
            if (state is PastryListState.Success) {
                val onSaleItems = state.data.filterIsInstance<DataItem.PastryItem>().filter { it.pastry.inPromotion }
                adapter.submitList(onSaleItems)
            }
        }
    }

    private fun showPastryDescription(pastry: Pastry) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_pastry_details, null)
        val dialogImage = dialogView.findViewById<android.widget.ImageView>(R.id.dialog_pastry_image)
        val dialogName = dialogView.findViewById<TextView>(R.id.dialog_pastry_name)
        val dialogDescription = dialogView.findViewById<TextView>(R.id.dialog_pastry_description)
        val closeButton = dialogView.findViewById<android.widget.Button>(R.id.dialog_close_button)

        dialogImage.setImageResource(pastry.imageRes)
        dialogName.text = pastry.name
        dialogDescription.text = pastry.description

        val alertDialog = AlertDialog.Builder(requireContext()).setView(dialogView).create()
        alertDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        closeButton.setOnClickListener { alertDialog.dismiss() }
        alertDialog.show()
    }
}
