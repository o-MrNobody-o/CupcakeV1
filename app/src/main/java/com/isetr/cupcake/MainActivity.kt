package com.isetr.cupcake

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.isetr.cupcake.data.local.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // ✅ IMPORTANT : retirer le thème Splash
        setTheme(R.style.Theme_Cupcake)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Charger les informations de l'utilisateur pour compléter le profil
        refreshUserInfo()

        Log.d("MAIN_DEBUG", "MainActivity lancée")
    }

    private fun refreshUserInfo() {
        val db = AppDatabase.getInstance(this)
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val user = db.userDao().getActiveUser()
                withContext(Dispatchers.Main) {
                    if (user != null) {
                        Log.d("MAIN_DEBUG", "Session active pour : ${user.prenom} ${user.nom}")
                        // Le nom est maintenant disponible pour toute l'application
                    } else {
                        Log.d("MAIN_DEBUG", "Aucun utilisateur connecté")
                    }
                }
            } catch (e: Exception) {
                Log.e("MAIN_DEBUG", "Erreur chargement session : ${e.message}")
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshUserInfo()
    }
}
