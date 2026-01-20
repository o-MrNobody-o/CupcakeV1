package com.isetr.cupcake.ui  // <- put your package here

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.isetr.cupcake.R

class FooterFragment : Fragment(R.layout.fragment_footer) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val bottomNav = view.findViewById<BottomNavigationView>(R.id.bottomNavigationView)

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_products -> {
                    findNavController().navigate(R.id.pastryProductsFragment)
                    true
                }
                R.id.navigation_account -> {
                    findNavController().navigate(R.id.accountFragment)
                    true
                }
                R.id.navigation_orders -> {
                    findNavController().navigate(R.id.pastryProductsFragment)
                    true
                }
                else -> false
            }
        }

    }
}
