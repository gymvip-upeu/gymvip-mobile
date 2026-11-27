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
import androidx.compose.ui.platform.LocalUriHandler
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
    orders: List<SupplementOrder> = emptyList(),
    digitalPaymentLink: DigitalPaymentLink? = null,
    culqiEnabled: Boolean = false,
    errorMessage: String? = null,
    onQuantityChange: (String, Int) -> Unit,
    onClearCart: () -> Unit,
    onCheckoutOrder: (paymentMethod: String, notes: String) -> Unit = { _, _ -> },
    onRefreshPaymentLink: () -> Unit = {},
    onSubmitPaymentEvidence: (reference: String, notes: String) -> Unit = { _, _ -> },
    onCancelOrder: (reason: String) -> Unit = {},
    onRefresh: () -> Unit = {},
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showCheckoutDialog by remember { mutableStateOf(false) }
    var showEvidenceDialog by remember { mutableStateOf(false) }
    var showTicketDialog by remember { mutableStateOf(false) }
    var showCancelDialog by remember { mutableStateOf(false) }

    var selectedPaymentMethod by remember { mutableStateOf("EFECTIVO_MOSTRADOR") }
    val uriHandler = LocalUriHandler.current
    val eligibleItems = cart.items.filter { item -> item.availableStock?.let { it >= item.quantity } == true }
    val eligibleSubtotal = ShoppingCart(eligibleItems).totalPrice
    val blockedItems = cart.items.size - eligibleItems.size
    val digitalAvailable = culqiEnabled && cart.totalPrice.cents in 600..999900
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
        TextButton(onClick = onBack) { Text("← Volver al catálogo") }
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
                    text = "Reserva suplementos y elige efectivo en recepción o pago seguro por enlace.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        TextButton(onClick = onRefresh) { Text("Actualizar carrito y pedidos") }
        errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        Spacer(modifier = Modifier.height(12.dp))

        // Si existe un pedido activo, mostrar banner de seguimiento
        if (activeOrder != null) {
            ActiveOrderBanner(
                order = activeOrder,
                digitalPaymentLink = digitalPaymentLink,
                onViewTicket = { showTicketDialog = true },
                onSubmitEvidence = { showEvidenceDialog = true },
                onCancelOrder = { showCancelDialog = true },
                onOpenPaymentLink = { url -> uriHandler.openUri(url) },
                onRefreshPaymentLink = onRefreshPaymentLink
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        if (cart.items.isEmpty()) {
            if (orders.isEmpty()) {
                EmptyView(
                    icon = Icons.Outlined.ShoppingCart,
                    title = "Tu Carrito está Vacío",
                    message = "Agrega suplementos desde el catálogo para generar tu pedido.",
                    modifier = Modifier.weight(1f)
                )
            } else {
                Text("Historial de pedidos", style = MaterialTheme.typography.titleMedium)
                LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(orders, key = { it.id }) { order ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(12.dp)) {
                                Text(order.orderNumber, fontWeight = FontWeight.Bold)
                                Text("${order.orderStatus.label} · ${order.total.amountInSoles}")
                                Text("Pago: ${order.paymentStatus.label} · Retiro: ${order.fulfillmentStatus.label}")
                            }
                        }
                    }
                }
            }
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
                            text = "Subtotal disponible",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = eligibleSubtotal.amountInSoles,
                            style = MaterialTheme.typography.headlineSmall,
                            color = GoldPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (blockedItems > 0) {
                        Text(
                            "Ajusta o retira los productos indicados antes de confirmar.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    Text(
                        text = "Al confirmar, el stock se reserva por 30 minutos. El enlace digital vence antes de la reserva.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )

                    Button(
                        enabled = blockedItems == 0,
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
                    Text("Elige cómo deseas pagar. El pedido se confirma únicamente después del cobro verificado.")
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = selectedPaymentMethod == "EFECTIVO_MOSTRADOR", onClick = { selectedPaymentMethod = "EFECTIVO_MOSTRADOR" })
                        Text("Efectivo en recepción")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = selectedPaymentMethod == "TARJETA_PASARELA", enabled = digitalAvailable,
                            onClick = { selectedPaymentMethod = "TARJETA_PASARELA" })
                        Text(if (digitalAvailable) "Pago en línea con CulqiLink" else if (!culqiEnabled)
                            "Pago en línea · pendiente de configuración" else "Pago en línea · importe fuera de rango")
                    }
                    if (selectedPaymentMethod == "TARJETA_PASARELA") {
                        Text("Abrirás un enlace seguro de Culqi. Volver a la app no confirma el pago; espera el estado verificado.",
                            style = MaterialTheme.typography.bodySmall)
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
                    enabled = selectedPaymentMethod != "TARJETA_PASARELA" || digitalAvailable,
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

    // Modal de visualización del código de retiro
    if (showTicketDialog && activeOrder?.pickupToken != null) {
        AlertDialog(
            onDismissRequest = { showTicketDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.QrCode, contentDescription = null, tint = GoldPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Código de Retiro en Mostrador")
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

                    // El código completo es la credencial verificable; no se muestra un QR decorativo.
                    Box(
                        modifier = Modifier
                            .size(180.dp)
                            .background(Color.White, RoundedCornerShape(12.dp))
                            .border(2.dp, GoldPrimary, RoundedCornerShape(12.dp))
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = activeOrder.pickupToken,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
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
    digitalPaymentLink: DigitalPaymentLink?,
    onViewTicket: () -> Unit,
    onSubmitEvidence: () -> Unit,
    onCancelOrder: () -> Unit,
    onOpenPaymentLink: (String) -> Unit,
    onRefreshPaymentLink: () -> Unit
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
                        Text("Ver código de retiro", color = Color.Black)
                    }
                } else if (order.paymentStatus == PaymentStatus.PENDIENTE) {
                    if (order.paymentMethod == "TARJETA_PASARELA") {
                        Column {
                            val linkState = when (digitalPaymentLink?.status) {
                                "PENDING" -> "enlace activo"
                                "EXPIRED" -> "enlace vencido"
                                "REVIEW" -> "pago en revisión"
                                "PAID" -> "pago verificado"
                                else -> "consultando enlace"
                            }
                            Text("Pago digital pendiente · $linkState",
                                style = MaterialTheme.typography.bodySmall)
                            if (digitalPaymentLink?.orderId == order.id && digitalPaymentLink.status == "PENDING") {
                                Button(onClick = { onOpenPaymentLink(digitalPaymentLink.url) },
                                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary)) {
                                    Text("Abrir pago seguro", color = Color.Black)
                                }
                            }
                            TextButton(onClick = onRefreshPaymentLink) { Text("Actualizar estado") }
                        }
                    } else Text("Pago pendiente en caja", style = MaterialTheme.typography.bodySmall)
                    OutlinedButton(onClick = onCancelOrder) { Text("Cancelar") }
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
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(item.product.name, style = MaterialTheme.typography.titleSmall)
            val warning = when (val remaining = item.availableStock) {
                null -> "Actualiza el carrito para comprobar el stock."
                0 -> "Agotado. Quita este producto."
                else -> if (remaining < item.quantity) "Quedan $remaining unidades. Ajusta la cantidad." else null
            }
            if (warning != null) {
                Text(warning, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(item.unitPrice.amountInSoles + " c/u", style = MaterialTheme.typography.bodySmall)
                    Text(
                        item.subtotal.amountInSoles,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (warning == null) GoldPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { onQuantityChange(item.product.id, item.quantity - 1) }) {
                        Icon(
                            if (item.quantity == 1) Icons.Default.Delete else Icons.Default.Remove,
                            contentDescription = if (item.quantity == 1) "Quitar producto" else "Disminuir cantidad"
                        )
                    }
                    Text(item.quantity.toString(), style = MaterialTheme.typography.titleMedium)
                    IconButton(
                        onClick = { onQuantityChange(item.product.id, item.quantity + 1) },
                        enabled = item.quantity < minOf(20, item.availableStock ?: 0)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Aumentar cantidad")
                    }
                }
            }
        }
    }
}
