package pe.edu.upeu.gymvip.data.remote.api

import kotlinx.coroutines.CancellationException
import io.ktor.client.plugins.HttpRequestTimeoutException
import pe.edu.upeu.gymvip.domain.error.AppError

suspend fun <T> resultadoLlamada(block: suspend () -> T): Result<T> = try {
    Result.success(block())
} catch (cancelled: CancellationException) {
    throw cancelled
} catch (error: AppError) {
    Result.failure(error)
} catch (error: HttpRequestTimeoutException) {
    Result.failure(AppError.Network("La solicitud tardó demasiado. Intenta de nuevo."))
} catch (error: kotlinx.serialization.SerializationException) {
    Result.failure(AppError.Server(502, "La respuesta del servidor no es compatible."))
} catch (error: Exception) {
    Result.failure(AppError.Network("Sin conexión. Revisa tu conexión e intenta de nuevo."))
}
