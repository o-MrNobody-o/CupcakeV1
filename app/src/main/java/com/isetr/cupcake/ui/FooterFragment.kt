package com.isetr.cupcake.ui

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.isetr.cupcake.R

class FooterFragment : Fragment(R.layout.fragment_footer) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val bottomNav = view.findViewById<BottomNavigationView>(R.id.bottomNavigationView)

        // Use the NavController from the main NavHostFragment
        val navController = requireActivity().findNavController(R.id.nav_host_fragment)

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_products -> {
                    navController.navigate(R.id.action_global_pastryProducts)
                    true
                }
                R.id.navigation_account -> {
                    navController.navigate(R.id.action_global_account)
                    true
                }
                R.id.navigation_cart -> {
                    navController.navigate(R.id.action_global_cart)
                    true
                }
                else -> false
            }
        }
    }
}
