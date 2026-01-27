package com.isetr.cupcake.ui.cart

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.isetr.cupcake.R
import com.isetr.cupcake.ui.order.OrderStatusActivity
import com.isetr.cupcake.viewmodel.AccountViewModel
import com.isetr.cupcake.viewmodel.CartViewModel
import com.isetr.cupcake.viewmodel.OrderState
import java.util.Locale

class CartFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var totalPriceTextView: TextView
    private lateinit var btnCheckout: Button
    
    private lateinit var paymentSection: View
    private lateinit var cardCash: MaterialCardView
    private lateinit var cardPay: MaterialCardView
    private lateinit var tilCardNumber: TextInputLayout
    private lateinit var etCardNumber: TextInputEditText
    private lateinit var tilShippingAddress: TextInputLayout
    private lateinit var etShippingAddress: TextInputEditText

    private lateinit var tvSummarySubtotal: TextView
    private lateinit var tvSummaryAfterDiscount: TextView
    private lateinit var tvSummaryDelivery: TextView
    private lateinit var tvSummaryTotal: TextView

    private val cartViewModel: CartViewModel by viewModels()
    private lateinit var accountViewModel: AccountViewModel
    private lateinit var cartAdapter: CartAdapter

    private var currentUserId: Int = -1 

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_cart, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        accountViewModel = ViewModelProvider(this, AccountViewModel.Factory(requireActivity().application))
            .get(AccountViewModel::class.java)

        initViews(view)
        setupRecyclerView()

        btnCheckout.isEnabled = false

        accountViewModel.currentUser.observe(viewLifecycleOwner) { user ->
            if (user != null) {
                currentUserId = user.id
                btnCheckout.isEnabled = true
                cartViewModel.loadCart(currentUserId)
            }
        }
        accountViewModel.loadCurrentUser()

        setupListeners()
        observeCart()
        observeOrderState()
    }

    private fun initViews(view: View) {
        recyclerView = view.findViewById(R.id.rvCart)
        totalPriceTextView = view.findViewById(R.id.tvTotalPrice)
        btnCheckout = view.findViewById(R.id.btnCheckout)
        paymentSection = view.findViewById(R.id.paymentSection)
        cardCash = view.findViewById(R.id.cardCash)
        cardPay = view.findViewById(R.id.cardPay)
        tilCardNumber = view.findViewById(R.id.tilCardNumber)
        etCardNumber = view.findViewById(R.id.etCardNumber)
        tilShippingAddress = view.findViewById(R.id.tilShippingAddress)
        etShippingAddress = view.findViewById(R.id.etShippingAddress)
        
        tvSummarySubtotal = view.findViewById(R.id.tvSummarySubtotal)
        tvSummaryAfterDiscount = view.findViewById(R.id.tvSummaryAfterDiscount)
        tvSummaryDelivery = view.findViewById(R.id.tvSummaryDelivery)
        tvSummaryTotal = view.findViewById(R.id.tvSummaryTotal)
    }

    private fun setupRecyclerView() {
        cartAdapter = CartAdapter(
            onQuantityChanged = { item, newQty -> cartViewModel.updateCartItem(item.copy(quantity = newQty)) },
            onRemoveItem = { item -> cartViewModel.removeCartItem(item) }
        )
        recyclerView.adapter = cartAdapter
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
    }

    private fun setupListeners() {
        cardCash.setOnClickListener { updatePaymentUI(cardCash, cardPay, "Cash") }
        cardPay.setOnClickListener { updatePaymentUI(cardPay, cardCash, "Card") }

        btnCheckout.setOnClickListener {
            val items = cartViewModel.cartItems.value
            if (items.isNullOrEmpty()) {
                Toast.makeText(requireContext(), "Votre panier est vide !", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (paymentSection.visibility == View.GONE) {
                paymentSection.visibility = View.VISIBLE
                btnCheckout.text = "Confirmer le paiement"
            } else {
                processPayment()
            }
        }
    }

    private fun updatePaymentUI(selected: MaterialCardView, unselected: MaterialCardView, method: String) {
        selected.strokeWidth = 5
        selected.strokeColor = Color.parseColor("#FE828C")
        unselected.strokeWidth = 2
        unselected.strokeColor = Color.parseColor("#DDDDDD")
        tilCardNumber.visibility = if (method == "Card") View.VISIBLE else View.GONE
        tilCardNumber.error = null
    }

    private fun processPayment() {
        if (currentUserId <= 0) return

        val items = cartViewModel.cartItems.value
        if (items.isNullOrEmpty()) return

        val address = etShippingAddress.text.toString().trim()
        if (address.isEmpty()) {
            tilShippingAddress.error = "Adresse obligatoire"
            return
        }

        val isCard = tilCardNumber.visibility == View.VISIBLE
        val cardNum = etCardNumber.text.toString().trim()
        if (isCard && cardNum.isEmpty()) {
            tilCardNumber.error = "Numéro requis"
            return
        }

        // Calcul du total final après remise pour l'envoi au serveur
        val finalSubtotal = items.sumOf { item ->
            val price = if (item.inPromotion) item.price * (1 - item.discountRate / 100.0) else item.price
            price * item.quantity
        }
        val totalWithDelivery = finalSubtotal + (if (finalSubtotal > 20.0) 0.0 else 5.0)

        cartViewModel.checkout(currentUserId, totalWithDelivery, if (isCard) "Carte" else "Espèces", if (isCard) cardNum else null, address)
    }

    private fun observeCart() {
        cartViewModel.cartItems.observe(viewLifecycleOwner) { items ->
            cartAdapter.submitList(items)
            
            // 1. Total SANS remise (pour le haut de l'écran)
            val rawSubtotal = items.sumOf { it.price * it.quantity }
            totalPriceTextView.text = String.format(Locale.US, "Total: %.2f TND", rawSubtotal)
            tvSummarySubtotal.text = String.format(Locale.US, "%.2f TND", rawSubtotal)

            // 2. Total AVEC remise
            val discountedSubtotal = items.sumOf { item ->
                val price = if (item.inPromotion) item.price * (1 - item.discountRate / 100.0) else item.price
                price * item.quantity
            }
            tvSummaryAfterDiscount.text = String.format(Locale.US, "%.2f TND", discountedSubtotal)

            // 3. Frais de livraison (Gratuit si total après remise > 20)
            val delivery = if (discountedSubtotal > 20.0 || items.isEmpty()) 0.0 else 5.0
            tvSummaryDelivery.text = if (delivery == 0.0) "GRATUIT" else "5.00 TND"

            // 4. Total Final à payer
            tvSummaryTotal.text = String.format(Locale.US, "%.2f TND", discountedSubtotal + delivery)
        }
    }

    private fun observeOrderState() {
        cartViewModel.orderState.observe(viewLifecycleOwner) { state ->
            if (state is OrderState.Success) {
                resetCheckoutUI()
                showOrderConfirmationDialog()
                cartViewModel.resetOrderState()
            }
        }
    }

    private fun resetCheckoutUI() {
        paymentSection.visibility = View.GONE
        btnCheckout.text = "Passer la commande"
        etCardNumber.setText("")
        etShippingAddress.setText("")
        tilCardNumber.visibility = View.GONE
    }

    private fun showOrderConfirmationDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_order_success, null)
        val dialog = AlertDialog.Builder(requireContext()).setView(dialogView).create()

        dialogView.findViewById<Button>(R.id.btnDialogTrack).setOnClickListener {
            dialog.dismiss()
            startActivity(Intent(requireContext(), OrderStatusActivity::class.java))
        }

        dialogView.findViewById<Button>(R.id.btnDialogHome).setOnClickListener {
            dialog.dismiss()
            findNavController().navigate(R.id.welcomeFragment)
        }

        dialog.show()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
    }
}
