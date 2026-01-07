package com.isetr.cupcake.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.isetr.cupcake.R
import com.isetr.cupcake.databinding.ActivityWelcomeBinding
import com.isetr.cupcake.ui.account.AccountActivity
import com.isetr.cupcake.ui.auth.AuthActivity
// Correction : Utilisation du nom de classe et du chemin qui existent réellement
import com.isetr.cupcake.ui.prodcuts.PastryProdcuts

class WelcomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWelcomeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_welcome)

        val nom = intent.getStringExtra("EXTRA_NOM") ?: ""
        val prenom = intent.getStringExtra("EXTRA_PRENOM") ?: ""
        binding.userName = "$prenom $nom"

        binding.btnProducts.setOnClickListener {
            // Correction : Lancement de la classe qui existe réellement
            startActivity(Intent(this, PastryProdcuts::class.java))
        }

        binding.btnAccount.setOnClickListener {
            startActivity(Intent(this, AccountActivity::class.java))
        }

        binding.btnLogout.setOnClickListener {
            val intent = Intent(this, AuthActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }
}
