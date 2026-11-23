package com.gymvip.mobile.presentation.screen.catalog

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddShoppingCart
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.gymvip.mobile.domain.model.Product
import com.gymvip.mobile.domain.model.ProductCategory

/** Presentación de tienda; los precios, disponibilidad y acciones vienen del catálogo existente. */
@Composable
internal fun CatalogShopCard(
    product: Product,
    isFinished: Boolean,
    onViewDetails: () -> Unit,
    onAddToFavorites: (Product) -> Unit,
    onAddToCart: ((Product) -> Unit)?,
    thumbnail: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    val offer = if (isFinished) product.finishedOffer else product.offer
    val accent = if (MaterialTheme.colorScheme.background.luminance() < 0.5f) Color(0xFFE7C579) else Color(0xFF765100)
    val canAdd = onAddToCart != null && product.category == ProductCategory.SUPLEMENTO &&
        product.availability == "DISPONIBLE" && !isFinished
    Card(
        modifier = modifier.clickable(onClickLabel = "Ver detalle de ${product.name}", onClick = onViewDetails),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Box(Modifier.fillMaxWidth().height(164.dp)) {
            thumbnail()
            if (offer != null) {
                val badge = if (isFinished) "Finalizada" else {
                    val regular = product.price.cents
                    val percent = if (regular > 0) ((regular - offer.price.cents) * 100 + regular / 2) / regular else 0
                    "-${percent.coerceAtLeast(0)}%"
                }
                Surface(Modifier.align(Alignment.TopStart), color = if (isFinished) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFE60032),
                    shape = RoundedCornerShape(bottomEnd = 10.dp)) {
                    Text(badge, Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                        color = if (isFinished) MaterialTheme.colorScheme.onSurfaceVariant else Color.White,
                        style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                }
            }
            product.catalogCategory?.let { category ->
                Surface(Modifier.align(Alignment.BottomStart).padding(8.dp), color = Color(0xDD11151B),
                    shape = RoundedCornerShape(7.dp)) {
                    Text(category.lowercase().replaceFirstChar { it.uppercase() }, Modifier.padding(5.dp),
                        color = Color.White, style = MaterialTheme.typography.labelSmall)
                }
            }
            IconButton(onClick = { onAddToFavorites(product) }, Modifier.align(Alignment.TopEnd)) {
                Icon(Icons.Outlined.StarBorder, "Guardar ${product.name} en favoritos", tint = accent)
            }
        }
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(product.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold,
                maxLines = 2, minLines = 2, overflow = TextOverflow.Ellipsis)
            product.brand?.takeIf { it.isNotBlank() }?.let {
                Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (offer != null && !isFinished) Text(product.price.amountInSoles,
                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                textDecoration = TextDecoration.LineThrough)
            Text((if (isFinished) product.price else product.currentPrice).amountInSoles,
                style = MaterialTheme.typography.titleMedium, color = accent, fontWeight = FontWeight.Bold)
            Text(when (product.availability) {
                "AGOTADO" -> "Agotado"
                "DISPONIBLE" -> "Disponible"
                else -> "Consultar en recepción"
            },
                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (canAdd) Button(onClick = { onAddToCart?.invoke(product) }, Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 7.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE7C579), contentColor = Color(0xFF1C1608))) {
                Icon(Icons.Outlined.AddShoppingCart, null, Modifier.size(17.dp))
                Spacer(Modifier.width(4.dp))
                Text("Agregar", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            } else TextButton(onClick = onViewDetails, Modifier.fillMaxWidth()) { Text("Ver detalle") }
        }
    }
}
