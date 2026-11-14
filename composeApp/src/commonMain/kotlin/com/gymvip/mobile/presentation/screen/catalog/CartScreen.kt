package com.gymvip.mobile.presentation.screen.catalog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gymvip.mobile.domain.model.CartItem
import com.gymvip.mobile.domain.model.ShoppingCart
import com.gymvip.mobile.presentation.component.EmptyView
import com.gymvip.mobile.presentation.theme.GoldPrimary

@Composable
fun CartScreen(
    cart: ShoppingCart,
    onQuantityChange: (String, Int) -> Unit,
    onClearCart: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showReceptionSummary by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        Text(
            text = "Mi Carrito Referencial",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "Seleccione los suplementos a consultar en recepción. Los pagos se realizan presencialmente.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (cart.items.isEmpty()) {
            EmptyView(
                icon = "🛒",
                title = "Carrito Vacío",
                message = "Tu carrito está vacío. Explora el catálogo de suplementos para armar tu pedido.",
                modifier = Modifier.weight(1f)
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(cart.items, key = { it.product.id }) { item ->
                    CartItemRow(
                        item = item,
                        onQuantityChange = onQuantityChange
                    )
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Total referencial:",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = cart.totalPrice.amountInSoles,
                            style = MaterialTheme.typography.titleLarge,
                            color = GoldPrimary
                        )
                    }
                    Text(
                        text = "El carrito no reserva stock. Confirma disponibilidad y precio final en el mostrador del gimnasio.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    Button(
                        onClick = { showReceptionSummary = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary)
                    ) {
                        Text("Mostrar lista en recepción")
                    }
                    TextButton(
                        onClick = onClearCart,
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Vaciar carrito")
                    }
                }
            }
        }
    }

    if (showReceptionSummary) {
        AlertDialog(
            onDismissRequest = { showReceptionSummary = false },
            title = { Text("Lista de Consulta en Recepción") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    cart.items.forEach { line ->
                        Text("${line.quantity} × ${line.product.name} — ${line.subtotal.amountInSoles}")
                    }
                    Divider(modifier = Modifier.padding(vertical = 4.dp))
                    Text(
                        "Total referencial: ${cart.totalPrice.amountInSoles}",
                        style = MaterialTheme.typography.titleMedium,
                        color = GoldPrimary
                    )
                    Text(
                        "Pago en mostrador (Efectivo, Tarjeta, Yape o Plin).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showReceptionSummary = false }) {
                    Text("Cerrar")
                }
            }
        )
    }
}

@Composable
private fun CartItemRow(
    item: CartItem,
    onQuantityChange: (String, Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.product.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${item.unitPrice.amountInSoles} c/u",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = item.subtotal.amountInSoles,
                    style = MaterialTheme.typography.titleMedium,
                    color = GoldPrimary
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { onQuantityChange(item.product.id, item.quantity - 1) }
                ) {
                    Icon(
                        imageVector = if (item.quantity == 1) Icons.Default.Delete else Icons.Default.Remove,
                        contentDescription = "Disminuir o quitar"
                    )
                }
                Text(
                    text = "${item.quantity}",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
                IconButton(
                    onClick = { onQuantityChange(item.product.id, item.quantity + 1) },
                    enabled = item.quantity < 20
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Aumentar unidad")
                }
            }
        }
    }
}
