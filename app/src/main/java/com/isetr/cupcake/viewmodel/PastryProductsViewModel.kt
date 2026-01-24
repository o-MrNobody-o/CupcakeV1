package com.isetr.cupcake.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.isetr.cupcake.R
import com.isetr.cupcake.data.local.CartEntity
import com.isetr.cupcake.data.repository.CartRepository
import com.isetr.cupcake.data.repository.PastryRepository
import com.isetr.cupcake.data.model.Pastry
import com.isetr.cupcake.ui.products.DataItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
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

    init {
        fetchPastries()
    }

    private fun fetchPastries() {
        _pastriesState.value = PastryListState.Loading

        viewModelScope.launch(Dispatchers.IO) {
            delay(1500) // simulate loading

            // Seed dummy data if table is empty
            if (pastryRepository.isEmpty()) {
                pastryRepository.insertAll(getDummyPastries())
            }

            // Load all pastries from Room
            _allPastries = pastryRepository.getAllPastries()

            // Switch to Main thread to update UI
            withContext(Dispatchers.Main) {
                applyFilters()
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
        return listOf(
            // 🧁 Cupcakes
            Pastry("1", "Cupcake Vanille", 5.0, R.drawable.cupvanille, true,
                "Cupcake moelleux parfumé à la vanille avec une crème douce et légère.",
                false, 0, "Cupcakes"),
            Pastry("2", "Cupcake Chocolat", 5.5, R.drawable.capchocolat, true,
                "Cupcake fondant au chocolat intense, idéal pour les amateurs de cacao.",
                false, 0, "Cupcakes"),
            Pastry("3", "Cupcake Citron", 5.5, R.drawable.caplemon, true,
                "Cupcake frais et légèrement acidulé au goût naturel de citron.",
                false, 0, "Cupcakes"),
            Pastry("4", "Cupcake Noisette Chocolat", 6.0, R.drawable.capnoisettechocolat, true,
                "Cupcake gourmand mêlant chocolat fondant et éclats de noisette.",
                true, 10, "Cupcakes"),
            Pastry("5", "Cupcake Noix Cacao", 6.0, R.drawable.capnoixcaco, true,
                "Cupcake riche en saveurs avec noix croquantes et cacao intense.",
                false, 0, "Cupcakes"),
            Pastry("6", "Cupcake Spéculoos", 6.5, R.drawable.capspeculos, true,
                "Cupcake onctueux au spéculoos, au goût épicé et caramelisé.",
                true, 15, "Cupcakes"),

            // 🎂 Gâteaux
            Pastry("7", "Gâteau Chocolat", 18.0, R.drawable.gateauchocolat, true,
                "Gâteau fondant au chocolat, riche et généreux.",
                false, 0, "Gâteaux"),
            Pastry("8", "Gâteau Chocolat Blanc", 19.0, R.drawable.gateauchocolatblanc, true,
                "Gâteau doux et crémeux au chocolat blanc.",
                false, 0, "Gâteaux"),
            Pastry("9", "Gâteau Caramel", 20.0, R.drawable.gateaucaramel, true,
                "Gâteau nappé de caramel fondant au goût délicieusement sucré.",
                true, 20, "Gâteaux"),
            Pastry("10", "Gâteau Vanille", 17.0, R.drawable.gateauvanille, true,
                "Gâteau léger et parfumé à la vanille naturelle.",
                false, 0, "Gâteaux"),
            Pastry("11", "Gâteau Citron", 18.0, R.drawable.gateaulemon, true,
                "Gâteau moelleux au citron, frais et légèrement acidulé.",
                false, 0, "Gâteaux"),
            Pastry("12", "Gâteau Noisette", 19.0, R.drawable.gateaunoisette, true,
                "Gâteau savoureux à la noisette, à la texture fondante.",
                false, 0, "Gâteaux"),
            Pastry("13", "Gâteau Fruits", 21.0, R.drawable.gateauufruit, true,
                "Gâteau garni de fruits frais et colorés de saison.",
                false, 0, "Gâteaux"),
            Pastry("14", "Gâteau Framboise", 22.0, R.drawable.gateaurasbery, true,
                "Gâteau fruité à la framboise, doux et légèrement acidulé.",
                true, 10, "Gâteaux"),
            Pastry("15", "Gâteau Red Velvet", 23.0, R.drawable.gateuredvelvet, true,
                "Gâteau red velvet moelleux avec une crème onctueuse.",
                false, 0, "Gâteaux"),

            // 🥐 Viennoiseries
            Pastry("16", "Croissant Nature", 2.5, R.drawable.croissantnaature, true,
                "Croissant pur beurre, croustillant à l’extérieur et fondant à l’intérieur.",
                false, 0, "Viennoiseries"),
            Pastry("17", "Croissant Chocolat", 3.0, R.drawable.croissantchocolat, true,
                "Croissant fourré au chocolat fondant.",
                false, 0, "Viennoiseries"),
            Pastry("18", "Croissant Crème Amande", 3.5, R.drawable.croissantcremeamande, true,
                "Croissant garni d’une délicieuse crème d’amande.",
                false, 0, "Viennoiseries"),
            Pastry("19", "Pain au Chocolat", 3.0, R.drawable.vinoiseriepainchocolat, true,
                "Pain au chocolat croustillant avec un cœur fondant.",
                false, 0, "Viennoiseries"),
            Pastry("20", "Brioche Nature", 3.5, R.drawable.vinoiseriebriochenature, true,
                "Brioche moelleuse et légèrement sucrée.",
                false, 0, "Viennoiseries"),
            Pastry("21", "Chausson aux Pommes", 3.5, R.drawable.vinoiseriechaussonpomme, true,
                "Chausson croustillant garni de compote de pommes.",
                false, 0, "Viennoiseries"),

            // 🥧 Tartes
            Pastry("22", "Tarte Citron Meringuée", 7.5, R.drawable.tarteaucitronmeringu, true,
                "Tarte au citron acidulée surmontée d’une meringue légère.",
                true, 10, "Tartes"),
            Pastry("23", "Tarte Fraises", 8.0, R.drawable.tartefraises, true,
                "Tarte gourmande aux fraises fraîches.",
                false, 0, "Tartes"),
            Pastry("24", "Tarte aux Fruits", 8.5, R.drawable.tartefruit, true,
                "Tarte colorée garnie de fruits de saison.",
                false, 0, "Tartes"),

            // 🍪 Macarons
            Pastry("25", "Macaron Chocolat", 3.0, R.drawable.maccaronchocolat, true,
                "Macaron croquant au chocolat avec un cœur fondant.",
                false, 0, "Macarons"),
            Pastry("26", "Macaron Pistache", 3.0, R.drawable.maccaronpistache, true,
                "Macaron délicat à la pistache au goût raffiné.",
                false, 0, "Macarons"),
            Pastry("27", "Macaron Framboise", 3.0, R.drawable.maccaronrasbery, true,
                "Macaron fruité à la framboise, doux et parfumé.",
                false, 0, "Macarons")
        )
    }
}
