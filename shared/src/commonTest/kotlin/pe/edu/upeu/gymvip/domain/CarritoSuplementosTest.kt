package pe.edu.upeu.gymvip.domain

import pe.edu.upeu.gymvip.domain.model.*
import kotlin.test.*

class CarritoSuplementosTest {
    private fun producto(id: String, disponible: Boolean = true) = Suplemento(
        id, "Creatina 500 g", "Marca", "CREATINA", "Pote", "500 g", "Sin sabor", "Descripción", "Creatina",
        MoneyPen(14990), disponible, true, emptyList()
    )

    @Test fun sumaCentimosYCantidadesSinComprar() {
        val p = producto("uno")
        val carrito = CarritoSuplementos().agregar(p, MoneyPen(14990)).agregar(p, MoneyPen(14990))
            .agregar(producto("dos"), MoneyPen(6990))
        assertEquals(3, carrito.cantidad)
        assertEquals(MoneyPen(36970), carrito.total)
        assertEquals(MoneyPen(29980), carrito.lineas.first().subtotal)
        assertEquals(2, carrito.cambiarCantidad("uno", 1).cantidad)
        assertEquals(1, carrito.cambiarCantidad("uno", 0).cantidad)
    }

    @Test fun excluyeAgotadosYLimitaCantidad() {
        val p = producto("uno")
        assertEquals(0, CarritoSuplementos().agregar(producto("agotado", false), p.precio).cantidad)
        var carrito = CarritoSuplementos()
        repeat(25) { carrito = carrito.agregar(p, p.precio) }
        assertEquals(20, carrito.cantidad)
    }
}
