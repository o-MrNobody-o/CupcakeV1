package com.isetr.cupcake.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.isetr.cupcake.data.local.AppDatabase
import com.isetr.cupcake.data.local.CartEntity
import com.isetr.cupcake.data.local.Pastry
import com.isetr.cupcake.data.mapper.PastryMapper
import com.isetr.cupcake.data.network.RetrofitClient
import com.isetr.cupcake.data.repository.CartRepository
import com.isetr.cupcake.data.repository.PastryRepository
import com.isetr.cupcake.ui.products.DataItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

sealed class PastryListState {
    object Loading : PastryListState()
    data class Success(val data: List<DataItem>, val categories: List<String>) : PastryListState()
    data class Error(val message: String) : PastryListState()
}

class PastryProductsViewModel(application: Application) : AndroidViewModel(application) {

    private val _pastriesState = MutableLiveData<PastryListState>()
    val pastriesState: LiveData<PastryListState> = _pastriesState

    private var _allPastries: List<Pastry> = emptyList()
    private var _searchQuery: String? = null
    private var _selectedCategory: String? = null

    // REPOSITORIES
    private val cartRepository = CartRepository(application.applicationContext)
    private val pastryRepository: PastryRepository

    init {
        // Initialisation du repository avec ses dépendances
        val database = AppDatabase.getInstance(application)
        pastryRepository = PastryRepository(
            api = RetrofitClient.api,
            dao = database.pastryDao(),
            mapper = PastryMapper(application)
        )

        observeDatabase()
        refreshDataFromApi()
    }

    // Observer la base de données Room en temps réel
    private fun observeDatabase() {
        viewModelScope.launch {
            pastryRepository.allPastries.collect { pastries ->
                _allPastries = pastries
                applyFilters()
            }
        }
    }

    // Déclencher la mise à jour depuis l'API
    private fun refreshDataFromApi() {
        viewModelScope.launch {
            _pastriesState.value = PastryListState.Loading
            pastryRepository.refreshPastries()
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery = query
        applyFilters()
    }

    fun setCategoryFilter(category: String?) {
        _selectedCategory = category
        applyFilters()
    }

    private fun applyFilters() {
        val categoryFilteredList = if (_selectedCategory == null || _selectedCategory == "Toutes") {
            _allPastries
        } else {
            _allPastries.filter { it.category.equals(_selectedCategory, ignoreCase = true) }
        }

        val finalList = if (_searchQuery.isNullOrBlank()) {
            categoryFilteredList
        } else {
            categoryFilteredList.filter { it.name.contains(_searchQuery!!, ignoreCase = true) }
        }

        updateUi(finalList)
    }

    private fun updateUi(pastries: List<Pastry>) {
        val dataItems = mutableListOf<DataItem>()
        val pastriesByCategory = pastries.groupBy { it.category }

        if (_selectedCategory != null && _selectedCategory != "Toutes") {
            pastries.forEach { pastry -> dataItems.add(DataItem.PastryItem(pastry)) }
        } else {
            pastriesByCategory.forEach { (category, pastryItems) ->
                dataItems.add(DataItem.HeaderItem(category))
                pastryItems.forEach { pastry -> dataItems.add(DataItem.PastryItem(pastry)) }
            }
        }

        val categories = listOf("Toutes") + _allPastries.map { it.category }.distinct()
        _pastriesState.value = PastryListState.Success(dataItems, categories)
    }

    fun addToCart(cartItem: CartEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            cartRepository.addOrUpdateCartItem(cartItem)
        }
    }
}
