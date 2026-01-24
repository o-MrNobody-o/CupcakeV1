package com.isetr.cupcake.ui.cart

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.isetr.cupcake.R
import com.isetr.cupcake.ui.order.OrderStatusActivity
import com.isetr.cupcake.viewmodel.CartViewModel
import com.isetr.cupcake.viewmodel.OrderState

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
    private lateinit var tvSummaryDelivery: TextView
    private lateinit var tvSummaryTotal: TextView

    private val cartViewModel: CartViewModel by viewModels()
    private lateinit var cartAdapter: CartAdapter

    private var currentUserId: Int = 0 
    private var selectedPaymentMethod = "Cash"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_cart, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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
        tvSummaryDelivery = view.findViewById(R.id.tvSummaryDelivery)
        tvSummaryTotal = view.findViewById(R.id.tvSummaryTotal)

        currentUserId = 1

        setupRecyclerView()
        observeCart()
        observeOrderState()
        setupListeners()
    }

    private fun setupRecyclerView() {
        cartAdapter = CartAdapter(
            onQuantityChanged = { item, newQty ->
                val updatedItem = item.copy(quantity = newQty)
                cartViewModel.updateCartItem(updatedItem)
            },
            onRemoveItem = { item ->
                cartViewModel.removeCartItem(item)
            }
        )

        recyclerView.adapter = cartAdapter
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
    }

    private fun setupListeners() {
        cardCash.setOnClickListener {
            selectedPaymentMethod = "Cash"
            updatePaymentUI(cardCash, cardPay, false)
        }

        cardPay.setOnClickListener {
            selectedPaymentMethod = "Card"
            updatePaymentUI(cardPay, cardCash, true)
        }

        btnCheckout.setOnClickListener {
            if (paymentSection.visibility == View.GONE) {
                paymentSection.visibility = View.VISIBLE
                btnCheckout.text = "Confirmer le paiement"
            } else {
                processPayment()
            }
        }
    }

    private fun updatePaymentUI(selected: MaterialCardView, unselected: MaterialCardView, showCardInput: Boolean) {
        selected.strokeWidth = 5
        selected.strokeColor = Color.parseColor("#FE828C")
        
        unselected.strokeWidth = 2
        unselected.strokeColor = Color.parseColor("#DDDDDD")

        tilCardNumber.visibility = if (showCardInput) View.VISIBLE else View.GONE
    }

    private fun processPayment() {
        val items = cartViewModel.cartItems.value
        if (items.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "Votre panier est vide", Toast.LENGTH_SHORT).show()
            return
        }

        val subtotal = items.sumOf { it.price * it.quantity }
        val deliveryFee = if (subtotal > 20.0) 0.0 else 5.0
        val finalTotal = subtotal + deliveryFee
        
        val cardNumber = etCardNumber.text.toString().trim()
        val address = etShippingAddress.text.toString().trim()

        if (address.isEmpty()) {
            tilShippingAddress.error = "Veuillez saisir votre adresse de livraison"
            return
        }
        tilShippingAddress.error = null

        if (selectedPaymentMethod == "Card") {
            if (cardNumber.length != 16 || !cardNumber.all { it.isDigit() }) {
                tilCardNumber.error = "Le numéro de carte doit contenir 16 chiffres"
                return
            }
        }
        tilCardNumber.error = null

        cartViewModel.checkout(currentUserId, finalTotal, selectedPaymentMethod, if (selectedPaymentMethod == "Card") cardNumber else null, address)
    }

    private fun observeCart() {
        cartViewModel.cartItems.observe(viewLifecycleOwner) { cartItems ->
            cartAdapter.submitList(cartItems)
            val subtotal = cartItems.sumOf { it.price * it.quantity }
            val deliveryFee = if (subtotal > 20.0) 0.0 else 5.0
            val total = subtotal + deliveryFee

            totalPriceTextView.text = "Total: $subtotal TND"
            
            // Mise à jour du résumé
            tvSummarySubtotal.text = "$subtotal TND"
            tvSummaryDelivery.text = if (deliveryFee == 0.0) "GRATUIT" else "5 TND"
            tvSummaryTotal.text = "$total TND"
            
            if (cartItems.isEmpty()) {
                paymentSection.visibility = View.GONE
                btnCheckout.text = "Passer commande"
            }
        }
        cartViewModel.loadCart(currentUserId)
    }

    private fun observeOrderState() {
        cartViewModel.orderState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is OrderState.Loading -> {
                    btnCheckout.isEnabled = false
                }
                is OrderState.Success -> {
                    btnCheckout.isEnabled = true
                    showOrderConfirmationDialog()
                    cartViewModel.resetOrderState()
                }
                is OrderState.Error -> {
                    btnCheckout.isEnabled = true
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                }
                is OrderState.Idle -> {
                    btnCheckout.isEnabled = true
                }
            }
        }
    }

    private fun showOrderConfirmationDialog() {
        val title = SpannableString("Commande Validée !")
        title.setSpan(
            ForegroundColorSpan(Color.parseColor("#E91E63")),
            0, title.length, 0
        )

        val alertDialog = AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setMessage("Votre délicieuse commande a été enregistrée. Elle sera préparée avec amour !")
            .setPositiveButton("Suivre ma commande") { _, _ ->
                val intent = Intent(requireContext(), OrderStatusActivity::class.java)
                startActivity(intent)
            }
            .setNegativeButton("Retour") { _, _ ->
                findNavController().navigate(R.id.welcomeFragment)
            }
            .setCancelable(false)
            .create()

        alertDialog.show()

        alertDialog.window?.setBackgroundDrawableResource(android.R.color.white)
        
        // Texte du message en noir
        val messageView = alertDialog.findViewById<TextView>(android.R.id.message)
        messageView?.setTextColor(Color.BLACK)
        
        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.parseColor("#E91E63"))
        alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.GRAY)
    }
}
