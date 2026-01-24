package com.isetr.cupcake.ui

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.findNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.isetr.cupcake.R

class FooterFragment : Fragment(R.layout.fragment_footer) {

    private lateinit var navController: NavController
    private lateinit var bottomNav: BottomNavigationView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bottomNav = view.findViewById(R.id.bottomNavigationView)
        navController = requireActivity().findNavController(R.id.nav_host_fragment)

        // Highlight correct item
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.pastryProductsFragment -> bottomNav.menu.findItem(R.id.navigation_products).isChecked = true
                R.id.accountFragment -> bottomNav.menu.findItem(R.id.navigation_account).isChecked = true
                R.id.cartFragment -> bottomNav.menu.findItem(R.id.navigation_cart).isChecked = true
                R.id.welcomeFragment -> bottomNav.menu.findItem(R.id.navigation_welcome).isChecked = true
            }
        }

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_products -> {
                    if (navController.currentDestination?.id != R.id.pastryProductsFragment)
                        navController.navigate(R.id.action_global_pastryProducts)
                }
                R.id.navigation_account -> {
                    if (navController.currentDestination?.id != R.id.accountFragment)
                        navController.navigate(R.id.action_global_account)
                }
                R.id.navigation_cart -> {
                    if (navController.currentDestination?.id != R.id.cartFragment)
                        navController.navigate(R.id.action_global_cart)
                }
                R.id.navigation_welcome -> {
                    if (navController.currentDestination?.id != R.id.welcomeFragment)
                        navController.navigate(R.id.action_global_welcome)
                }
            }
            true
        }
    }
}
