package com.isetr.cupcake

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {

        // ✅ IMPORTANT : retirer le thème Splash
        setTheme(R.style.Theme_Cupcake)

        super.onCreate(savedInstanceState)

        // ✅ OBLIGATOIRE
        setContentView(R.layout.activity_main)

        Log.d("MAIN_DEBUG", "MainActivity onCreate OK")
    }

    override fun onResume() {
        super.onResume()
        Log.d("MAIN_DEBUG", "MainActivity onResume")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.e("MAIN_DEBUG", "MainActivity DESTROYED")
    }
}
