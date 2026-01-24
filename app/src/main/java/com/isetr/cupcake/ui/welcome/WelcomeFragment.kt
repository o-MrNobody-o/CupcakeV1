package com.isetr.cupcake.ui.welcome

import android.graphics.Color
import android.os.Bundle
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.isetr.cupcake.R
import com.isetr.cupcake.data.local.CartEntity
import com.isetr.cupcake.data.local.Pastry
import com.isetr.cupcake.databinding.ActivityWelcomeBinding
import com.isetr.cupcake.ui.FooterFragment
import com.isetr.cupcake.ui.products.DataItem
import com.isetr.cupcake.ui.products.PastryAdapter
import com.isetr.cupcake.viewmodel.PastryListState
import com.isetr.cupcake.viewmodel.PastryProductsViewModel

class WelcomeFragment : Fragment() {

    private lateinit var binding: ActivityWelcomeBinding
    private val viewModel: PastryProductsViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.activity_welcome, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val nom = arguments?.getString("nom") ?: ""
        val prenom = arguments?.getString("prenom") ?: ""
        binding.userName = "$prenom $nom"

        // Setup horizontal RecyclerView for on-sale products
        setupOnSaleProductsRecyclerView()

        // Listener pour voir tout le catalogue
        binding.cardAllProducts.setOnClickListener {
            findNavController().navigate(R.id.action_welcomeFragment_to_pastryProductsFragment)
        }

        // Add footer fragment
        if (savedInstanceState == null) {
            childFragmentManager.beginTransaction()
                .replace(R.id.footer_container, FooterFragment())
                .commit()
        }
    }

    private fun setupOnSaleProductsRecyclerView() {
        // Horizontal layout
        val layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.rvOnSaleProducts.layoutManager = layoutManager

        // Reuse same PastryAdapter
        val adapter = PastryAdapter(
            onDetailClick = { pastry ->
                showPastryDescription(pastry)
            },
            onAddToCartClick = { pastry ->
                val currentUserId = 1 // replace with actual logged-in user ID
                val cartItem = CartEntity(
                    productId = pastry.id,
                    userId = currentUserId,
                    name = pastry.name,
                    price = pastry.price,
                    quantity = 1,
                    imageRes = pastry.imageRes
                )
                // Insert into DB
                viewModel.addToCart(cartItem)
                
                // --- confirmation dialog ---
                showAddToCartDialog(pastry.name)
            }
        )
        binding.rvOnSaleProducts.adapter = adapter

        // Observe ViewModel
        viewModel.pastriesState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is PastryListState.Loading -> {
                    // Optionally show a loading indicator
                }
                is PastryListState.Success -> {
                    // Convert to DataItem.PastryItem and filter onPromotion
                    val onSaleItems: List<DataItem> = state.data
                        .filterIsInstance<DataItem.PastryItem>() // keep only pastry items
                        .filter { it.pastry.inPromotion }       // only on-sale pastries
                    adapter.submitList(onSaleItems)
                }

                is PastryListState.Error -> {
                    // Optionally show error message
                }
            }
        }
    }

    private fun showPastryDescription(pastry: Pastry) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_pastry_details, null)

        val dialogImage = dialogView.findViewById<android.widget.ImageView>(R.id.dialog_pastry_image)
        val dialogName = dialogView.findViewById<android.widget.TextView>(R.id.dialog_pastry_name)
        val dialogDescription = dialogView.findViewById<android.widget.TextView>(R.id.dialog_pastry_description)
        val closeButton = dialogView.findViewById<android.widget.Button>(R.id.dialog_close_button)

        dialogImage.setImageResource(pastry.imageRes)
        dialogName.text = pastry.name
        dialogDescription.text = pastry.description

        val alertDialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        alertDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        closeButton.setOnClickListener {
            alertDialog.dismiss()
        }

        alertDialog.show()
    }

    private fun showAddToCartDialog(productName: String) {
        val title = SpannableString("Succès !")
        title.setSpan(
            ForegroundColorSpan(Color.parseColor("#E91E63")),
            0,
            title.length,
            0
        )

        val alertDialog = AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setMessage("$productName a été ajouté au panier avec succès.")
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }
            .create()

        alertDialog.show()

        // Fond blanc
        alertDialog.window?.setBackgroundDrawableResource(android.R.color.white)
        
        // Texte du message en noir
        alertDialog.findViewById<TextView>(android.R.id.message)?.setTextColor(Color.BLACK)
        
        // Bouton OK en rose
        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.parseColor("#E91E63"))
    }
}
