package com.gymvip.mobile.presentation.screen.catalog

import com.gymvip.mobile.domain.model.CatalogOffer
import com.gymvip.mobile.domain.model.MoneyPen
import com.gymvip.mobile.domain.model.Product
import com.gymvip.mobile.domain.model.ProductCategory
import kotlin.test.Test
import kotlin.test.assertEquals

class CatalogFiltersTest {
    @Test
    fun promotionFiltersSeparateCurrentFinishedAndRegularProducts() {
        val current = product("Oferta vigente", offer = offer("active"))
        val finished = product("Oferta finalizada", finishedOffer = offer("finished"))
        val regular = product("Precio regular")

        assertEquals(
            listOf(current),
            listOf(current, finished, regular).filterCatalog(
                query = "",
                category = null,
                supplementCategory = null,
                onlyPromotions = true,
                maxPrice = null
            )
        )
        assertEquals(
            listOf(finished),
            listOf(current, finished, regular).filterCatalog(
                query = "",
                category = null,
                supplementCategory = null,
                onlyPromotions = false,
                maxPrice = null,
                finishedOnly = true
            )
        )
        assertEquals(listOf(current, finished, regular), listOf(current, finished, regular).filterCatalog(
            query = "",
            category = null,
            supplementCategory = null,
            onlyPromotions = false,
            maxPrice = null
        ))
    }

    private fun product(
        name: String,
        offer: CatalogOffer? = null,
        finishedOffer: CatalogOffer? = null
    ) = Product(
        id = name,
        name = name,
        description = "Descripción de prueba",
        category = ProductCategory.SUPLEMENTO,
        price = MoneyPen.fromDouble(100.0),
        offer = offer,
        finishedOffer = finishedOffer
    )

    private fun offer(id: String) = CatalogOffer(
        id = id,
        name = "Promoción $id",
        description = "Descripción de promoción",
        conditions = "Condiciones de prueba",
        price = MoneyPen.fromDouble(80.0),
        startDate = "2026-11-01",
        endDate = "2026-11-30"
    )
}
