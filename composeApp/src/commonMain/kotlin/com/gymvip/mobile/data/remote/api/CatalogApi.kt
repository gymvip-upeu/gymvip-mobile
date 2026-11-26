package com.gymvip.mobile.data.remote.api

import com.gymvip.mobile.data.remote.dto.AdminSupplementDto
import com.gymvip.mobile.data.remote.dto.BarcodeUpdateRequestDto
import com.gymvip.mobile.data.remote.dto.ProductDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.statement.bodyAsBytes
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.patch
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.serialization.Serializable
import kotlin.math.roundToInt

class CatalogApi(
    private val client: HttpClient,
    baseUrl: String? = null
) {
    private val explicitBaseUrl: String? = baseUrl
    private val baseUrl: String get() = explicitBaseUrl ?: GymVipHttpClient.baseUrl

    suspend fun getCatalogProducts(): Result<List<ProductDto>> = GymVipHttpClient.safeApiCall {
        val pageSize = 50
        val supplements = fetchSupplements(pageSize).associateBy { it.id }
        val promotions = fetchPromotions(pageSize).groupBy { it.supplement.id }
        supplements.values.map { supplement ->
            supplement.toProductDto(promotions[supplement.id].orEmpty())
        }
    }

    private suspend fun fetchSupplements(pageSize: Int): List<CatalogSupplementDto> {
        val supplements = linkedMapOf<String, CatalogSupplementDto>()
        var page = 0
        while (true) {
            val response = client.get("$baseUrl/api/v1/catalog/supplements") {
                parameter("page", page)
                parameter("size", pageSize)
            }
            if (response.status != HttpStatusCode.OK) throw GymVipHttpClient.parseResponseError(response)
            val result = response.body<CatalogSupplementPageDto>()
            val previousSize = supplements.size
            result.content.forEach { supplements[it.id] = it }
            if (supplements.size == previousSize || result.totalPages?.let { page + 1 >= it } == true) break
            if (result.totalPages == null && result.content.size < pageSize) break
            page++
        }
        return supplements.values.toList()
    }

    private suspend fun fetchPromotions(pageSize: Int): List<CatalogPromotionDto> {
        val promotions = linkedMapOf<String, CatalogPromotionDto>()
        var page = 0
        while (true) {
            val response = client.get("$baseUrl/api/v1/catalog/promotions") {
                parameter("state", "TODAS")
                parameter("page", page)
                parameter("size", pageSize)
            }
            if (response.status != HttpStatusCode.OK) throw GymVipHttpClient.parseResponseError(response)
            val result = response.body<CatalogPromotionPageDto>()
            val previousSize = promotions.size
            result.content.forEach { promotions[it.id] = it }
            if (promotions.size == previousSize || result.totalPages?.let { page + 1 >= it } == true) break
            if (result.totalPages == null && result.content.size < pageSize) break
            page++
        }
        return promotions.values.toList()
    }

    private fun CatalogSupplementDto.toProductDto(promotions: List<CatalogPromotionDto>): ProductDto {
        val currentOffer = promotions
            .filter { it.state.equals("VIGENTE", ignoreCase = true) && it.enabled }
            .minWithOrNull(compareByDescending<CatalogPromotionDto> { it.priority }
                .thenBy { it.offerPricePen }.thenBy { it.id })
        val previousOffer = promotions
            .filter { it.state.equals("FINALIZADA", ignoreCase = true) }
            .maxByOrNull { it.endDate }
        val thumbnail = images.firstOrNull()?.let { image ->
            "/api/v1/catalog/supplements/$id/images/${image.id}?thumbnail=true"
        }
        return ProductDto(
            id = id,
            name = name,
            description = description.orEmpty(),
            category = category,
            pricePen = pricePen,
            isPromotion = currentOffer != null,
            promotionDiscountPercent = currentOffer?.let { discountPercent(pricePen, it.offerPricePen) },
            imageUrl = thumbnail,
            brand = brand,
            availability = availability,
            presentation = presentation,
            netContent = netContent,
            flavor = flavor,
            ingredients = ingredients,
            offer = currentOffer?.toOfferDto(),
            finishedOffer = previousOffer?.toOfferDto()
        )
    }

    private fun CatalogPromotionDto.toOfferDto() = com.gymvip.mobile.data.remote.dto.CatalogOfferDto(
        id = id,
        name = name,
        description = description.orEmpty(),
        conditions = conditions.orEmpty(),
        pricePen = offerPricePen,
        startDate = startDate,
        endDate = endDate
    )

    private fun discountPercent(referencePrice: Double, offerPrice: Double): Int? {
        if (!referencePrice.isFinite() || !offerPrice.isFinite() || referencePrice <= 0 || offerPrice >= referencePrice) {
            return null
        }
        return (((referencePrice - offerPrice) / referencePrice) * 100).roundToInt().coerceIn(1, 100)
    }

    suspend fun getThumbnail(path: String): ByteArray {
        require(THUMBNAIL_PATH.matches(path))
        val response = client.get("$baseUrl$path")
        if (response.status != HttpStatusCode.OK) throw GymVipHttpClient.parseResponseError(response)
        return response.bodyAsBytes()
    }

    suspend fun scanSupplement(code: String): Result<AdminSupplementDto> = runCatching {
        val response = client.get("$baseUrl/api/v1/catalog/supplements/scan/$code")
        if (response.status == HttpStatusCode.OK) {
            response.body<AdminSupplementDto>()
        } else {
            throw GymVipHttpClient.parseResponseError(response)
        }
    }

    suspend fun updateSupplementBarcode(id: String, barcode: String): Result<AdminSupplementDto> = runCatching {
        val response = client.patch("$baseUrl/api/v1/catalog/supplements/$id/barcode") {
            contentType(ContentType.Application.Json)
            setBody(BarcodeUpdateRequestDto(barcode))
        }
        if (response.status == HttpStatusCode.OK) {
            response.body<AdminSupplementDto>()
        } else {
            throw GymVipHttpClient.parseResponseError(response)
        }
    }
    private companion object {
        val THUMBNAIL_PATH = Regex("/api/v1/catalog/supplements/[0-9a-fA-F-]{36}/images/[0-9a-fA-F-]{36}\\?thumbnail=true")
    }
}

@Serializable
private data class CatalogSupplementPageDto(
    val content: List<CatalogSupplementDto> = emptyList(),
    val totalPages: Int? = null
)

@Serializable
private data class CatalogSupplementDto(
    val id: String,
    val name: String,
    val brand: String? = null,
    val category: String,
    val presentation: String? = null,
    val netContent: String? = null,
    val flavor: String? = null,
    val description: String? = null,
    val ingredients: String? = null,
    val pricePen: Double,
    val availability: String? = null,
    val images: List<CatalogImageDto> = emptyList()
)

@Serializable
private data class CatalogImageDto(val id: String)

@Serializable
private data class CatalogPromotionPageDto(
    val content: List<CatalogPromotionDto> = emptyList(),
    val totalPages: Int? = null
)

@Serializable
private data class CatalogPromotionDto(
    val id: String,
    val supplement: CatalogPromotionSupplementDto,
    val name: String,
    val description: String? = null,
    val conditions: String? = null,
    val offerPricePen: Double,
    val startDate: String,
    val endDate: String,
    val state: String,
    val enabled: Boolean = true,
    val priority: Int = 0
)

@Serializable
private data class CatalogPromotionSupplementDto(val id: String)
