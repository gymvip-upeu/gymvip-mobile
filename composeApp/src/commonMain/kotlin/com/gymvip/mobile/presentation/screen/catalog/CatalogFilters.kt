package com.gymvip.mobile.presentation.screen.catalog

import com.gymvip.mobile.domain.model.Product
import com.gymvip.mobile.domain.model.ProductCategory

internal fun List<Product>.filterCatalog(
    query: String, category: ProductCategory?, supplementCategory: String?, onlyPromotions: Boolean,
    maxPrice: Double?, finishedOnly: Boolean = false
): List<Product> {
    val text = query.trim()
    return filter { product ->
        (category == null || product.category == category) &&
            (supplementCategory == null || product.catalogCategory == supplementCategory) &&
            (!onlyPromotions || product.offer != null) &&
            (!finishedOnly || product.finishedOffer != null) &&
            (product.name.contains(text, ignoreCase = true) ||
                product.brand?.contains(text, ignoreCase = true) == true ||
                (if (finishedOnly) product.finishedOffer else product.offer)
                    ?.name?.contains(text, ignoreCase = true) == true) &&
            (maxPrice == null || (if (finishedOnly) product.price else product.currentPrice).cents / 100.0 <= maxPrice)
    }
}
