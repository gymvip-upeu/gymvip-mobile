package com.gymvip.mobile.presentation.screen.catalog

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.AddShoppingCart
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.gymvip.mobile.domain.model.Product

@Composable
internal fun catalogAccentColor(): Color {
    val colors = MaterialTheme.colorScheme
    val primary = colors.primary.luminance()
    val surface = colors.surface.luminance()
    val contrast = (maxOf(primary, surface) + 0.05f) / (minOf(primary, surface) + 0.05f)
    return if (contrast >= 4.5f) colors.primary else colors.onSurface
}

@Composable
internal fun CatalogAvailability(product: Product, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Surface(modifier, shape = RoundedCornerShape(50),
        color = if (product.availability == "DISPONIBLE") colors.primaryContainer else colors.surfaceVariant,
        contentColor = if (product.availability == "DISPONIBLE") colors.onPrimaryContainer else colors.onSurfaceVariant,
        border = BorderStroke(1.dp, colors.outlineVariant.copy(alpha = 0.5f))) {
        Text(when (product.availability) {
            "DISPONIBLE" -> "Disponible"
            "AGOTADO" -> "Agotado"
            else -> "Consultar disponibilidad"
        }, Modifier.padding(horizontal = 12.dp, vertical = 7.dp), style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
internal fun CatalogPrice(product: Product, isFinished: Boolean, prominent: Boolean = false) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        if (isFinished) Text("Precio regular", style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        else if (product.offer?.price?.cents?.let { it < product.price.cents } == true) Text(product.price.amountInSoles,
            style = MaterialTheme.typography.bodySmall, textDecoration = TextDecoration.LineThrough,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text((if (isFinished) product.price else product.currentPrice).amountInSoles,
            style = if (prominent) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.titleLarge,
            color = catalogAccentColor(), fontWeight = FontWeight.Bold)
    }
}

/** Existing callbacks continue to own cart, favorite and server operations. */
@Composable
internal fun CatalogShopCard(
    product: Product,
    isFinished: Boolean,
    onViewDetails: () -> Unit,
    onAddToFavorites: (Product) -> Unit,
    onAddToCart: ((Product) -> Unit)?,
    thumbnail: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    isFavorite: Boolean = false,
    compact: Boolean = false
) {
    val colors = MaterialTheme.colorScheme
    val canAdd = product.canAddFromCatalog(isFinished, onAddToCart != null)
    Card(modifier.clickable(onClickLabel = "Ver detalle de ${product.name}", onClick = onViewDetails),
        shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = colors.surface),
        border = BorderStroke(1.dp, Brush.linearGradient(listOf(colors.primary.copy(alpha = 0.35f),
            colors.outlineVariant.copy(alpha = 0.55f), colors.primary.copy(alpha = 0.10f))))) {
        Box(Modifier.fillMaxWidth().height(if (compact) 184.dp else 224.dp)) {
            thumbnail()
            if (isFinished) Surface(Modifier.align(Alignment.TopStart).padding(10.dp),
                shape = RoundedCornerShape(7.dp), color = colors.surfaceVariant, contentColor = colors.onSurfaceVariant) {
                Text("Promoción finalizada", Modifier.padding(8.dp), style = MaterialTheme.typography.labelSmall)
            } else product.catalogDiscountPercent()?.let { discount ->
                Surface(Modifier.align(Alignment.TopStart).padding(10.dp), shape = RoundedCornerShape(7.dp),
                    color = colors.errorContainer, contentColor = colors.onErrorContainer) {
                    Text(if (discount > 0) "−$discount%" else "Oferta", Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                }
            }
            Surface(Modifier.align(Alignment.TopEnd).padding(8.dp), shape = CircleShape,
                color = colors.surface.copy(alpha = 0.94f)) {
                IconButton(onClick = { onAddToFavorites(product) }) {
                    Icon(if (isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                        if (isFavorite) "Favorito: ${product.name}" else "Guardar ${product.name} en favoritos",
                        tint = catalogAccentColor())
                }
            }
            CatalogAvailability(product, Modifier.align(Alignment.BottomEnd).padding(10.dp))
        }
        Column(Modifier.fillMaxWidth().padding(if (compact) 14.dp else 20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(product.catalogCategoryLabel().uppercase(), style = MaterialTheme.typography.labelSmall,
                color = colors.onSurfaceVariant)
            Text(product.name, style = if (compact) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold, color = colors.onSurface, minLines = if (compact) 2 else 1,
                maxLines = 3, overflow = TextOverflow.Ellipsis)
            product.brand?.takeIf(String::isNotBlank)?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            }
            product.catalogPresentation().takeIf(String::isNotBlank)?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant,
                    maxLines = 3, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.height(2.dp))
            CatalogPrice(product, isFinished, prominent = !compact)
            if (isFinished) Text("El precio de esa promoción ya no aplica.", style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            if (onAddToCart != null && product.category == com.gymvip.mobile.domain.model.ProductCategory.SUPLEMENTO && !isFinished) Button(onClick = { onAddToCart(product) }, enabled = canAdd,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp)) {
                Icon(Icons.Outlined.AddShoppingCart, null, Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(if (product.availability == "AGOTADO") "Agotado" else if (compact) "Agregar" else "Agregar al carrito")
            }
            TextButton(onClick = onViewDetails, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                colors = ButtonDefaults.textButtonColors(contentColor = catalogAccentColor())) {
                Text("Ver detalle")
            }
        }
    }
}
