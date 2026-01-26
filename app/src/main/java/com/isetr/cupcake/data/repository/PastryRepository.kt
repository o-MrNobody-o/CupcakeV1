package com.isetr.cupcake.data.repository

import android.util.Log
import com.isetr.cupcake.R
import com.isetr.cupcake.data.local.Pastry
import com.isetr.cupcake.data.local.PastryDao
import com.isetr.cupcake.data.mapper.PastryMapper
import com.isetr.cupcake.data.network.ApiService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class PastryRepository(
    private val api: ApiService,
    private val dao: PastryDao,
    private val mapper: PastryMapper
) {
    val allPastries: Flow<List<Pastry>> = dao.getAllPastries()

    suspend fun refreshPastries() {
        try {
            // Tentative de récupération via l'API Express
            val response = api.getPastries()
            val entities = mapper.toEntityList(response)
            dao.insertPastries(entities)
            Log.d("PastryRepository", "Données rafraîchies depuis Express")
        } catch (e: Exception) {
            // En cas d'échec, on charge les données locales par défaut
            val currentData = dao.getAllPastries().first()
            if (currentData.size < 27) {
                insertDefaultPastries()
            }
            Log.e("PastryRepository", "Échec refresh Express, chargement local: ${e.message}")
        }
    }

    private suspend fun insertDefaultPastries() {
        val fullList = listOf(
            // 🧁 Cupcakes - Utilisation de null pour imageUrl (Local)
            Pastry("1", "Cupcake Vanille", 5.0, R.drawable.cupvanille, null, true, "Cupcake moelleux parfumé à la vanille.", false, 0, "Cupcakes"),
            Pastry("2", "Cupcake Chocolat", 5.5, R.drawable.capchocolat, null, true, "Cupcake fondant au chocolat intense.", false, 0, "Cupcakes"),
            Pastry("3", "Cupcake Citron", 5.5, R.drawable.caplemon, null, true, "Cupcake frais et acidulé.", false, 0, "Cupcakes"),
            Pastry("4", "Cupcake Noisette Chocolat", 6.0, R.drawable.capnoisettechocolat, null, true, "Mélange chocolat fondant et éclats de noisette.", true, 10, "Cupcakes"),
            Pastry("5", "Cupcake Noix Cacao", 6.0, R.drawable.capnoixcaco, null, true, "Noix croquantes et cacao intense.", false, 0, "Cupcakes"),
            Pastry("6", "Cupcake Spéculoos", 6.5, R.drawable.capspeculos, null, true, "Cupcake onctueux au spéculoos.", true, 15, "Cupcakes"),

            // 🎂 Gâteaux
            Pastry("7", "Gâteau Chocolat", 18.0, R.drawable.gateauchocolat, null, true, "Gâteau fondant au chocolat, riche.", false, 0, "Gâteaux"),
            Pastry("8", "Gâteau Chocolat Blanc", 19.0, R.drawable.gateauchocolatblanc, null, true, "Gâteau doux au chocolat blanc.", false, 0, "Gâteaux"),
            Pastry("9", "Gâteau Caramel", 20.0, R.drawable.gateaucaramel, null, true, "Gâteau nappé de caramel fondant.", true, 20, "Gâteaux"),
            Pastry("10", "Gâteau Vanille", 17.0, R.drawable.gateauvanille, null, true, "Gâteau parfumé à la vanille.", false, 0, "Gâteaux"),
            Pastry("11", "Gâteau Citron", 18.0, R.drawable.gateaulemon, null, true, "Gâteau frais et acidulé.", false, 0, "Gâteaux"),
            Pastry("12", "Gâteau Noisette", 19.0, R.drawable.gateaunoisette, null, true, "Gâteau savoureux à la noisette.", false, 0, "Gâteaux"),
            Pastry("13", "Gâteau Fruits", 21.0, R.drawable.gateauufruit, null, true, "Gâteau garni de fruits frais.", false, 0, "Gâteaux"),
            Pastry("14", "Gâteau Framboise", 22.0, R.drawable.gateaurasbery, null, true, "Gâteau fruité à la framboise.", true, 10, "Gâteaux"),
            Pastry("15", "Gâteau Red Velvet", 23.0, R.drawable.gateuredvelvet, null, true, "Gâteau red velvet moelleux.", false, 0, "Gâteaux"),

            // 🥐 Viennoiseries
            Pastry("16", "Croissant Nature", 2.5, R.drawable.croissantnaature, null, true, "Croissant pur beurre.", false, 0, "Viennoiseries"),
            Pastry("17", "Croissant Chocolat", 3.0, R.drawable.croissantchocolat, null, true, "Croissant fourré au chocolat.", false, 0, "Viennoiseries"),
            Pastry("18", "Croissant Crème Amande", 3.5, R.drawable.croissantcremeamande, null, true, "Croissant à la crème d’amande.", false, 0, "Viennoiseries"),
            Pastry("19", "Pain au Chocolat", 3.0, R.drawable.vinoiseriepainchocolat, null, true, "Pain au chocolat croustillant.", false, 0, "Viennoiseries"),
            Pastry("20", "Brioche Nature", 3.5, R.drawable.vinoiseriebriochenature, null, true, "Brioche moelleuse.", false, 0, "Viennoiseries"),
            Pastry("21", "Chausson aux Pommes", 3.5, R.drawable.vinoiseriechaussonpomme, null, true, "Chausson à la compote de pommes.", false, 0, "Viennoiseries"),

            // 🥧 Tartes
            Pastry("22", "Tarte Citron Meringuée", 7.5, R.drawable.tarteaucitronmeringu, null, true, "Tarte au citron et meringue.", true, 10, "Tartes"),
            Pastry("23", "Tarte Fraises", 8.0, R.drawable.tartefraises, null, true, "Tarte aux fraises fraîches.", false, 0, "Tartes"),
            Pastry("24", "Tarte aux Fruits", 8.5, R.drawable.tartefruit, null, true, "Tarte colorée aux fruits.", false, 0, "Tartes"),

            // 🍪 Macarons
            Pastry("25", "Macaron Chocolat", 3.0, R.drawable.maccaronchocolat, null, true, "Macaron croquant au chocolat.", false, 0, "Macarons"),
            Pastry("26", "Macaron Pistache", 3.0, R.drawable.maccaronpistache, null, true, "Macaron délicat à la pistache.", false, 0, "Macarons"),
            Pastry("27", "Macaron Framboise", 3.0, R.drawable.maccaronrasbery, null, true, "Macaron fruité à la framboise.", false, 0, "Macarons")
        )
        dao.insertPastries(fullList)
    }
}
