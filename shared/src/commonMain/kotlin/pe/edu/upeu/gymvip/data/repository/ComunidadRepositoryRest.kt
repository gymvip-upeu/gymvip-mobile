package pe.edu.upeu.gymvip.data.repository

import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import pe.edu.upeu.gymvip.data.local.SesionLocal
import pe.edu.upeu.gymvip.data.remote.api.*
import pe.edu.upeu.gymvip.data.remote.dto.NuevoFavoritoDto
import pe.edu.upeu.gymvip.domain.error.AppError
import pe.edu.upeu.gymvip.domain.model.*
import pe.edu.upeu.gymvip.domain.repository.*

class ComunidadRepositoryRest(private val api: ComunidadApi, private val session: SesionLocal) : ComunidadRepository {
    private val favoritosMutex = Mutex()
    private val avisosMutex = Mutex()
    override fun observar(): Flow<DatosComunidad> = combine(session.comunidad, session.estado) { data, state ->
        if (state is AuthState.Authenticated) data else DatosComunidad()
    }.distinctUntilChanged()
    private suspend fun token(socio: Boolean = false): String {
        val token = session.store.getAccessToken() ?: throw AppError.Unauthorized("Tu sesión expiró.")
        session.conSesion(token, socio) {}
        return token
    }
    private suspend fun <T> privado(mutex: Mutex, block: suspend (String) -> T): Result<T> = resultadoLlamada {
        val t = token(true)
        mutex.withLock {
            // Una operación en espera sigue perteneciendo a la sesión que la inició.
            session.conSesion(t, true) {}
            block(t)
        }
    }
    override suspend fun catalogo(filtro: FiltroCatalogo, pagina: Int) = resultadoLlamada {
        val t = token()
        val min = filtro.minimo.takeIf(String::isNotBlank)?.toDoubleOrNull()
        val max = filtro.maximo.takeIf(String::isNotBlank)?.toDoubleOrNull()
        if (filtro.minimo.isNotBlank() && min == null || filtro.maximo.isNotBlank() && max == null ||
            min != null && (!min.isFinite() || min < 0) || max != null && (!max.isFinite() || max < 0) || min != null && max != null && min > max)
            throw AppError.Validation("Revisa el rango de precios.")
        val p = api.conSesion(t).catalogo(filtro, pagina)
        session.conSesion(t) {
            val items = p.content.map { it.dominio() }.filter { session.estado.value !is AuthState.Authenticated || it.publicado }
            PaginaComercial(items, p.page, p.totalPages, p.totalElements)
        }
    }
    override suspend fun producto(id: String) = resultadoLlamada {
        val t = token(); val item = api.conSesion(t).producto(id).dominio()
        session.conSesion(t) {
            if (session.estado.value is AuthState.Authenticated && !item.publicado) throw AppError.NotFound("Producto no disponible.")
            item
        }
    }
    override suspend fun promociones(estado: String, productoId: String?, pagina: Int) = resultadoLlamada {
        val t = token(); val page = api.conSesion(t).promociones(estado, productoId, pagina)
        session.conSesion(t) {
            val ofertas = page.content.map { it.dominio() }.filter {
                (estado != "VIGENTE" || it.vigente) && (estado != "FINALIZADA" || it.estado in setOf("FINALIZADA", "CANCELADA")) &&
                    (session.estado.value !is AuthState.Authenticated || it.producto.publicado)
            }
            PaginaComercial(ofertas, page.page, page.totalPages, page.totalElements)
        }
    }
    override suspend fun oferta(id: String) = resultadoLlamada {
        val t = token(); val item = api.conSesion(t).oferta(id).dominio()
        session.conSesion(t) {
            if (session.estado.value is AuthState.Authenticated && !item.producto.publicado) throw AppError.NotFound("Promoción no disponible.")
            item
        }
    }
    override suspend fun imagen(producto: String, imagen: String) = resultadoLlamada {
        val t = token(); val bytes = api.conSesion(t).imagen(producto, imagen)
        session.conSesion(t) {
            if (bytes.size > 2_000_000) throw AppError.Server(502, "Imagen demasiado grande.")
            bytes
        }
    }
    override suspend fun favoritos() = privado(favoritosMutex) { t ->
        val favoritos = api.conSesion(t).favoritos().map { it.dominio() }
        session.conSesion(t, true) { session.comunidad.update { it.copy(favoritos = favoritos) } }
        favoritos
    }
    private fun validarNota(nota: String) { if (nota.length > 250) throw AppError.Validation("La nota admite hasta 250 caracteres.") }
    override suspend fun guardarFavorito(producto: Suplemento, nota: String) = privado(favoritosMutex) { t ->
        validarNota(nota)
        api.conSesion(t).guardarFavorito(NuevoFavoritoDto(producto.id, producto.nombre, producto.categoria, nota))
        confirmarFavoritos(t) { it.any { f -> f.productoId == producto.id && f.nota == nota } }
    }
    override suspend fun editarNota(id: String, nota: String) = privado(favoritosMutex) { t ->
        validarNota(nota)
        if (session.comunidad.value.favoritos.none { it.id == id }) throw AppError.NotFound("No se encontró tu favorito.")
        api.conSesion(t).editarNota(id, nota)
        confirmarFavoritos(t) { it.any { f -> f.id == id && f.nota == nota } }
    }
    override suspend fun quitarFavorito(id: String) = privado(favoritosMutex) { t ->
        if (session.comunidad.value.favoritos.none { it.id == id }) throw AppError.NotFound("No se encontró tu favorito.")
        api.conSesion(t).quitarFavorito(id)
        confirmarFavoritos(t) { it.none { f -> f.id == id } }
    }
    private suspend fun confirmarFavoritos(t: String, verificar: (List<FavoritoSocio>) -> Boolean) {
        val data = api.conSesion(t).favoritos().map { it.dominio() }
        session.conSesion(t, true) {
            if (!verificar(data)) throw AppError.Server(502, "No se pudo confirmar el cambio. Actualiza tus favoritos.")
            session.comunidad.update { it.copy(favoritos = data) }
        }
    }
    override suspend fun avisos() = privado(avisosMutex) { t ->
        val account = session.conSesion(t, true) { (session.estado.value as AuthState.Authenticated).profile.accountId }
        val items = api.conSesion(t).avisos().map { dto ->
            if (!dto.accountId.isNullOrBlank() && dto.accountId != account) throw AppError.Forbidden("El aviso no corresponde a tu cuenta.")
            InboxItem(dto.id, account, dto.title, dto.message,
                InboxCategory.entries.firstOrNull { it.name == dto.category } ?: InboxCategory.COMUNICADO, dto.createdAt, dto.isRead)
        }.sortedByDescending { instantePago(it.createdAt)?.epochSeconds ?: Long.MIN_VALUE }
        session.conSesion(t, true) { session.comunidad.update { it.copy(avisos = items) } }
        items
    }
    override suspend fun leerAviso(id: String) = privado(avisosMutex) { t ->
        if (session.comunidad.value.avisos.none { it.id == id }) throw AppError.NotFound("El aviso no está disponible.")
        api.conSesion(t).leerAviso(id)
        val items = api.conSesion(t).avisos()
        session.conSesion(t, true) {
            if (items.none { it.id == id && it.isRead }) throw AppError.Server(502, "No se pudo confirmar la lectura. Intenta de nuevo.")
        }
        // Volver a validar propietario y token antes de publicar el contador.
        val account = (session.estado.value as? AuthState.Authenticated)?.profile?.accountId
        if (items.any { !it.accountId.isNullOrBlank() && it.accountId != account }) throw AppError.Forbidden("Aviso de otra cuenta.")
        session.conSesion(t, true) { session.comunidad.update { data -> data.copy(avisos = data.avisos.map { if (it.id == id) it.copy(isRead = true) else it }) } }
    }
    override suspend fun preguntas() = resultadoLlamada {
        val t = token(true); val data = api.conSesion(t).preguntas().map { Faq(it.id, it.question, it.answer, it.category, it.orderIndex) }.sortedBy { it.orderIndex }
        session.conSesion(t, true) { session.comunidad.update { it.copy(preguntas = data) } }
        data
    }
}
