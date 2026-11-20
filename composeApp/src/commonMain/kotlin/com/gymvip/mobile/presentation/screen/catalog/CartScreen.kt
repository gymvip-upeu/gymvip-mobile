package com.gymvip.mobile.presentation.screen.catalog

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gymvip.mobile.domain.model.*
import com.gymvip.mobile.presentation.component.EmptyView
import com.gymvip.mobile.presentation.theme.GoldPrimary

@Composable
fun CartScreen(
    cart: ShoppingCart,
    activeOrder: SupplementOrder? = null,
    onQuantityChange: (String, Int) -> Unit,
    onClearCart: () -> Unit,
    onCheckoutOrder: (paymentMethod: String, notes: String) -> Unit = { _, _ -> },
    onSubmitPaymentEvidence: (reference: String, notes: String) -> Unit = { _, _ -> },
    onCancelOrder: (reason: String) -> Unit = {},
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showCheckoutDialog by remember { mutableStateOf(false) }
    var showEvidenceDialog by remember { mutableStateOf(false) }
    var showTicketDialog by remember { mutableStateOf(false) }
    var showCancelDialog by remember { mutableStateOf(false) }

    var selectedPaymentMethod by remember { mutableStateOf("YAPE") }
    var orderNotes by remember { mutableStateOf("") }
    var paymentReferenceInput by remember { mutableStateOf("") }
    var paymentNotesInput by remember { mutableStateOf("") }
    var cancelReasonInput by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Mi Carrito de Suplementos",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Compra en línea con retiro seguro en el mostrador del gimnasio.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Si existe un pedido activo, mostrar banner de seguimiento
        if (activeOrder != null) {
            ActiveOrderBanner(
                order = activeOrder,
                onViewTicket = { showTicketDialog = true },
                onSubmitEvidence = { showEvidenceDialog = true },
                onCancelOrder = { showCancelDialog = true }
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        if (cart.items.isEmpty()) {
            EmptyView(
                icon = Icons.Outlined.ShoppingCart,
                title = "Tu Carrito está Vacío",
                message = "Agrega suplementos desde el catálogo para generar tu pedido.",
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

            // Panel de Resumen de Carrito
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
                            text = "Total a Pagar:",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = cart.totalPrice.amountInSoles,
                            style = MaterialTheme.typography.headlineSmall,
                            color = GoldPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = "Al confirmar, el stock se reserva por 30 minutos mientras registras tu comprobante.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )

                    Button(
                        onClick = { showCheckoutDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary)
                    ) {
                        Text(
                            text = "Confirmar Pedido y Reservar Stock",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    TextButton(
                        onClick = onClearCart,
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Vaciar Carrito", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }

    // Modal de Checkout / Confirmación de Pedido
    if (showCheckoutDialog) {
        AlertDialog(
            onDismissRequest = { showCheckoutDialog = false },
            title = { Text("Confirmar Compra de Suplementos") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Total: ${cart.totalPrice.amountInSoles} (${cart.totalQuantity} productos)",
                        fontWeight = FontWeight.SemiBold,
                        color = GoldPrimary
                    )
                    Text("Selecciona tu medio de pago:")

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = selectedPaymentMethod == "YAPE",
                            onClick = { selectedPaymentMethod = "YAPE" },
                            label = { Text("Yape") }
                        )
                        FilterChip(
                            selected = selectedPaymentMethod == "PLIN",
                            onClick = { selectedPaymentMethod = "PLIN" },
                            label = { Text("Plin") }
                        )
                        FilterChip(
                            selected = selectedPaymentMethod == "TRANSFERENCIA",
                            onClick = { selectedPaymentMethod = "TRANSFERENCIA" },
                            label = { Text("Transferencia") }
                        )
                    }

                    OutlinedTextField(
                        value = orderNotes,
                        onValueChange = { orderNotes = it },
                        label = { Text("Notas o instrucciones (opcional)") },
                        placeholder = { Text("Ej: Retiro por la tarde") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        text = "⚠ Se generará una reserva temporal con bloqueo de stock de 30 minutos.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showCheckoutDialog = false
                        onCheckoutOrder(selectedPaymentMethod, orderNotes)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary)
                ) {
                    Text("Confirmar Pedido", color = Color.Black)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCheckoutDialog = false }) {
                    Text("Volver")
                }
            }
        )
    }

    // Modal de Subida de Evidencia de Pago
    if (showEvidenceDialog && activeOrder != null) {
        AlertDialog(
            onDismissRequest = { showEvidenceDialog = false },
            title = { Text("Registrar Comprobante de Pago") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Pedido: ${activeOrder.orderNumber}")
                    Text(
                        text = "Total a Pagar: ${activeOrder.total.amountInSoles}",
                        fontWeight = FontWeight.Bold,
                        color = GoldPrimary
                    )

                    OutlinedTextField(
                        value = paymentReferenceInput,
                        onValueChange = { paymentReferenceInput = it },
                        label = { Text("N° de Operación o Referencia *") },
                        placeholder = { Text("Ej: 78945612") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = paymentNotesInput,
                        onValueChange = { paymentNotesInput = it },
                        label = { Text("Observaciones (opcional)") },
                        placeholder = { Text("Ej: Yape a nombre de Carlos") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        text = "La referencia será verificada en caja antes de autorizar la entrega de suplementos.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (paymentReferenceInput.isNotBlank()) {
                            showEvidenceDialog = false
                            onSubmitPaymentEvidence(paymentReferenceInput.trim(), paymentNotesInput.trim())
                        }
                    },
                    enabled = paymentReferenceInput.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary)
                ) {
                    Text("Enviar Comprobante", color = Color.Black)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEvidenceDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Modal de Visualización de Ticket QR de Retiro
    if (showTicketDialog && activeOrder?.pickupToken != null) {
        AlertDialog(
            onDismissRequest = { showTicketDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.QrCode, contentDescription = null, tint = GoldPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Ticket de Retiro en Mostrador")
                }
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = activeOrder.orderNumber,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Representación visual del código QR / Token
                    Box(
                        modifier = Modifier
                            .size(180.dp)
                            .background(Color.White, RoundedCornerShape(12.dp))
                            .border(2.dp, GoldPrimary, RoundedCornerShape(12.dp))
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.QrCode,
                                contentDescription = "Código QR de Retiro",
                                modifier = Modifier.size(110.dp),
                                tint = Color.Black
                            )
                            Text(
                                text = activeOrder.pickupToken.take(16) + "...",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                color = Color.DarkGray
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Presente este código en el mostrador de recepción para retirar sus productos.",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "⚠ Advertencia: Este ticket es una credencial de entrega física y no constituye comprobante de pago electrónico ante SUNAT.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showTicketDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary)
                ) {
                    Text("Entendido", color = Color.Black)
                }
            }
        )
    }

    // Modal de Cancelación de Pedido
    if (showCancelDialog && activeOrder != null) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            title = { Text("Cancelar Pedido") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("¿Desea cancelar el pedido ${activeOrder.orderNumber}? El stock reservado será liberado inmediatamente.")
                    OutlinedTextField(
                        value = cancelReasonInput,
                        onValueChange = { cancelReasonInput = it },
                        label = { Text("Motivo de cancelación") },
                        placeholder = { Text("Ej: Cambio de opinión") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showCancelDialog = false
                        onCancelOrder(cancelReasonInput.ifBlank { "Cancelado por el usuario" })
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Confirmar Cancelación", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelDialog = false }) {
                    Text("Volver")
                }
            }
        )
    }
}

@Composable
private fun ActiveOrderBanner(
    order: SupplementOrder,
    onViewTicket: () -> Unit,
    onSubmitEvidence: () -> Unit,
    onCancelOrder: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Pedido ${order.orderNumber}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = order.orderStatus.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = GoldPrimary,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Total: ${order.total.amountInSoles} · Pago: ${order.paymentStatus.label} · Retiro: ${order.fulfillmentStatus.label}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (order.hasPickupToken) {
                    Button(
                        onClick = onViewTicket,
                        colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary)
                    ) {
                        Icon(Icons.Default.QrCode, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Ver Ticket QR", color = Color.Black)
                    }
                } else if (order.paymentStatus == PaymentStatus.PENDIENTE) {
                    Button(
                        onClick = onSubmitEvidence,
                        colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary)
                    ) {
                        Text("Subir Comprobante", color = Color.Black)
                    }
                    OutlinedButton(onClick = onCancelOrder) {
                        Text("Cancelar")
                    }
                }
            }
        }
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
                    color = GoldPrimary,
                    fontWeight = FontWeight.SemiBold
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
