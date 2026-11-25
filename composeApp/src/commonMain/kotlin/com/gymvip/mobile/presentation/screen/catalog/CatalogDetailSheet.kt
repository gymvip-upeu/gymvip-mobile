package com.gymvip.mobile.presentation.screen.catalog

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddShoppingCart
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gymvip.mobile.domain.model.Product
import com.gymvip.mobile.domain.model.ProductCategory
import com.gymvip.mobile.presentation.screen.faq.receptionWhatsAppUrl

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CatalogDetailSheet(
    product: Product,
    isFinished: Boolean,
    receptionWhatsAppNumber: String?,
    onDismiss: () -> Unit,
    onAddToCart: ((Product) -> Unit)?,
    catalogNotice: String? = null,
    image: @Composable () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val uriHandler = LocalUriHandler.current
    val whatsApp = receptionWhatsAppUrl(receptionWhatsAppNumber)
    var contactFailed by remember(product.id) { mutableStateOf(false) }
    val offer = if (isFinished) product.finishedOffer else product.offer
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = colors.surface, contentColor = colors.onSurface) {
        Column(Modifier.fillMaxWidth().fillMaxHeight(0.90f)) {
            Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Text("Detalle del producto", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                IconButton(onClick = onDismiss) { Icon(Icons.Outlined.Close, "Cerrar detalle") }
            }
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Surface(shape = MaterialTheme.shapes.large, border = BorderStroke(1.dp, colors.outlineVariant)) { image() }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    Text(product.catalogCategoryLabel().uppercase(), Modifier.weight(1f),
                        style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
                    CatalogAvailability(product)
                }
                Text(product.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                product.brand?.takeIf(String::isNotBlank)?.let {
                    Text(it, style = MaterialTheme.typography.titleMedium, color = colors.onSurfaceVariant)
                }
                product.catalogPresentation().takeIf(String::isNotBlank)?.let {
                    Text(it, style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant)
                }
                CatalogPrice(product, isFinished, prominent = true)
                if (isFinished) Text("Promoción finalizada. Su precio promocional ya no está vigente.",
                    color = colors.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                product.description.takeIf(String::isNotBlank)?.let {
                    Text("Descripción", style = MaterialTheme.typography.titleSmall)
                    Text(it, style = MaterialTheme.typography.bodyMedium)
                }
                product.ingredients?.takeIf(String::isNotBlank)?.let {
                    Text("Ingredientes", style = MaterialTheme.typography.titleSmall)
                    Text(it, style = MaterialTheme.typography.bodyMedium)
                }
                offer?.let {
                    HorizontalDivider(color = colors.outlineVariant)
                    Text(if (isFinished) "Promoción anterior" else "Promoción vigente", style = MaterialTheme.typography.labelLarge,
                        color = catalogAccentColor())
                    Text(it.name, style = MaterialTheme.typography.titleMedium)
                    if (it.description.isNotBlank()) Text(it.description)
                    if (isFinished) Text("Precio de esa promoción: ${it.price.amountInSoles} · Ya no aplica")
                    else if (product.price.cents > it.price.cents) Text("Ahorras ${(product.price - it.price).amountInSoles}", color = catalogAccentColor())
                    Text("Vigencia: ${it.startDate} al ${it.endDate}", style = MaterialTheme.typography.bodySmall)
                    if (it.conditions.isNotBlank()) {
                        Text("Condiciones", style = MaterialTheme.typography.titleSmall)
                        Text(it.conditions, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                if (product.availability == "AGOTADO") Text("Puedes consultar sus características. No se puede agregar al carrito mientras esté agotado.",
                    style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
            }
            HorizontalDivider(color = colors.outlineVariant)
            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                catalogNotice?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = colors.error) }
                if (product.category == ProductCategory.SUPLEMENTO && !isFinished) {
                    Text("La disponibilidad del catálogo no reserva unidades. Las existencias se comprueban al confirmar el pedido.", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                }
                if (onAddToCart != null && product.category == ProductCategory.SUPLEMENTO && !isFinished) {
                    Button(onClick = { onAddToCart(product); onDismiss() },
                        enabled = product.canAddFromCatalog(isFinished, true),
                        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                        Icon(Icons.Outlined.AddShoppingCart, null, Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(if (product.availability == "AGOTADO") "Producto agotado" else "Agregar al carrito")
                    }
                }
                if (whatsApp != null) OutlinedButton(onClick = {
                    contactFailed = runCatching { uriHandler.openUri(whatsApp) }.isFailure
                }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = catalogAccentColor())) { Text("Consultar a recepción") }
                if (contactFailed) Text("No se pudo abrir WhatsApp. Intenta nuevamente.", color = colors.error)
            }
        }
    }
}
