package com.example.mybusinessapp.cart

import androidx.compose.ui.graphics.vector.ImageVector

// ==================================================================
// 🛒 PRODUCT MODEL
// ==================================================================
data class Product(
    val id: String,
    val name: String,
    val category: String,
    val price: Double,
    val rating: Double = 4.8,
    val description: String = "Premium Edition",
    val inStock: Boolean = true
)

// ==================================================================
// 📦 CART ITEM & STATE
// ==================================================================
data class CartItem(
    val product: Product,
    val quantity: Int = 1
) {
    val totalPrice: Double get() = product.price * quantity
}

data class CartState(
    val items: List<CartItem> = emptyList(),
    val promoCode: String? = null,
    val discountPercent: Double = 0.0,
    val lastAddedProductName: String? = null
) {
    val totalItemCount: Int get() = items.sumOf { it.quantity }
    val subtotal: Double get() = items.sumOf { it.totalPrice }
    val discountAmount: Double get() = subtotal * (discountPercent / 100.0)
    val finalTotal: Double get() = (subtotal - discountAmount).coerceAtLeast(0.0)
}

// ==================================================================
// 🔄 MVI CART INTENTS
// ==================================================================
sealed interface CartIntent {
    data class AddItem(val product: Product) : CartIntent
    data class RemoveItem(val productId: String) : CartIntent
    data class UpdateQuantity(val productId: String, val quantity: Int) : CartIntent
    data class ApplyPromo(val promoCode: String) : CartIntent
    data object ClearCart : CartIntent
}
