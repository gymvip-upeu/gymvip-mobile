package com.gymvip.mobile.domain.model

/**
 * Estados desacoplados del ciclo de vida del pedido, pago y retiro físico.
 */
enum class OrderStatus(val label: String) {
    CREADO("Creado"),
    PENDIENTE_PAGO("Pendiente de Pago"),
    PAGADO_POR_VERIFICAR("Comprobante por Verificar"),
    CONFIRMADO_PREPARACION("Confirmado en Preparación"),
    LISTO_PARA_RETIRO("Listo para Retiro en Mostrador"),
    ENTREGADO("Entregado al Socio"),
    CANCELADO("Cancelado"),
    RECHAZADO("Pago Observado/Rechazado"),
    EXPIRADO("Reserva Expirada")
}

enum class PaymentStatus(val label: String) {
    PENDIENTE("Pendiente"),
    EVIDENCIA_SUBIDA("Evidencia Registrada"),
    VERIFICADO("Verificado en Caja"),
    RECHAZADO("Rechazado"),
    DEVUELTO("Devuelto")
}

enum class FulfillmentStatus(val label: String) {
    PENDIENTE("Pendiente"),
    EN_PREPARACION("En Preparación"),
    LISTO("Listo para Entrega"),
    ENTREGADO("Entregado"),
    ANULADO("Anulado")
}

/**
 * Línea individual en el carrito de compras de suplementos.
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

/**
 * Snapshot inmutable de ítem en pedido persistido.
 */
data class OrderItemSnapshot(
    val productName: String,
    val brand: String,
    val presentation: String,
    val quantity: Int,
    val unitPrice: MoneyPen,
    val lineTotal: MoneyPen
)

/**
 * Pedido persistido con separación de estados y reserva temporal de stock.
 */
data class SupplementOrder(
    val id: String,
    val orderNumber: String,
    val items: List<OrderItemSnapshot>,
    val subtotal: MoneyPen,
    val discount: MoneyPen,
    val total: MoneyPen,
    val orderStatus: OrderStatus,
    val paymentStatus: PaymentStatus,
    val fulfillmentStatus: FulfillmentStatus,
    val paymentMethod: String,
    val paymentReference: String?,
    val reservationExpiresAtUtc: String?,
    val pickupToken: String?,
    val hasPickupToken: Boolean
)

/**
 * Ticket de Retiro en Mostrador.
 * Referencia opaca con advertencia tributaria explícita.
 */
data class PickupTicket(
    val orderNumber: String,
    val pickupToken: String,
    val clientName: String,
    val total: MoneyPen,
    val isConsumed: Boolean,
    val instructions: String = "Presente este código QR en el mostrador de recepción del gimnasio para reclamar sus suplementos.",
    val disclaimer: String = "Este ticket es una credencial de entrega física y no constituye comprobante de pago electrónico SUNAT."
)
