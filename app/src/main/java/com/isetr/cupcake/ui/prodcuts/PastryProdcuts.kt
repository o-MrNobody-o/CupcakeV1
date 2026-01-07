package com.isetr.cupcake.ui.prodcuts

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.isetr.cupcake.R
import com.isetr.cupcake.data.model.Pastry
import com.isetr.cupcake.viewmodel.PastryListState
import com.isetr.cupcake.viewmodel.PastryProductsViewModel

class PastryProdcuts : AppCompatActivity() {

    private val viewModel: PastryProductsViewModel by viewModels()

    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var pastryAdapter: PastryAdapter
    private lateinit var searchView: SearchView
    private lateinit var categoryChipGroup: ChipGroup

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pastry_prodcuts)

        progressBar = findViewById(R.id.progress_bar)
        recyclerView = findViewById(R.id.pastry_recycler_view)
        searchView = findViewById(R.id.search_view)
        categoryChipGroup = findViewById(R.id.category_chip_group)

        setupRecyclerView()
        setupSearchView()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        pastryAdapter = PastryAdapter { pastry ->
            showPastryDescription(pastry)
        }
        recyclerView.adapter = pastryAdapter
        recyclerView.layoutManager = LinearLayoutManager(this)
    }

    private fun setupSearchView() {
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
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
        viewModel.pastriesState.observe(this) { state ->
            when (state) {
                is PastryListState.Loading -> {
                    progressBar.visibility = View.VISIBLE
                    recyclerView.visibility = View.GONE
                }
                is PastryListState.Success -> {
                    progressBar.visibility = View.GONE
                    recyclerView.visibility = View.VISIBLE
                    pastryAdapter.submitList(state.data)
                    // Mettre à jour les puces de catégorie
                    updateCategoryChips(state.categories)
                }
                is PastryListState.Error -> {
                    progressBar.visibility = View.GONE
                    recyclerView.visibility = View.GONE
                    Toast.makeText(this, state.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun updateCategoryChips(categories: List<String>) {
        // Ne met à jour les puces que si elles n'ont pas déjà été créées
        if (categoryChipGroup.childCount > 0) return

        categories.forEach { category ->
            val chip = Chip(this)
            chip.text = category
            chip.isCheckable = true
            categoryChipGroup.addView(chip)

            // Pré-cocher la puce "Toutes"
            if (category == "Toutes") {
                chip.isChecked = true
            }
        }

        categoryChipGroup.setOnCheckedChangeListener { group, checkedId ->
            val chip = group.findViewById<Chip>(checkedId)
            if (chip != null) {
                viewModel.setCategoryFilter(chip.text.toString())
            } else {
                // Si aucune puce n'est cochée, afficher toutes les catégories
                viewModel.setCategoryFilter(null)
            }
        }
    }

    private fun showPastryDescription(pastry: Pastry) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_pastry_details, null)

        val dialogImage = dialogView.findViewById<ImageView>(R.id.dialog_pastry_image)
        val dialogName = dialogView.findViewById<TextView>(R.id.dialog_pastry_name)
        val dialogDescription = dialogView.findViewById<TextView>(R.id.dialog_pastry_description)
        val closeButton = dialogView.findViewById<Button>(R.id.dialog_close_button)

        dialogImage.setImageResource(pastry.imageRes)
        dialogName.text = pastry.name
        dialogDescription.text = pastry.description

        val alertDialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        alertDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        closeButton.setOnClickListener {
            alertDialog.dismiss()
        }

        alertDialog.show()
    }
}
