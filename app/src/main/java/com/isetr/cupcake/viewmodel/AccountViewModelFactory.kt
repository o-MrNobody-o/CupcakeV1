package com.isetr.cupcake.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/**
 * Factory for creating AccountViewModel with Context dependency.
 * 
 * Usage:
 * ```
 * val viewModel = ViewModelProvider(
 *     this,
 *     AccountViewModelFactory(requireContext().applicationContext)
 * )[AccountViewModel::class.java]
 * ```
 * 
 * Note: Always use applicationContext to avoid memory leaks.
 */
class AccountViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(AccountViewModel::class.java) -> {
                AccountViewModel(context.applicationContext) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
