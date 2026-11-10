package pe.edu.upeu.gymvip.data.remote.api

import pe.edu.upeu.gymvip.data.remote.dto.ProductDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode

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
}
