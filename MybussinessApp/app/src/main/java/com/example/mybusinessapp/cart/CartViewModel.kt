package com.example.mybusinessapp.cart

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// ==================================================================
// 🛒 SHARED CART VIEW MODEL (UNIDIRECTIONAL MVI ARCHITECTURE)
// ==================================================================
class CartViewModel : ViewModel() {

    // Default initial mock catalog for the Products screen
    val sampleProducts = listOf(
        Product("prod_1", "Solar Pro", "Hardware", 199.99, 4.9, "Next-gen solar telemetry module"),
        Product("prod_2", "Nebula Hub", "IoT", 249.00, 4.8, "Multi-protocol intelligent hub"),
        Product("prod_3", "Quantum Gear", "Accessories", 89.50, 4.7, "Ultra-durable ergonomic mount"),
        Product("prod_4", "Pulse Watch", "Wearables", 159.00, 4.9, "Biometric performance tracker"),
        Product("prod_5", "Aura Pods", "Audio", 129.99, 4.6, "Spatial audio enterprise earphones"),
        Product("prod_6", "Vortex Key", "Security", 79.00, 5.0, "Hardware FIDO2 cryptographic key")
    )

    // Initial state with 3 items preloaded to match the Profile screen initial badge requirement
    private val _state = MutableStateFlow(
        CartState(
            items = listOf(
                CartItem(sampleProducts[0], quantity = 1),
                CartItem(sampleProducts[3], quantity = 1),
                CartItem(sampleProducts[5], quantity = 1)
            )
        )
    )
    val state: StateFlow<CartState> = _state.asStateFlow()

    // Side-effects / one-off events (e.g. snackbars)
    private val _effect = MutableSharedFlow<String>()
    val effect: SharedFlow<String> = _effect.asSharedFlow()

    fun processIntent(intent: CartIntent) {
        _state.update { currentState ->
            reduce(currentState, intent)
        }
    }

    /**
     * Pure Reducer Function
     * Single Source of Truth for all cart transformations.
     */
    private fun reduce(currentState: CartState, intent: CartIntent): CartState {
        return when (intent) {
            is CartIntent.AddItem -> {
                val existing = currentState.items.find { it.product.id == intent.product.id }
                val updatedItems = if (existing != null) {
                    currentState.items.map {
                        if (it.product.id == intent.product.id) it.copy(quantity = it.quantity + 1)
                        else it
                    }
                } else {
                    currentState.items + CartItem(product = intent.product, quantity = 1)
                }
                viewModelScope.launch {
                    _effect.emit("Added ${intent.product.name} to cart")
                }
                currentState.copy(
                    items = updatedItems,
                    lastAddedProductName = intent.product.name
                )
            }

            is CartIntent.RemoveItem -> {
                val updatedItems = currentState.items.filterNot { it.product.id == intent.productId }
                viewModelScope.launch {
                    _effect.emit("Item removed from cart")
                }
                currentState.copy(items = updatedItems)
            }

            is CartIntent.UpdateQuantity -> {
                val updatedItems = if (intent.quantity <= 0) {
                    currentState.items.filterNot { it.product.id == intent.productId }
                } else {
                    currentState.items.map {
                        if (it.product.id == intent.productId) it.copy(quantity = intent.quantity)
                        else it
                    }
                }
                currentState.copy(items = updatedItems)
            }

            is CartIntent.ApplyPromo -> {
                val normalizedPromo = intent.promoCode.trim().uppercase()
                val discount = when (normalizedPromo) {
                    "SUNBURST10" -> 10.0
                    "VIP20" -> 20.0
                    "KORE50" -> 50.0
                    else -> 0.0
                }
                viewModelScope.launch {
                    if (discount > 0.0) _effect.emit("Promo $normalizedPromo applied: ${discount.toInt()}% off!")
                    else _effect.emit("Invalid promo code")
                }
                currentState.copy(
                    promoCode = if (discount > 0.0) normalizedPromo else null,
                    discountPercent = discount
                )
            }

            is CartIntent.ClearCart -> {
                viewModelScope.launch {
                    _effect.emit("Cart cleared")
                }
                currentState.copy(items = emptyList(), promoCode = null, discountPercent = 0.0)
            }
        }
    }
}
