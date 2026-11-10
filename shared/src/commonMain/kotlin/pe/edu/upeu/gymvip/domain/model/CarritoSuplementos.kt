package pe.edu.upeu.gymvip.domain.model

/** Selección local para consultar y pagar presencialmente; no registra una venta. */
data class LineaCarrito(val producto: Suplemento, val precioUnitario: MoneyPen, val cantidad: Int) {
    val subtotal: MoneyPen get() = MoneyPen(precioUnitario.cents * cantidad)
}

data class CarritoSuplementos(val lineas: List<LineaCarrito> = emptyList()) {
    val cantidad: Int get() = lineas.sumOf { it.cantidad }
    val total: MoneyPen get() = lineas.fold(MoneyPen.ZERO) { acumulado, linea -> acumulado + linea.subtotal }

    fun agregar(producto: Suplemento, precio: MoneyPen): CarritoSuplementos {
        if (!producto.disponible) return this
        val actual = lineas.firstOrNull { it.producto.id == producto.id }
        if (actual?.cantidad == 20) return this
        return copy(lineas = if (actual == null) lineas + LineaCarrito(producto, precio, 1)
            else lineas.map { if (it.producto.id == producto.id) it.copy(cantidad = it.cantidad + 1) else it })
    }

    fun cambiarCantidad(id: String, cantidad: Int): CarritoSuplementos = copy(
        lineas = lineas.mapNotNull { linea -> when {
            linea.producto.id != id -> linea
            cantidad <= 0 -> null
            else -> linea.copy(cantidad = cantidad.coerceAtMost(20))
        } }
    )
}
