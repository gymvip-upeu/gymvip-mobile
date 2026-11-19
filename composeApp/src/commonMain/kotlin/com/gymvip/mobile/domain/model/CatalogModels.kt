package com.gymvip.mobile.domain.model

enum class ProductCategory {
    PLAN_MEMBRESIA,
    SUPLEMENTO,
    BEBIDA,
    MERCHANDISING,
    SERVICIO_ESPECIAL
}

data class Product(
    val id: String,
    val name: String,
    val description: String,
    val category: ProductCategory,
    val price: MoneyPen,
    val isPromotion: Boolean = false,
    val promotionDiscountPercent: Int? = null,
    val imageUrl: String? = null,
    val brand: String? = null,
    val catalogCategory: String? = null,
    val availability: String? = null,
    val presentation: String? = null,
    val netContent: String? = null,
    val flavor: String? = null,
    val ingredients: String? = null,
    val offer: CatalogOffer? = null
) {
    val currentPrice: MoneyPen get() = offer?.price ?: price
}

data class CatalogOffer(
    val id: String,
    val name: String,
    val description: String,
    val conditions: String,
    val price: MoneyPen,
    val startDate: String,
    val endDate: String
)

data class Promotion(
    val id: String,
    val title: String,
    val description: String,
    val discountPercent: Int,
    val validUntil: String,
    val applicableCategory: ProductCategory
)
