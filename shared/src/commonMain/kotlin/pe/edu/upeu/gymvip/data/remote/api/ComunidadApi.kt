package pe.edu.upeu.gymvip.data.remote.api

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.*
import io.ktor.http.*
import pe.edu.upeu.gymvip.data.remote.dto.*
import pe.edu.upeu.gymvip.domain.model.FiltroCatalogo

class ComunidadApi(private val client: HttpClient, private val base: String = GymVipHttpClient.baseUrl,
    private val tokenEsperado: String? = null) {
    fun conSesion(token: String) = ComunidadApi(client, base, token)
    private suspend inline fun <reified T> get(path: String, params: Map<String, String> = emptyMap()): T {
        val response = client.get("$base/api/v1/$path") {
            tokenEsperado?.let { attributes.put(GymVipHttpClient.tokenEsperado, it) }
            params.forEach { (k,v) -> if (v.isNotBlank()) parameter(k,v) }
        }
        if (response.status.value !in 200..299) throw GymVipHttpClient.parseResponseError(response)
        return response.body()
    }
    suspend fun catalogo(f: FiltroCatalogo, pagina: Int) = get<PaginaComercialDto<SuplementoDto>>("catalog/supplements",
        mapOf("query" to f.consulta, "category" to f.categoria, "availability" to f.disponibilidad,
            "minPrice" to f.minimo, "maxPrice" to f.maximo, "withPromotion" to f.enPromocion.toString(),
            "page" to pagina.toString(), "size" to "12"))
    suspend fun producto(id: String) = get<SuplementoDto>("catalog/supplements/${id.encodeURLPathPart()}")
    suspend fun oferta(id: String) = get<OfertaDto>("catalog/promotions/${id.encodeURLPathPart()}")
    suspend fun promociones(estado: String, producto: String?, pagina: Int) = get<PaginaComercialDto<OfertaDto>>("catalog/promotions",
        mapOf("state" to estado, "supplementId" to producto.orEmpty(), "page" to pagina.toString(), "size" to "12"))
    suspend fun imagen(producto: String, imagen: String) = get<ByteArray>("catalog/supplements/${producto.encodeURLPathPart()}/images/${imagen.encodeURLPathPart()}", mapOf("thumbnail" to "true"))
    suspend fun favoritos() = get<List<FavoritoSocioDto>>("member-app/favorites")
    suspend fun avisos() = get<List<InboxItemDto>>("member-app/inbox")
    suspend fun preguntas() = get<List<FaqDto>>("member-app/faqs")
    suspend fun guardarFavorito(body: NuevoFavoritoDto) = cambio("member-app/favorites", HttpMethod.Post, body)
    suspend fun editarNota(id: String, nota: String) = cambio("member-app/favorites/${id.encodeURLPathPart()}", HttpMethod.Put, NotaFavoritoDto(nota))
    suspend fun quitarFavorito(id: String) = cambio("member-app/favorites/${id.encodeURLPathPart()}", HttpMethod.Delete, null as NotaFavoritoDto?)
    suspend fun leerAviso(id: String) = cambio("member-app/inbox/${id.encodeURLPathPart()}/read", HttpMethod.Post, null as NotaFavoritoDto?)
    private suspend inline fun <reified T> cambio(path: String, metodo: HttpMethod, body: T) {
        val response = client.request("$base/api/v1/$path") {
            tokenEsperado?.let { attributes.put(GymVipHttpClient.tokenEsperado, it) }
            method = metodo
            if (body != null) { contentType(ContentType.Application.Json); setBody(body) }
        }
        if (response.status.value !in 200..299) throw GymVipHttpClient.parseResponseError(response)
    }
}
