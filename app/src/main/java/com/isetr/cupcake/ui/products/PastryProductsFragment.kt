package com.isetr.cupcake.ui.products

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.isetr.cupcake.R
import com.isetr.cupcake.ui.products.DataItem
import com.isetr.cupcake.data.local.CartEntity
import com.isetr.cupcake.data.model.Pastry
import com.isetr.cupcake.ui.FooterFragment
import com.isetr.cupcake.viewmodel.AccountViewModel
import com.isetr.cupcake.viewmodel.AccountViewModelFactory
import com.isetr.cupcake.viewmodel.PastryListState
import com.isetr.cupcake.viewmodel.PastryProductsViewModel

/**
 * PastryProductsFragment: Displays all products with add-to-cart functionality.
 * 
 * KEY FIX: Now uses reactive AccountViewModel.currentUser to get the correct userId.
 * 
 * Previous bug:
 * - currentUserId was hardcoded to 1
 * - All products added from this page went to user 1's cart regardless of who was logged in
 * 
 * New behavior:
 * - Gets current user from AccountViewModel (which is reactive to SessionManager)
 * - When user switches, the next add-to-cart uses the NEW user's ID
 * - Shows error if no user is logged in
 */
class PastryProductsFragment : Fragment() {

    private val viewModel: PastryProductsViewModel by viewModels()
    
    // KEY FIX: AccountViewModel provides reactive access to current logged-in user
    private lateinit var accountViewModel: AccountViewModel

    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var pastryAdapter: PastryAdapter
    private lateinit var searchView: androidx.appcompat.widget.SearchView
    private lateinit var categoryChipGroup: ChipGroup

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.activity_pastry_prodcuts, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        progressBar = view.findViewById(R.id.progress_bar)
        recyclerView = view.findViewById(R.id.pastry_recycler_view)
        searchView = view.findViewById(R.id.search_view)
        categoryChipGroup = view.findViewById(R.id.category_chip_group)

        // KEY FIX: Initialize AccountViewModel (scoped to activity for consistency across fragments)
        // This gives us reactive access to the current logged-in user via SessionManager
        accountViewModel = ViewModelProvider(
            requireActivity(),
            AccountViewModelFactory(requireContext())
        ).get(AccountViewModel::class.java)

        setupRecyclerView()
        setupSearchView()
        observeViewModel()

        if (savedInstanceState == null) {
            childFragmentManager.beginTransaction()
                .replace(R.id.footer_container, FooterFragment())
                .commit()
        }
    }

    private fun setupRecyclerView() {
        pastryAdapter = PastryAdapter(
            onDetailClick = { pastry -> showPastryDescription(pastry) },
            onAddToCartClick = { pastry ->
                /**
                 * KEY FIX: Get current user ID from reactive AccountViewModel
                 * 
                 * Previous bug: userId was hardcoded to 1, so all products from
                 * this page were added to user 1's cart regardless of who was logged in.
                 * 
                 * Fix: accountViewModel.currentUser is backed by a Flow that:
                 * 1. Listens to SessionManager.activeUserIdFlow
                 * 2. When userId changes (user switches), loads the new user from Room
                 * 3. currentUser.value always reflects the CURRENT logged-in user
                 * 
                 * This ensures products are always added to the correct user's cart.
                 */
                val currentUser = accountViewModel.currentUser.value
                
                if (currentUser == null) {
                    // No user logged in - show error and don't add to cart
                    Toast.makeText(requireContext(), "Veuillez vous connecter d'abord", Toast.LENGTH_SHORT).show()
                    return@PastryAdapter
                }
                
                val cartItem = CartEntity(
                    productId = pastry.id,
                    userId = currentUser.id,  // Now uses the ACTUAL current user's ID
                    name = pastry.name,
                    price = pastry.price,
                    quantity = 1,
                    imageUrl = pastry.imageUrl
                )
                // Insert into DB - will appear in this user's cart
                viewModel.addToCart(cartItem)
                Toast.makeText(requireContext(), "Produit ajouté", Toast.LENGTH_SHORT).show()
            }
        )
        recyclerView.adapter = pastryAdapter
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
    }


    private fun setupSearchView() {
        searchView.setOnQueryTextListener(object : androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                viewModel.setSearchQuery(query.orEmpty())
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                viewModel.setSearchQuery(newText.orEmpty())
                return true
            }
        })
    }

    private fun observeViewModel() {
        viewModel.pastriesState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is PastryListState.Loading -> {
                    progressBar.visibility = View.VISIBLE
                    recyclerView.visibility = View.GONE
                }
                is PastryListState.Success -> {
                    progressBar.visibility = View.GONE
                    recyclerView.visibility = View.VISIBLE
                    // Adapter now expects List<Pastry> instead of DataItem
                    val pastries = state.data
                        .filterIsInstance<DataItem.PastryItem>()
                        .map { it.pastry }
                    pastryAdapter.submitList(pastries)
                    // Mettre à jour les puces de catégorie
                    updateCategoryChips(state.categories)
                }
                is PastryListState.Error -> {
                    progressBar.visibility = View.GONE
                    recyclerView.visibility = View.GONE
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun updateCategoryChips(categories: List<String>) {
        // Ne met à jour les puces que si elles n'ont pas déjà été créées
        if (categoryChipGroup.childCount > 0) return

        categories.forEach { category ->
            val chip = Chip(requireContext())
            chip.text = category
            chip.isCheckable = true
            categoryChipGroup.addView(chip)

            // Pré-cocher la puce "Toutes"
            if (category == "Toutes") {
                chip.isChecked = true
            }
        }

        categoryChipGroup.setOnCheckedStateChangeListener { group, checkedIds ->
            if (checkedIds.isNotEmpty()) {
                val chip = group.findViewById<Chip>(checkedIds[0])
                viewModel.setCategoryFilter(chip.text.toString())
            } else {
                viewModel.setCategoryFilter(null)
            }
        }
    }

    private fun showPastryDescription(pastry: Pastry) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_pastry_details, null)

        val dialogImage = dialogView.findViewById<ImageView>(R.id.dialog_pastry_image)
        val dialogName = dialogView.findViewById<TextView>(R.id.dialog_pastry_name)
        val dialogDescription = dialogView.findViewById<TextView>(R.id.dialog_pastry_description)
        val closeButton = dialogView.findViewById<Button>(R.id.dialog_close_button)

        com.bumptech.glide.Glide.with(requireContext())
            .load(pastry.imageUrl)
            .placeholder(R.drawable.ic_launcher_foreground)
            .error(R.drawable.ic_launcher_foreground)
            .into(dialogImage)
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
}
