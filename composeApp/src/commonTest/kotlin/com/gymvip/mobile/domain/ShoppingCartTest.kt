package com.gymvip.mobile.domain

import com.gymvip.mobile.domain.model.*
import kotlin.test.*

class ShoppingCartTest {

    private fun testProduct(id: String) = Product(
        id = id,
        name = "Creatina Monohidratada 500 g",
        description = "Suplemento de alta pureza para fuerza y volumen.",
        category = ProductCategory.SUPLEMENTO,
        price = MoneyPen(14990),
        isPromotion = false,
        promotionDiscountPercent = null,
        imageUrl = null
    )

    @Test
    fun testSumaCentimosYCantidadesSinComprarEnLinea() {
        val p1 = testProduct("prod-01")
        val p2 = Product(
            id = "prod-02",
            name = "Proteína Whey 1 kg",
            description = "Proteína concentrada",
            category = ProductCategory.SUPLEMENTO,
            price = MoneyPen(6990)
        )

        val cart = ShoppingCart()
            .addProduct(p1, MoneyPen(14990))
            .addProduct(p1, MoneyPen(14990))
            .addProduct(p2, MoneyPen(6990))

        assertEquals(3, cart.totalQuantity)
        assertEquals(MoneyPen(36970), cart.totalPrice)
        assertEquals(MoneyPen(29980), cart.items.first().subtotal)
        assertEquals(2, cart.changeQuantity("prod-01", 1).totalQuantity)
        assertEquals(1, cart.changeQuantity("prod-01", 0).totalQuantity)
    }

    @Test
    fun testLimitaCantidadMaximaAVeinte() {
        val p = testProduct("prod-01")
        var cart = ShoppingCart()
        repeat(25) { cart = cart.addProduct(p, p.price) }
        assertEquals(20, cart.totalQuantity)
        assertEquals(20, cart.items.first().quantity)
    }
}
