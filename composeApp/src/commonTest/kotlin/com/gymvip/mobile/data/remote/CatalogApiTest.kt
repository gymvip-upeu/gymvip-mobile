package com.gymvip.mobile.data.remote

import com.gymvip.mobile.data.remote.api.CatalogApi
import com.gymvip.mobile.data.remote.api.GymVipHttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CatalogApiTest {
    private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")

    @Test
    fun loadsEveryCatalogPageAndMapsBackendOffersAndImages() = runTest {
        val requests = mutableListOf<String>()
        val supplementId = "11111111-1111-4111-8111-111111111111"
        val imageId = "22222222-2222-4222-8222-222222222222"
        val promotionId = "33333333-3333-4333-8333-333333333333"
        val engine = MockEngine { request ->
            val path = request.url.encodedPath
            val page = request.url.parameters["page"] ?: "0"
            requests += "${request.url.encodedPath}?page=$page&state=${request.url.parameters["state"].orEmpty()}"
            when {
                path == "/api/v1/catalog/supplements" && page == "0" -> respond(
                    content = """{"content":[{"id":"$supplementId","name":"Quest Apple Pie 60 g","brand":"Quest","category":"PROTEINA","presentation":"Barra","netContent":"60 g","flavor":"Apple Pie","description":"Barra proteica","ingredients":"Proteína de leche","pricePen":14.0,"availability":"DISPONIBLE","published":true,"version":1,"updatedAtUtc":"2026-11-25T10:00:00","images":[{"id":"$imageId","filename":"quest.png","mediaType":"image/png"}]}],"totalElements":2,"page":0,"totalPages":2}
                    """.trimIndent(),
                    status = HttpStatusCode.OK,
                    headers = jsonHeaders
                )
                path == "/api/v1/catalog/supplements" && page == "1" -> respond(
                    content = """{"content":[{"id":"44444444-4444-4444-8444-444444444444","name":"Creatina agotada","brand":"Dymatize","category":"CREATINA","presentation":"Polvo","netContent":"300 g","description":"Creatina monohidratada","pricePen":99.0,"availability":"AGOTADO","published":true,"version":1,"images":[]}],"totalElements":2,"page":1,"totalPages":2}""",
                    status = HttpStatusCode.OK,
                    headers = jsonHeaders
                )
                path == "/api/v1/catalog/promotions" && page == "0" -> respond(
                    content = """{"content":[{"id":"$promotionId","supplement":{"id":"$supplementId","name":"Quest Apple Pie 60 g"},"name":"Oferta de temporada","description":"Precio especial","conditions":"Hasta agotar stock","referencePricePen":14.0,"offerPricePen":12.0,"savingsPen":2.0,"startDate":"2026-11-01","endDate":"2026-11-30","state":"VIGENTE","enabled":true,"version":1,"updatedAtUtc":"2026-11-25T10:00:00"}],"totalElements":1,"page":0,"totalPages":2}""",
                    status = HttpStatusCode.OK,
                    headers = jsonHeaders
                )
                path == "/api/v1/catalog/promotions" && page == "1" -> respond(
                    content = """{"content":[],"totalElements":1,"page":1,"totalPages":2}""",
                    status = HttpStatusCode.OK,
                    headers = jsonHeaders
                )
                path == "/api/v1/catalog/supplements/$supplementId/images/$imageId" -> respond(
                    content = "thumbnail-bytes",
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "image/png")
                )
                else -> error("Solicitud de catálogo inesperada: ${request.url}")
            }
        }
        val client = GymVipHttpClient.create(engine = engine, enableLogging = false)
        val api = CatalogApi(client, baseUrl = "https://mock.gymvip.pe")

        val result = api.getCatalogProducts()

        assertTrue(result.isSuccess, result.exceptionOrNull()?.message.orEmpty())
        val products = result.getOrThrow()
        assertEquals(2, products.size)
        val bar = products.first()
        assertEquals("Quest", bar.brand)
        assertEquals("DISPONIBLE", bar.availability)
        assertEquals("60 g", bar.netContent)
        assertEquals("PROTEINA", bar.category)
        assertEquals("/api/v1/catalog/supplements/$supplementId/images/$imageId?thumbnail=true", bar.imageUrl)
        assertEquals(true, bar.isPromotion)
        assertEquals(14, bar.promotionDiscountPercent)
        assertEquals("Oferta de temporada", bar.offer?.name)
        assertEquals(12.0, bar.offer?.pricePen)

        val unavailable = products.last()
        assertEquals("AGOTADO", unavailable.availability)
        assertNull(unavailable.imageUrl)
        assertNull(unavailable.offer)
        assertNotNull(bar.imageUrl)
        assertEquals("thumbnail-bytes", api.getThumbnail(bar.imageUrl!!).decodeToString())
        assertEquals(
            listOf(
                "/api/v1/catalog/supplements?page=0&state=",
                "/api/v1/catalog/supplements?page=1&state=",
                "/api/v1/catalog/promotions?page=0&state=TODAS",
                "/api/v1/catalog/promotions?page=1&state=TODAS",
                "/api/v1/catalog/supplements/$supplementId/images/$imageId?page=0&state="
            ),
            requests
        )
        client.close()
    }
}
