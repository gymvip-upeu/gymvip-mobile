package com.gymvip.mobile.domain.model

/**
 * Línea individual en el carrito de suplementos.
 * El carrito es local para consulta presencial en recepción y no registra una venta en línea.
 */
data class CartItem(
    val product: Product,
    val unitPrice: MoneyPen,
    val quantity: Int
) {
    val subtotal: MoneyPen get() = MoneyPen(unitPrice.cents * quantity)
}

/**
 * Carrito de suplementos con cálculo de subtotales, totales y límite de cantidad (máximo 20 unidades).
 */
data class ShoppingCart(
    val items: List<CartItem> = emptyList()
) {
    val totalQuantity: Int get() = items.sumOf { it.quantity }
    val totalPrice: MoneyPen get() = items.fold(MoneyPen.ZERO) { acc, item -> acc + item.subtotal }

    fun addProduct(product: Product, price: MoneyPen): ShoppingCart {
        val existing = items.firstOrNull { it.product.id == product.id }
        if (existing?.quantity == 20) return this
        return copy(
            items = if (existing == null) {
                items + CartItem(product, price, 1)
            } else {
                items.map { if (it.product.id == product.id) it.copy(quantity = it.quantity + 1) else it }
            }
        )
    }

    fun changeQuantity(productId: String, quantity: Int): ShoppingCart = copy(
        items = items.mapNotNull { item ->
            when {
                item.product.id != productId -> item
                quantity <= 0 -> null
                else -> item.copy(quantity = quantity.coerceAtMost(20))
            }
        }
    )

    fun clear(): ShoppingCart = copy(items = emptyList())
}
