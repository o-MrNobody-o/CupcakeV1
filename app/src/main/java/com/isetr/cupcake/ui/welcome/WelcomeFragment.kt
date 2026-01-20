package com.isetr.cupcake.ui.welcome

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.isetr.cupcake.R
import com.isetr.cupcake.data.model.Pastry
import com.isetr.cupcake.databinding.ActivityWelcomeBinding
import com.isetr.cupcake.ui.FooterFragment
import com.isetr.cupcake.ui.products.DataItem
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

        // Setup RecyclerView for on-sale products
        setupOnSaleProductsRecyclerView()

        if (savedInstanceState == null) {
            childFragmentManager.beginTransaction()
                .replace(R.id.footer_container, FooterFragment())
                .commit()
        }
    }

    private fun setupOnSaleProductsRecyclerView() {
        val layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.rvOnSaleProducts.layoutManager = layoutManager

        // Observe the ViewModel for products
        viewModel.pastriesState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is PastryListState.Loading -> {
                    // TODO: show loading indicator if needed
                }
                is PastryListState.Success -> {
                    // Filter only on-sale products
                    val onSaleProducts: List<Pastry> = state.data
                        .filterIsInstance<DataItem.PastryItem>()
                        .map { it.pastry }
                        .filter { it.inPromotion }

                    // Setup adapter
                    val adapter = OnSaleProductAdapter(onSaleProducts)
                    binding.rvOnSaleProducts.adapter = adapter
                }
                is PastryListState.Error -> {
                    // TODO: show error message
                }
            }
        }
    }
}
