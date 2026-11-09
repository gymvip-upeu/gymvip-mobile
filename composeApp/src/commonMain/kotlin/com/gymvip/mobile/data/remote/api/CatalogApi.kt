package com.gymvip.mobile.data.remote.api

import com.gymvip.mobile.data.remote.dto.AdminSupplementDto
import com.gymvip.mobile.data.remote.dto.BarcodeUpdateRequestDto
import com.gymvip.mobile.data.remote.dto.ProductDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType

class CatalogApi(
    private val client: HttpClient,
    private val baseUrl: String = GymVipHttpClient.baseUrl
) {
    suspend fun getCatalogProducts(): Result<List<ProductDto>> = runCatching {
        val response = client.get("$baseUrl/api/v1/catalog/products")
        if (response.status == HttpStatusCode.OK) {
            response.body<List<ProductDto>>()
        } else {
            throw GymVipHttpClient.parseResponseError(response)
        }
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
}
