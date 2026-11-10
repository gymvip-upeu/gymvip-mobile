package pe.edu.upeu.gymvip.presentation.screen.catalog

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import pe.edu.upeu.gymvip.presentation.component.EstadoVacio
import pe.edu.upeu.gymvip.presentation.viewmodel.ComunidadViewModel

@Composable
fun CarritoScreen(vm: ComunidadViewModel, volver: () -> Unit) {
    val s by vm.state.collectAsState()
    var mostrarResumen by remember { mutableStateOf(false) }
    MarcoComunidad("Mi carrito", "Selecciona tus suplementos y consulta en recepción.", volver) {
        if (s.carrito.lineas.isEmpty()) item { EstadoVacio("Tu carrito está vacío. Explora el catálogo para agregar suplementos.") }
        s.carrito.lineas.forEach { linea -> item(key = linea.producto.id) {
            Card(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    FotoProducto(linea.producto, vm, Modifier.size(88.dp))
                    Column(Modifier.weight(1f)) {
                        Text(linea.producto.nombre, style = MaterialTheme.typography.titleSmall)
                        Text(linea.precioUnitario.amountInSoles + " c/u", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(linea.subtotal.amountInSoles, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleMedium)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton({ vm.cantidadCarrito(linea.producto.id, linea.cantidad - 1) }) {
                                Icon(Icons.Outlined.Remove, "Quitar una unidad")
                            }
                            Text("${linea.cantidad}")
                            IconButton({ vm.cantidadCarrito(linea.producto.id, linea.cantidad + 1) }, enabled = linea.cantidad < 20) {
                                Icon(Icons.Outlined.Add, "Agregar una unidad")
                            }
                            IconButton({ vm.cantidadCarrito(linea.producto.id, 0) }) {
                                Icon(Icons.Outlined.DeleteOutline, "Quitar ${linea.producto.nombre}")
                            }
                        }
                    }
                }
            }
        } }
        if (s.carrito.lineas.isNotEmpty()) item {
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total referencial", style = MaterialTheme.typography.titleMedium)
                        Text(s.carrito.total.amountInSoles, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleLarge)
                    }
                    Text("El carrito es una lista local. No reserva stock ni registra una compra. Confirma precio y disponibilidad en recepción antes de pagar.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Button({ mostrarResumen = true }, Modifier.fillMaxWidth()) { Text("Mostrar lista en recepción") }
                    TextButton(vm::vaciarCarrito, Modifier.align(Alignment.End)) { Text("Vaciar carrito") }
                }
            }
        }
    }
    if (mostrarResumen) AlertDialog(onDismissRequest = { mostrarResumen = false },
        title = { Text("Lista para recepción") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            s.carrito.lineas.forEach { Text("${it.cantidad} × ${it.producto.nombre} — ${it.subtotal.amountInSoles}") }
            HorizontalDivider()
            Text("Total referencial: ${s.carrito.total.amountInSoles}", color = MaterialTheme.colorScheme.primary)
            Text("Pago y confirmación de stock presenciales.", style = MaterialTheme.typography.bodySmall)
        } },
        confirmButton = { TextButton({ mostrarResumen = false }) { Text("Cerrar") } })
}
