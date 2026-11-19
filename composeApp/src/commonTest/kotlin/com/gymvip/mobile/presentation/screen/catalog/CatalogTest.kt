package com.gymvip.mobile.presentation.screen.catalog

import com.gymvip.mobile.domain.model.CatalogOffer
import com.gymvip.mobile.domain.model.MoneyPen
import com.gymvip.mobile.domain.model.Product
import com.gymvip.mobile.domain.model.ProductCategory.SUPLEMENTO
import com.gymvip.mobile.domain.repository.CatalogRepository
import com.gymvip.mobile.domain.usecase.GetCatalogUseCase
import com.gymvip.mobile.presentation.viewmodel.CatalogViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CatalogTest {
    private val offer = CatalogOffer("offer", "Oferta", "Ahorro", "Solo en recepción",
        MoneyPen.fromSoles(135), "2026-11-01", "2026-11-30")
    private val whey = Product("whey", "Proteína sabor vainilla", "900 g", SUPLEMENTO, MoneyPen.fromSoles(150),
        isPromotion = true, brand = "Nutri Gym", catalogCategory = "PROTEINA",
        availability = "AGOTADO", offer = offer)
    private val creatine = Product("creatine", "Creatina", "300 g", SUPLEMENTO,
        MoneyPen.fromSoles(100), brand = "Vida Fit", catalogCategory = "CREATINA")

    @Test
    fun combinesNameBrandCategoryPriceAndCurrentPromotion() {
        val products = listOf(whey, creatine)
        assertEquals(listOf(whey), products.filterCatalog("vainilla", null, null, false, null))
        assertEquals(listOf(whey), products.filterCatalog(" nutri ", SUPLEMENTO, "PROTEINA", true, 135.0))
        assertTrue(products.filterCatalog("nutri", null, null, true, 134.99).isEmpty())
        assertEquals(listOf(creatine), products.filterCatalog("", null, "CREATINA", false, null))
        assertTrue(products.filterCatalog("", null, "CREATINA", true, null).isEmpty())
    }

    @Test
    fun separatesFinishedOffersAndMatchesOfferOrProductName() {
        val archived = whey.copy(offer = null, isPromotion = false, finishedOffer = offer)
        val products = listOf(whey, archived, creatine)
        assertEquals(listOf(whey), products.filterCatalog("Oferta", null, null, true, null))
        assertEquals(listOf(archived), products.filterCatalog("vainilla", null, null, false, null, true))
        assertTrue(products.filterCatalog("Oferta", null, null, false, 135.0, true).isEmpty())
        assertEquals("AGOTADO", archived.availability)
        assertEquals(MoneyPen.fromSoles(150), archived.currentPrice)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun distinguishesEmptyVerifiedCatalogFromFailedRefresh() = runTest {
        val repo = FakeCatalog()
        val viewModel = CatalogViewModel(GetCatalogUseCase(repo), backgroundScope)
        repo.result = Result.success(listOf(whey))
        viewModel.refresh()
        runCurrent()
        assertEquals(listOf(whey), viewModel.uiState.value.products)

        repo.result = Result.failure(IllegalStateException("offline"))
        viewModel.refresh()
        runCurrent()
        assertTrue(viewModel.uiState.value.products.isEmpty())
        assertNotNull(viewModel.uiState.value.errorMessage)

        repo.result = Result.success(emptyList())
        viewModel.refresh()
        runCurrent()
        assertTrue(viewModel.uiState.value.products.isEmpty())
        assertNull(viewModel.uiState.value.errorMessage)
    }

    private class FakeCatalog : CatalogRepository {
        var result: Result<List<Product>> = Result.success(emptyList())
        override fun observeProducts() = emptyFlow<List<Product>>()
        override suspend fun refreshCatalog() = result
        override suspend fun getProductById(id: String) = null
        override suspend fun getThumbnail(path: String): ByteArray = error("Not used")
        override suspend fun scanSupplement(code: String): Nothing = error("Not used")
        override suspend fun updateSupplementBarcode(id: String, barcode: String): Nothing = error("Not used")
    }
}
