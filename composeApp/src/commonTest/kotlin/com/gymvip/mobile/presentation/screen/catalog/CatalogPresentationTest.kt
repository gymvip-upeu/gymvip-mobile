package com.gymvip.mobile.presentation.screen.catalog

import com.gymvip.mobile.domain.model.*
import kotlin.test.*

class CatalogPresentationTest {
    private val product = Product("bar", "Barra Apple Pie 60 g", "Barra proteica", ProductCategory.SUPLEMENTO,
        MoneyPen.fromSoles(14), brand = "Quest", availability = "DISPONIBLE", catalogCategory = "OTROS",
        flavor = "Apple Pie", netContent = "60 g", presentation = "Barra individual")
    @Test fun smallScreensAndLargeTextKeepReadableSingleColumn() {
        assertEquals(1, catalogColumnCount(360f, 1f))
        assertEquals(2, catalogColumnCount(412f, 1f))
        assertEquals(1, catalogColumnCount(412f, 1.3f))
        assertEquals(2, catalogColumnCount(800f, 2f))
    }
    @Test fun presentationMatchesTheMemberFacingLabelAndOmitsMissingData() {
        assertEquals("Apple Pie · 60 g · Barra individual", product.catalogPresentation())
        assertEquals("60 g", product.copy(flavor = null, presentation = " ").catalogPresentation())
        assertEquals("Otros", product.catalogCategoryLabel())
    }
    @Test fun unavailableAndArchivedItemsCannotEnterCartButMemberCanPurchaseWithoutMembershipInput() {
        assertTrue(product.canAddFromCatalog(isFinished = false, canPurchase = true))
        assertFalse(product.copy(availability = "AGOTADO").canAddFromCatalog(false, true))
        assertFalse(product.copy(availability = null).canAddFromCatalog(false, true))
        assertFalse(product.canAddFromCatalog(true, true))
        assertFalse(product.canAddFromCatalog(false, false))
        assertFalse(product.copy(category = ProductCategory.PLAN_MEMBRESIA).canAddFromCatalog(false, true))
    }
    @Test fun discountOnlyUsesCurrentOfferAndDoesNotRecycleFinishedPrices() {
        val offer = CatalogOffer("offer", "Promo", "", "", MoneyPen.fromSoles(7), "2026-11-01", "2026-11-30")
        assertEquals(50L, product.copy(offer = offer).catalogDiscountPercent())
        val archived = product.copy(finishedOffer = offer)
        assertNull(archived.catalogDiscountPercent())
        assertEquals(MoneyPen.fromSoles(14), archived.currentPrice)
        assertNull(product.copy(offer = offer.copy(price = MoneyPen.fromSoles(14))).catalogDiscountPercent())
        assertEquals(0L, product.copy(offer = offer.copy(price = MoneyPen(1399))).catalogDiscountPercent())
    }
}
