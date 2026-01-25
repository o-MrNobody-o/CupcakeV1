package com.isetr.cupcake.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.isetr.cupcake.R
import com.isetr.cupcake.data.local.CartEntity
import com.isetr.cupcake.data.remote.ApiClient
import com.isetr.cupcake.data.repository.CartRepository
import com.isetr.cupcake.data.repository.PastryRepository
import com.isetr.cupcake.data.model.Pastry
import com.isetr.cupcake.ui.products.DataItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

    private val cartRepository = CartRepository(application.applicationContext)
    private val pastryRepository = PastryRepository(application.applicationContext)
    private val api = ApiClient.api

    init {
        fetchPastries()
    }

    /**
     * Fetch pastries from REST API, then cache to Room for offline access.
     * Falls back to Room if API call fails.
     */
    private fun fetchPastries() {
        viewModelScope.launch(Dispatchers.IO) {
            // Set loading state on main thread
            withContext(Dispatchers.Main) {
                _pastriesState.value = PastryListState.Loading
            }

            try {
                // Attempt to fetch from API
                val response = api.getAllPastries()
                
                if (response.isSuccessful) {
                    val apiPastries = response.body()?.let { sanitizePastries(it) }
                    
                    if (!apiPastries.isNullOrEmpty()) {
                        // Update Room database for offline access
                        pastryRepository.clearAll()
                        pastryRepository.insertAll(apiPastries)
                        
                        // Update in-memory list
                        _allPastries = apiPastries
                        
                        Log.d("PastryViewModel", "Fetched ${apiPastries.size} pastries from API")
                    } else {
                        // Empty response from API, try to load from Room
                        loadFromRoomOrSeedData()
                    }
                } else {
                    // API returned error, fallback to Room
                    Log.e("PastryViewModel", "API error: ${response.code()} - ${response.message()}")
                    loadFromRoomOrSeedData()
                }
                
                // Update UI on main thread
                withContext(Dispatchers.Main) {
                    applyFilters()
                }
                
            } catch (e: Exception) {
                // Network or other error, fallback to Room
                Log.e("PastryViewModel", "Exception fetching pastries: ${e.message}", e)
                
                try {
                    loadFromRoomOrSeedData()
                    
                    // Update UI on main thread
                    withContext(Dispatchers.Main) {
                        applyFilters()
                    }
                } catch (roomError: Exception) {
                    // Even Room failed, show error state
                    Log.e("PastryViewModel", "Room error: ${roomError.message}", roomError)
                    withContext(Dispatchers.Main) {
                        _pastriesState.value = PastryListState.Error(
                            "Impossible de charger les pâtisseries. Vérifiez votre connexion."
                        )
                    }
                }
            }
        }
    }

    /**
     * Load pastries from Room database, or seed with dummy data if empty.
     * Also re-seeds if the base URL has changed (detects localhost vs network IP).
     */
    private suspend fun loadFromRoomOrSeedData() {
        // Check if Room database has pastries
        if (pastryRepository.isEmpty()) {
            // Seed with dummy data for first-time use
            val dummyPastries = getDummyPastries()
            pastryRepository.insertAll(dummyPastries)
            _allPastries = dummyPastries
            Log.d("PastryViewModel", "Seeded ${dummyPastries.size} dummy pastries")
        } else {
            // Load from Room
            _allPastries = pastryRepository.getAllPastries()
            
            // Check if URLs need updating (detect localhost vs actual IP)
            val firstPastry = _allPastries.firstOrNull()
            if (firstPastry != null && firstPastry.imageUrl.contains("localhost")) {
                Log.d("PastryViewModel", "Detected old localhost URLs, re-seeding with new base URL")
                pastryRepository.clearAll()
                val updatedPastries = getDummyPastries()
                pastryRepository.insertAll(updatedPastries)
                _allPastries = updatedPastries
                Log.d("PastryViewModel", "Re-seeded ${updatedPastries.size} pastries with updated URLs")
            } else {
                Log.d("PastryViewModel", "Loaded ${_allPastries.size} pastries from Room")
            }
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

    /**
     * Normalize pastry data coming from API: trim fields and null-out blank image URLs
     * so Glide uses placeholders instead of logging failures for empty strings.
     */
    private fun sanitizePastries(pastries: List<Pastry>): List<Pastry> =
        pastries.map { pastry ->
            val cleanedImageUrl = pastry.imageUrl.trim().takeIf { it.isNotBlank() } ?: ""
            pastry.copy(imageUrl = cleanedImageUrl)
        }

    private fun applyFilters() {
        val categoryFilteredList = if (_selectedCategory.isNullOrBlank() || _selectedCategory == "Toutes") {
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

        if (!_selectedCategory.isNullOrBlank() && _selectedCategory != "Toutes") {
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

    // -------------------
    // CART FUNCTIONS
    // -------------------
    fun addToCart(cartItem: CartEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            cartRepository.addOrUpdateCartItem(cartItem)
        }
    }

    fun getCartItems(userId: Int, callback: (List<CartEntity>) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val items = cartRepository.getCartItemsForUser(userId)
            withContext(Dispatchers.Main) {
                callback(items)
            }
        }
    }

    private fun getDummyPastries(): List<Pastry> {
        // Use same base URL as ApiClient for image paths
        // If your backend is at a different IP, update this to match
        val baseUrl = "http://192.168.1.123:3000/images/"
        // For emulator testing: use "http://10.0.2.2:3000/images/"
        // Without backend: Glide will fail gracefully with placeholder; Room fallback handles data
        return listOf(
            // 🧁 Cupcakes
            Pastry("1", "Cupcake Vanille", 5.0, "${baseUrl}cupvanille.jpg", true,
                "Cupcake moelleux parfumé à la vanille avec une crème douce et légère.",
                false, 0, "Cupcakes"),
            Pastry("2", "Cupcake Chocolat", 5.5, "${baseUrl}capchocolat.jpg", true,
                "Cupcake fondant au chocolat intense, idéal pour les amateurs de cacao.",
                false, 0, "Cupcakes"),
            Pastry("3", "Cupcake Citron", 5.5, "${baseUrl}caplemon.jpg", true,
                "Cupcake frais et légèrement acidulé au goût naturel de citron.",
                false, 0, "Cupcakes"),
            Pastry("4", "Cupcake Noisette Chocolat", 6.0, "${baseUrl}capnoisettechocolat.jpg", true,
                "Cupcake gourmand mêlant chocolat fondant et éclats de noisette.",
                true, 10, "Cupcakes"),
            Pastry("5", "Cupcake Noix Cacao", 6.0, "${baseUrl}capnoixcaco.jpg", true,
                "Cupcake riche en saveurs avec noix croquantes et cacao intense.",
                false, 0, "Cupcakes"),
            Pastry("6", "Cupcake Spéculoos", 6.5, "${baseUrl}capspeculos.jpg", true,
                "Cupcake onctueux au spéculoos, au goût épicé et caramelisé.",
                true, 15, "Cupcakes"),

            // 🎂 Gâteaux
            Pastry("7", "Gâteau Chocolat", 18.0, "${baseUrl}gateauchocolat.jpg", true,
                "Gâteau fondant au chocolat, riche et généreux.",
                false, 0, "Gâteaux"),
            Pastry("8", "Gâteau Chocolat Blanc", 19.0, "${baseUrl}gateauchocolatblanc.jpg", true,
                "Gâteau doux et crémeux au chocolat blanc.",
                false, 0, "Gâteaux"),
            Pastry("9", "Gâteau Caramel", 20.0, "${baseUrl}gateaucaramel.jpg", true,
                "Gâteau nappé de caramel fondant au goût délicieusement sucré.",
                true, 20, "Gâteaux"),
            Pastry("10", "Gâteau Vanille", 17.0, "${baseUrl}gateauvanille.jpg", true,
                "Gâteau léger et parfumé à la vanille naturelle.",
                false, 0, "Gâteaux"),
            Pastry("11", "Gâteau Citron", 18.0, "${baseUrl}gateaulemon.jpg", true,
                "Gâteau moelleux au citron, frais et légèrement acidulé.",
                false, 0, "Gâteaux"),
            Pastry("12", "Gâteau Noisette", 19.0, "${baseUrl}gateaunoisette.jpg", true,
                "Gâteau savoureux à la noisette, à la texture fondante.",
                false, 0, "Gâteaux"),
            Pastry("13", "Gâteau Fruits", 21.0, "${baseUrl}gateauufruit.jpg", true,
                "Gâteau garni de fruits frais et colorés de saison.",
                false, 0, "Gâteaux"),
            Pastry("14", "Gâteau Framboise", 22.0, "${baseUrl}gateaurasbery.jpg", true,
                "Gâteau fruité à la framboise, doux et légèrement acidulé.",
                true, 10, "Gâteaux"),
            Pastry("15", "Gâteau Red Velvet", 23.0, "${baseUrl}gateuredvelvet.jpg", true,
                "Gâteau red velvet moelleux avec une crème onctueuse.",
                false, 0, "Gâteaux"),

            // 🥐 Viennoiseries
            Pastry("16", "Croissant Nature", 2.5, "${baseUrl}croissantnaature.jpg", true,
                "Croissant pur beurre, croustillant à l’extérieur et fondant à l’intérieur.",
                false, 0, "Viennoiseries"),
            Pastry("17", "Croissant Chocolat", 3.0, "${baseUrl}croissantchocolat.jpg", true,
                "Croissant fourré au chocolat fondant.",
                false, 0, "Viennoiseries"),
            Pastry("18", "Croissant Crème Amande", 3.5, "${baseUrl}croissantcremeamande.jpg", true,
                "Croissant garni d’une délicieuse crème d’amande.",
                false, 0, "Viennoiseries"),
            Pastry("19", "Pain au Chocolat", 3.0, "${baseUrl}vinoiseriepainchocolat.jpg", true,
                "Pain au chocolat croustillant avec un cœur fondant.",
                false, 0, "Viennoiseries"),
            Pastry("20", "Brioche Nature", 3.5, "${baseUrl}vinoiseriebriochenature.jpg", true,
                "Brioche moelleuse et légèrement sucrée.",
                false, 0, "Viennoiseries"),
            Pastry("21", "Chausson aux Pommes", 3.5, "${baseUrl}vinoiseriechaussonpomme.jpg", true,
                "Chausson croustillant garni de compote de pommes.",
                false, 0, "Viennoiseries"),

            // 🥧 Tartes
            Pastry("22", "Tarte Citron Meringuée", 7.5, "${baseUrl}tarteaucitronmeringu.jpg", true,
                "Tarte au citron acidulée surmontée d’une meringue légère.",
                true, 10, "Tartes"),
            Pastry("23", "Tarte Fraises", 8.0, "${baseUrl}tartefraises.jpg", true,
                "Tarte gourmande aux fraises fraîches.",
                false, 0, "Tartes"),
            Pastry("24", "Tarte aux Fruits", 8.5, "${baseUrl}tartefruit.jpg", true,
                "Tarte colorée garnie de fruits de saison.",
                false, 0, "Tartes"),

            // 🍪 Macarons
            Pastry("25", "Macaron Chocolat", 3.0, "${baseUrl}maccaronchocolat.jpg", true,
                "Macaron croquant au chocolat avec un cœur fondant.",
                false, 0, "Macarons"),
            Pastry("26", "Macaron Pistache", 3.0, "${baseUrl}maccaronpistache.jpg", true,
                "Macaron délicat à la pistache au goût raffiné.",
                false, 0, "Macarons"),
            Pastry("27", "Macaron Framboise", 3.0, "${baseUrl}maccaronrasbery.jpg", true,
                "Macaron fruité à la framboise, doux et parfumé.",
                false, 0, "Macarons")
        )
    }
}
