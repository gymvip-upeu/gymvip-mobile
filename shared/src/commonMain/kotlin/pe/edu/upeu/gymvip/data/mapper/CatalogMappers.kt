package pe.edu.upeu.gymvip.data.mapper

import pe.edu.upeu.gymvip.data.local.Catalog_product
import pe.edu.upeu.gymvip.data.remote.dto.ProductDto
import pe.edu.upeu.gymvip.domain.model.MoneyPen
import pe.edu.upeu.gymvip.domain.model.Product
import pe.edu.upeu.gymvip.domain.model.ProductCategory

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
