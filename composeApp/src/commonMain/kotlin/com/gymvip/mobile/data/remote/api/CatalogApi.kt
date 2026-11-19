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

class CatalogApi(
    private val client: HttpClient,
    baseUrl: String? = null
) {
    private val explicitBaseUrl: String? = baseUrl
    private val baseUrl: String get() = explicitBaseUrl ?: GymVipHttpClient.baseUrl
    suspend fun getCatalogProducts(): Result<List<ProductDto>> = runCatching {
        val pageSize = 50
        val products = linkedMapOf<String, ProductDto>()
        var page = 0
        while (true) {
            val response = client.get("$baseUrl/api/v1/catalog/products") {
                parameter("page", page)
                parameter("size", pageSize)
            }
            if (response.status != HttpStatusCode.OK) {
                throw GymVipHttpClient.parseResponseError(response)
            }
            val items = response.body<List<ProductDto>>()
            val previousSize = products.size
            items.forEach { products[it.id] = it }
            // Servidores anteriores solo responden la primera página.
            if (items.size < pageSize || products.size == previousSize) break
            page++
        }
        products.values.toList()
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
