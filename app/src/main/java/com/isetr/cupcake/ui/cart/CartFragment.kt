package com.isetr.cupcake.ui.cart

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
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
    private lateinit var accountViewModel: AccountViewModel
    private lateinit var cartAdapter: CartAdapter

    private var currentUserId: Int = -1 // Valeur par défaut invalide
    private val TAG = "SessionCoherence"

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_cart, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialisation correcte du ViewModel
        accountViewModel = ViewModelProvider(this, AccountViewModel.Factory(requireActivity().application))
            .get(AccountViewModel::class.java)

        initViews(view)
        setupRecyclerView()

        // BLOQUER le checkout tant que l'utilisateur n'est pas authentifié
        btnCheckout.isEnabled = false

        // Observation de la session active
        accountViewModel.currentUser.observe(viewLifecycleOwner) { user ->
            if (user != null) {
                currentUserId = user.id
                Log.d(TAG, "Utilisateur détecté : ID = $currentUserId")
                btnCheckout.isEnabled = true
                cartViewModel.loadCart(currentUserId)
            } else {
                Log.e(TAG, "Aucun utilisateur connecté")
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
    }

    private fun processPayment() {
        if (currentUserId <= 0) {
            Toast.makeText(requireContext(), "Veuillez vous reconnecter", Toast.LENGTH_LONG).show()
            return
        }

        val items = cartViewModel.cartItems.value
        if (items.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "Votre panier est vide", Toast.LENGTH_SHORT).show()
            return
        }

        val address = etShippingAddress.text.toString().trim()
        if (address.isEmpty()) {
            tilShippingAddress.error = "Adresse obligatoire"
            return
        }

        val subtotal = items.sumOf { it.price * it.quantity }
        val total = subtotal + (if (subtotal > 20.0) 0.0 else 5.0)

        // Enregistrement avec l'ID RÉEL de la session
        cartViewModel.checkout(
            userId = currentUserId,
            total = total,
            paymentMethod = if (tilCardNumber.visibility == View.VISIBLE) "Carte" else "Espèces",
            cardNumber = etCardNumber.text.toString(),
            shippingAddress = address
        )
    }

    private fun observeCart() {
        cartViewModel.cartItems.observe(viewLifecycleOwner) { items ->
            cartAdapter.submitList(items)
            val subtotal = items.sumOf { it.price * it.quantity }
            val delivery = if (subtotal > 20.0 || items.isEmpty()) 0.0 else 5.0
            tvSummarySubtotal.text = "$subtotal TND"
            tvSummaryDelivery.text = if (delivery == 0.0) "GRATUIT" else "5 TND"
            tvSummaryTotal.text = "${subtotal + delivery} TND"
            totalPriceTextView.text = "Total: $subtotal TND"
        }
    }

    private fun observeOrderState() {
        cartViewModel.orderState.observe(viewLifecycleOwner) { state ->
            if (state is OrderState.Success) {
                showOrderConfirmationDialog()
                cartViewModel.resetOrderState()
            } else if (state is OrderState.Error) {
                Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showOrderConfirmationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Succès !")
            .setMessage("Votre commande a bien été enregistrée.")
            .setPositiveButton("Suivre") { _, _ -> startActivity(Intent(requireContext(), OrderStatusActivity::class.java)) }
            .setNegativeButton("Retour") { _, _ -> findNavController().navigate(R.id.welcomeFragment) }
            .setCancelable(false)
            .show()
    }
}
