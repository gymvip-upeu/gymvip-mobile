package pe.edu.upeu.gymvip.data.remote.dto

import kotlinx.serialization.Serializable
import pe.edu.upeu.gymvip.domain.model.*

@Serializable data class ImagenComercialDto(val id: String)
@Serializable data class SuplementoDto(val id: String, val name: String, val brand: String, val category: String,
    val presentation: String, val netContent: String, val flavor: String? = null, val description: String,
    val ingredients: String? = null, val pricePen: Double, val availability: String, val published: Boolean,
    val images: List<ImagenComercialDto> = emptyList()) {
    fun dominio() = Suplemento(id, name, brand, category, presentation, netContent, flavor.orEmpty(), description,
        ingredients.orEmpty(), MoneyPen.fromDouble(pricePen), availability == "DISPONIBLE", published, images.map { it.id })
}
@Serializable data class OfertaDto(val id: String, val supplement: SuplementoDto, val name: String, val description: String,
    val conditions: String, val referencePricePen: Double, val offerPricePen: Double,
    val startDate: String, val endDate: String, val state: String) {
    fun dominio() = Oferta(id, supplement.dominio(), name, description, conditions, MoneyPen.fromDouble(referencePricePen),
        MoneyPen.fromDouble(offerPricePen), startDate, endDate, state)
}
@Serializable data class PaginaComercialDto<T>(val content: List<T> = emptyList(), val page: Int = 0,
    val totalPages: Int = 0, val totalElements: Long = 0)
@Serializable data class FavoritoSocioDto(val id: String, val product_id: String, val product_title: String,
    val product_category: String, val notes: String? = null, val updated_at_utc: String? = null) {
    fun dominio() = FavoritoSocio(id, product_id, product_title, product_category, notes.orEmpty(), updated_at_utc.orEmpty())
}
@Serializable data class NuevoFavoritoDto(val productId: String, val productTitle: String, val productCategory: String, val notes: String)
@Serializable data class NotaFavoritoDto(val notes: String)
