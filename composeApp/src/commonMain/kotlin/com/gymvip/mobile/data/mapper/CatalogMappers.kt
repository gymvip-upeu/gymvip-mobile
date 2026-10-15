package com.gymvip.mobile.data.mapper

import com.gymvip.mobile.data.local.Catalog_product
import com.gymvip.mobile.data.remote.dto.ProductDto
import com.gymvip.mobile.domain.model.MoneyPen
import com.gymvip.mobile.domain.model.Product
import com.gymvip.mobile.domain.model.ProductCategory

fun ProductDto.toDomain(): Product {
    val domainCategory = try {
        ProductCategory.valueOf(category.uppercase())
    } catch (_: Exception) {
        ProductCategory.PLAN_MEMBRESIA
    }
    return Product(
        id = id,
        name = name,
        description = description,
        category = domainCategory,
        price = MoneyPen.fromDouble(pricePen),
        isPromotion = isPromotion,
        promotionDiscountPercent = promotionDiscountPercent,
        imageUrl = imageUrl
    )
}

fun Catalog_product.toDomain(): Product {
    val domainCategory = try {
        ProductCategory.valueOf(category.uppercase())
    } catch (_: Exception) {
        ProductCategory.PLAN_MEMBRESIA
    }
    return Product(
        id = id,
        name = name,
        description = description,
        category = domainCategory,
        price = MoneyPen(priceCents),
        isPromotion = isPromotion == 1L,
        promotionDiscountPercent = promotionDiscountPercent?.toInt(),
        imageUrl = imageUrl
    )
}
