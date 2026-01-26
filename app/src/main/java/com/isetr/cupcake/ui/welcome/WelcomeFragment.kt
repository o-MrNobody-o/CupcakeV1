package com.isetr.cupcake.ui.welcome

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.isetr.cupcake.R
import com.isetr.cupcake.data.local.CartEntity
import com.isetr.cupcake.data.model.Pastry
import com.isetr.cupcake.databinding.ActivityWelcomeBinding
import com.isetr.cupcake.ui.FooterFragment
import com.isetr.cupcake.ui.products.DataItem
import com.isetr.cupcake.ui.products.PastryAdapter
import com.isetr.cupcake.viewmodel.AccountViewModel
import com.isetr.cupcake.viewmodel.AccountViewModelFactory
import com.isetr.cupcake.viewmodel.PastryListState
import com.isetr.cupcake.viewmodel.PastryProductsViewModel

/**
 * WelcomeFragment: Shows welcome message and on-sale products.
 * 
 * KEY FIX: Welcome message now updates reactively with session changes.
 * 
 * Previous bug:
 * - Used navigation arguments as primary source for user name
 * - When user switched, old name showed briefly before update
 * 
 * New behavior:
 * - accountViewModel.currentUser is reactive (Flow-backed)
 * - When session changes, userName updates instantly
 * - Navigation arguments are only used as initial value if user isn't loaded yet
 */
class WelcomeFragment : Fragment() {

    private lateinit var binding: ActivityWelcomeBinding
    private val viewModel: PastryProductsViewModel by viewModels()
    private lateinit var accountViewModel: AccountViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.activity_welcome, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (activity as? androidx.appcompat.app.AppCompatActivity)?.supportActionBar?.hide()

        // Shared ViewModel for current user - scoped to activity for consistency
        accountViewModel = ViewModelProvider(
            requireActivity(),
            AccountViewModelFactory(requireContext())
        ).get(AccountViewModel::class.java)

        /**
         * KEY FIX: Observe currentUser which is now REACTIVE.
         * 
         * accountViewModel.currentUser is backed by a Flow that:
         * 1. Listens to SessionManager.activeUserIdFlow
         * 2. When userId changes, loads the new user from Room
         * 3. Emits null when no user is logged in
         * 
         * This ensures:
         * - When user X logs out and Y logs in, userName updates instantly to Y
         * - No stale data from previous user
         */
        accountViewModel.currentUser.observe(viewLifecycleOwner) { user ->
            if (user != null) {
                // Always use the reactive user data as the source of truth
                binding.userName = "${user.prenom} ${user.nom}"
            } else {
                // User logged out - clear the welcome message
                binding.userName = ""
            }
        }
        
        // Note: We removed the fallback to navigation arguments because
        // it was causing stale data to appear briefly. The reactive
        // currentUser from AccountViewModel is now the single source of truth.
        // Navigation arguments are no longer needed for the welcome message.

        // Setup horizontal RecyclerView for on-sale products
        setupOnSaleProductsRecyclerView()

        // Add footer fragment
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
                // Use reactive currentUser instead of potentially stale value
                val currentUser = accountViewModel.currentUser.value
                if (currentUser == null) {
                    Toast.makeText(requireContext(), "Please log in first", Toast.LENGTH_SHORT).show()
                    return@PastryAdapter
                }
                
                val cartItem = CartEntity(
                    productId = pastry.id,
                    userId = currentUser.id,
                    name = pastry.name,
                    price = pastry.price,
                    quantity = 1,
                    imageUrl = pastry.imageUrl
                )
                viewModel.addToCart(cartItem)
                Toast.makeText(requireContext(), "Produit ajouté", Toast.LENGTH_SHORT).show()
            }
        )

        binding.rvOnSaleProducts.adapter = adapter

        viewModel.pastriesState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is PastryListState.Loading -> {}
                is PastryListState.Success -> {
                    val onSalePastries = state.data
                        .filterIsInstance<DataItem.PastryItem>()
                        .map { it.pastry }
                        .filter { it.inPromotion }
                    adapter.submitList(onSalePastries)
                }
                is PastryListState.Error -> {}
            }
        }
    }

    private fun showPastryDescription(pastry: Pastry) {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_pastry_details, null)
        val dialogImage = dialogView.findViewById<android.widget.ImageView>(R.id.dialog_pastry_image)
        val dialogName = dialogView.findViewById<android.widget.TextView>(R.id.dialog_pastry_name)
        val dialogDescription = dialogView.findViewById<android.widget.TextView>(R.id.dialog_pastry_description)
        val closeButton = dialogView.findViewById<android.widget.Button>(R.id.dialog_close_button)

        com.bumptech.glide.Glide.with(requireContext())
            .load(pastry.imageUrl)
            .placeholder(R.drawable.ic_launcher_foreground)
            .error(R.drawable.ic_launcher_foreground)
            .into(dialogImage)
        dialogName.text = pastry.name
        dialogDescription.text = pastry.description

        val alertDialog = androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()
        alertDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        closeButton.setOnClickListener { alertDialog.dismiss() }
        alertDialog.show()
    }
}
