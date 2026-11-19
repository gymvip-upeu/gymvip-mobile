package com.gymvip.mobile.presentation.screen.catalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import com.gymvip.mobile.data.remote.api.PickupOrder
import com.gymvip.mobile.data.remote.api.PickupOrders
import com.gymvip.mobile.data.remote.api.PickupOrdersApi
import com.gymvip.mobile.data.remote.api.PickupRequest
import com.gymvip.mobile.domain.model.MoneyPen
import com.gymvip.mobile.domain.model.Product
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
@Composable
internal fun PickupOrdersDialog(api: PickupOrdersApi, product: Product?, onRefreshCatalog: () -> Unit,
                                onClose: () -> Unit) {
    val scope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current
    val requestId = remember(product?.id) { Uuid.random().toString() }
    var orders by remember { mutableStateOf<PickupOrders?>(null) }
    var submitted by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    suspend fun perform(message: String, action: suspend () -> Unit) {
        busy = true
        error = null
        try {
            action()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            error = message
        } finally {
            busy = false
        }
    }

    fun save(order: PickupOrder) {
        orders = orders?.let { current ->
            current.copy(orders = listOf(order) + current.orders.filterNot { it.id == order.id })
        }
    }

    LaunchedEffect(api) {
        perform("No se pudieron cargar tus pedidos.") { orders = api.list() }
    }

    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(if (product != null && !submitted) "Compra de prueba" else "Mis pedidos") },
        text = {
            Column(Modifier.heightIn(max = 400.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Mercado Pago de prueba · Recojo en recepción")
                if (orders?.sandboxEnabled == false) Text("El pago de prueba aún no está habilitado.")
                if (product != null && !submitted) {
                    Text("1 × ${product.name} · ${product.currentPrice.amountInSoles}")
                    if (orders?.sandboxEnabled == true) {
                        Button(enabled = !busy, onClick = {
                            scope.launch {
                                perform("No se pudo iniciar el pago. Si cambió el precio, actualiza el catálogo.") {
                                    save(api.create(PickupRequest(requestId, product.id, 1,
                                        product.currentPrice.cents / 100.0)))
                                    submitted = true
                                }
                            }
                        }) { Text("Crear pedido de prueba") }
                    }
                }
                if (busy) Text("Consultando…")
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                if (!busy && orders == null) {
                    OutlinedButton(onClick = {
                        scope.launch {
                            perform("No se pudieron cargar tus pedidos.") { orders = api.list() }
                        }
                    }) { Text("Reintentar") }
                }
                if (!busy && orders?.orders?.isEmpty() == true && product == null) Text("Aún no tienes pedidos.")
                orders?.orders.orEmpty().forEach { order ->
                    Text("${order.quantity} × ${order.productName} · ${MoneyPen.fromDouble(order.totalPen).amountInSoles}",
                        style = MaterialTheme.typography.titleSmall)
                    Text(when (order.status) {
                        "PAGADO_PRUEBA" -> "Pago de prueba confirmado"
                        "RECHAZADO" -> "Pago rechazado"
                        "VENCIDO" -> "Pedido vencido"
                        "CANCELADO" -> "Pedido cancelado"
                        "REEMBOLSADO" -> "Pago reembolsado o revertido"
                        else -> "Pendiente de verificación"
                    })
                    if (orders?.sandboxEnabled == true) {
                        order.checkoutUrl?.let { checkout ->
                            Button(enabled = !busy, onClick = {
                                if (!checkout.startsWith("https://www.mercadopago.com.pe/checkout/v1/redirect?") &&
                                    !checkout.startsWith("https://mercadopago.com.pe/checkout/v1/redirect?")) {
                                    error = "Enlace de pago inválido."
                                } else if (runCatching { uriHandler.openUri(checkout) }.isFailure) {
                                    error = "No se pudo abrir Mercado Pago."
                                }
                            }) { Text("Abrir Mercado Pago") }
                        }
                        OutlinedButton(enabled = !busy, onClick = {
                            scope.launch {
                                perform("No se pudo verificar el pago.") { save(api.refresh(order.id)) }
                            }
                        }) { Text("Consultar estado") }
                    }
                }
            }
        },
        confirmButton = { OutlinedButton(onClick = onClose) { Text("Cerrar") } },
        dismissButton = {
            if (error != null && product != null && !submitted) {
                OutlinedButton(onClick = { onRefreshCatalog(); onClose() }) { Text("Actualizar catálogo") }
            }
        }
    )
}
