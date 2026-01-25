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
import com.isetr.cupcake.viewmodel.PastryListState
import com.isetr.cupcake.viewmodel.PastryProductsViewModel

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

        // Shared ViewModel for current user
        accountViewModel = ViewModelProvider(
            requireActivity(),
            AccountViewModel.Factory(requireActivity().application)
        ).get(AccountViewModel::class.java)

        // Observe current user
        accountViewModel.currentUser.observe(viewLifecycleOwner) { user ->
            user?.let {
                binding.userName = "${it.prenom} ${it.nom}"
            }
        }

        // Fallback for first-time nav arguments
        val nomArg = arguments?.getString("nom")
        val prenomArg = arguments?.getString("prenom")
        if (!nomArg.isNullOrEmpty() && !prenomArg.isNullOrEmpty()) {
            binding.userName = "$prenomArg $nomArg"
        }

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
                val currentUserId = accountViewModel.currentUser.value?.id ?: return@PastryAdapter
                val cartItem = CartEntity(
                    productId = pastry.id,
                    userId = currentUserId,
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
