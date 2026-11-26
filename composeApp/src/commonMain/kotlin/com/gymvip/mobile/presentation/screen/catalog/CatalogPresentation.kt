package com.gymvip.mobile.presentation.screen.catalog

import com.gymvip.mobile.domain.model.Product
import com.gymvip.mobile.domain.model.ProductCategory

internal fun catalogColumnCount(widthDp: Float, fontScale: Float): Int =
    if ((widthDp - 44f) / 2f >= 180f * fontScale.coerceAtLeast(1f)) 2 else 1

internal fun Product.catalogPresentation(): String =
    listOfNotNull(flavor, netContent, presentation).map(String::trim).filter(String::isNotEmpty)
        .distinct().joinToString(" · ")

internal fun Product.catalogCategoryLabel(): String = when (catalogCategory ?: category.name) {
    "PROTEINA" -> "Proteína"
    "CREATINA" -> "Creatina"
    "PREENTRENO" -> "Preentreno"
    "AMINOACIDOS" -> "Aminoácidos"
    "VITAMINAS" -> "Vitaminas"
    "OTROS" -> "Otros"
    "SUPLEMENTO" -> "Suplementos"
    "PLAN_MEMBRESIA" -> "Planes"
    "BEBIDA" -> "Bebidas"
    "MERCHANDISING" -> "Merchandising"
    "SERVICIO_ESPECIAL" -> "Servicios"
    else -> catalogCategory.orEmpty().lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() }
}

internal fun Product.canAddFromCatalog(isFinished: Boolean, canPurchase: Boolean): Boolean =
    canPurchase && category == ProductCategory.SUPLEMENTO && availability == "DISPONIBLE" && !isFinished

internal fun Product.catalogDiscountPercent(): Long? {
    val promotionalPrice = offer?.price?.cents ?: return null
    if (price.cents <= 0 || promotionalPrice < 0 || promotionalPrice >= price.cents) return null
    return (((price.cents - promotionalPrice) * 100 + price.cents / 2) / price.cents).coerceIn(0, 100)
}
