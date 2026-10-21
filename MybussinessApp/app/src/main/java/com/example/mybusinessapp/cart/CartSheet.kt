package com.example.mybusinessapp.cart

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mybusinessapp.ui.FluidGlassCard
import com.example.mybusinessapp.ui.theme.GradientSunset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartBottomSheet(
    cartViewModel: CartViewModel,
    onDismiss: () -> Unit
) {
    val state by cartViewModel.state.collectAsState()
    val colors = MaterialTheme.colorScheme
    var promoInput by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = colors.onSurfaceVariant.copy(alpha = 0.4f)) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Rounded.ShoppingCart,
                        contentDescription = null,
                        tint = colors.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "Your Cart (${state.totalItemCount})",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.onSurface
                    )
                }

                if (state.items.isNotEmpty()) {
                    TextButton(
                        onClick = { cartViewModel.processIntent(CartIntent.ClearCart) },
                        colors = ButtonDefaults.textButtonColors(contentColor = colors.error)
                    ) {
                        Text("Clear All", fontSize = 12.sp)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            if (state.items.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Rounded.RemoveShoppingCart,
                            contentDescription = null,
                            tint = colors.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Your cart is empty",
                            color = colors.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .heightIn(max = 280.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(state.items, key = { it.product.id }) { item ->
                        FluidGlassCard(shape = RoundedCornerShape(16.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.product.name,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.onSurface,
                                        fontSize = 15.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "$${String.format("%.2f", item.product.price)} each",
                                        fontSize = 12.sp,
                                        color = colors.onSurfaceVariant
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Decrement
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(colors.primary.copy(alpha = 0.15f))
                                            .clickable {
                                                cartViewModel.processIntent(
                                                    CartIntent.UpdateQuantity(item.product.id, item.quantity - 1)
                                                )
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Rounded.Remove,
                                            contentDescription = "Decrease",
                                            tint = colors.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }

                                    Text(
                                        text = "${item.quantity}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = colors.onSurface
                                    )

                                    // Increment
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(colors.primary.copy(alpha = 0.15f))
                                            .clickable {
                                                cartViewModel.processIntent(
                                                    CartIntent.AddItem(item.product)
                                                )
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Rounded.Add,
                                            contentDescription = "Increase",
                                            tint = colors.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }

                                    Spacer(Modifier.width(8.dp))

                                    // Total for item
                                    Text(
                                        text = "$${String.format("%.2f", item.totalPrice)}",
                                        fontWeight = FontWeight.Bold,
                                        color = colors.tertiary,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Promo code row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = promoInput,
                        onValueChange = { promoInput = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Try: SUNBURST10 or VIP20", fontSize = 12.sp) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (promoInput.isNotBlank()) {
                                cartViewModel.processIntent(CartIntent.ApplyPromo(promoInput))
                            }
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Apply")
                    }
                }

                if (state.discountPercent > 0.0) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Discount Applied: ${state.discountPercent.toInt()}% off (${state.promoCode})",
                        color = Color(0xFF00E676),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = colors.onSurface.copy(alpha = 0.1f))
                Spacer(Modifier.height(12.dp))

                // Subtotal & Total
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Total Amount", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = colors.onSurface)
                    Text(
                        text = "$${String.format("%.2f", state.finalTotal)}",
                        fontWeight = FontWeight.Black,
                        fontSize = 20.sp,
                        color = colors.tertiary
                    )
                }

                Spacer(Modifier.height(16.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.primary)
                ) {
                    Text("Proceed to Checkout", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
