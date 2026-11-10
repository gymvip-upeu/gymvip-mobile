package pe.edu.upeu.gymvip.domain.model

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
    val imageUrl: String? = null
)

data class Promotion(
    val id: String,
    val title: String,
    val description: String,
    val discountPercent: Int,
    val validUntil: String,
    val applicableCategory: ProductCategory
)
