package pe.edu.upeu.gymvip.domain.repository

import kotlinx.coroutines.flow.Flow
import pe.edu.upeu.gymvip.domain.model.*

interface ComunidadRepository {
    fun observar(): Flow<DatosComunidad>
    suspend fun catalogo(filtro: FiltroCatalogo, pagina: Int): Result<PaginaComercial<Suplemento>>
    suspend fun producto(id: String): Result<Suplemento>
    suspend fun oferta(id: String): Result<Oferta>
    suspend fun promociones(estado: String, productoId: String?, pagina: Int): Result<PaginaComercial<Oferta>>
    suspend fun imagen(producto: String, imagen: String): Result<ByteArray>
    suspend fun favoritos(): Result<List<FavoritoSocio>>
    suspend fun guardarFavorito(producto: Suplemento, nota: String): Result<Unit>
    suspend fun editarNota(id: String, nota: String): Result<Unit>
    suspend fun quitarFavorito(id: String): Result<Unit>
    suspend fun avisos(): Result<List<InboxItem>>
    suspend fun leerAviso(id: String): Result<Unit>
    suspend fun preguntas(): Result<List<Faq>>
}
