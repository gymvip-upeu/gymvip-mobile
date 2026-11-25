package com.gymvip.mobile.presentation.viewmodel

import com.gymvip.mobile.data.remote.dto.AdminSupplementDto
import com.gymvip.mobile.domain.model.*
import com.gymvip.mobile.domain.repository.CatalogRepository
import com.gymvip.mobile.domain.usecase.GetCatalogUseCase
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import kotlin.test.*

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class CatalogRefreshViewModelTest {
    private fun product(price: Long = 1400) = Product("quest", "Barra Apple Pie", "Barra individual", ProductCategory.SUPLEMENTO, MoneyPen(price), availability = "DISPONIBLE")
    private class Repository : CatalogRepository {
        var response = Result.success(emptyList<Product>())
        var pending: CompletableDeferred<Result<List<Product>>>? = null
        var calls = 0
        override fun observeProducts() = flowOf(emptyList<Product>())
        override suspend fun refreshCatalog(): Result<List<Product>> { calls++; return pending?.await() ?: response }
        override suspend fun getThumbnail(path: String) = byteArrayOf()
        override suspend fun getProductById(id: String): Product? = null
        override suspend fun scanSupplement(code: String): Result<AdminSupplementDto> = error("unused")
        override suspend fun updateSupplementBarcode(id: String, barcode: String): Result<AdminSupplementDto> = error("unused")
    }
    @Test fun failedRefreshRetainsCompletePreviousDataAndRetryReplacesIt() = runTest {
        val repository = Repository()
        val vm = CatalogViewModel(GetCatalogUseCase(repository), backgroundScope)
        repository.response = Result.success(listOf(product()))
        vm.refresh(); runCurrent()
        repository.response = Result.failure(IllegalStateException("network"))
        vm.refresh(); runCurrent()
        assertEquals(listOf(product()), vm.uiState.value.products)
        assertTrue(vm.uiState.value.isStale)
        assertNotNull(vm.uiState.value.errorMessage)
        repository.response = Result.success(listOf(product(1500).copy(description = "Nueva descripción", availability = "AGOTADO")))
        vm.refresh(); runCurrent()
        assertEquals(1500L, vm.uiState.value.products.single().price.cents)
        assertEquals("AGOTADO", vm.uiState.value.products.single().availability)
        assertFalse(vm.uiState.value.isStale)
        assertNull(vm.uiState.value.errorMessage)
    }
    @Test fun authoritativeEmptyResponseRemovesHiddenProductsInsteadOfMergingOldCache() = runTest {
        val repository = Repository()
        val vm = CatalogViewModel(GetCatalogUseCase(repository), backgroundScope)
        repository.response = Result.success(listOf(product(), product()))
        vm.refresh(); runCurrent()
        assertEquals(1, vm.uiState.value.products.size)
        repository.response = Result.success(emptyList())
        vm.refresh(); runCurrent()
        assertTrue(vm.uiState.value.products.isEmpty())
        assertFalse(vm.uiState.value.isStale)
    }
    @Test fun initialFailureDoesNotPretendToHavePreviousInformation() = runTest {
        val repository = Repository()
        repository.response = Result.failure(IllegalStateException("network"))
        val vm = CatalogViewModel(GetCatalogUseCase(repository), backgroundScope)
        vm.refresh(); runCurrent()
        assertFalse(vm.uiState.value.hasLoaded)
        assertFalse(vm.uiState.value.isStale)
        assertNotNull(vm.uiState.value.errorMessage)
    }
    @Test fun repeatedRefreshWhileLoadingDoesNotStartDuplicateRequests() = runTest {
        val repository = Repository()
        val pending = CompletableDeferred<Result<List<Product>>>()
        repository.pending = pending
        val vm = CatalogViewModel(GetCatalogUseCase(repository), backgroundScope)
        vm.refresh(); vm.refresh(); runCurrent()
        assertTrue(vm.uiState.value.isLoading)
        assertEquals(1, repository.calls)
        pending.complete(Result.success(listOf(product())))
        runCurrent()
        assertFalse(vm.uiState.value.isLoading)
    }
}
