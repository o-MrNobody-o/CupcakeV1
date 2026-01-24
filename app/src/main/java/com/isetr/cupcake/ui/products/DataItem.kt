package com.isetr.cupcake.ui.products

import com.isetr.cupcake.data.local.Pastry

sealed class DataItem {
    data class PastryItem(val pastry: Pastry) : DataItem() {
        override val id = pastry.id
    }
    data class HeaderItem(val categoryName: String) : DataItem() {
        override val id = categoryName
    }
    abstract val id: String
}
