package com.gymvip.mobile.domain.error

sealed class AppError : Exception() {
    data class Network(val messageDetail: String) : AppError()
    data class Unauthorized(val messageDetail: String = "Sesión expirada o credenciales inválidas") : AppError()
    data class Forbidden(val messageDetail: String = "Acceso denegado") : AppError()
    data class NotFound(val messageDetail: String = "Recurso no encontrado") : AppError()
    data class Conflict(val code: String, val messageDetail: String) : AppError()
    data class Validation(val messageDetail: String) : AppError()
    data class Server(val statusCode: Int, val messageDetail: String) : AppError()
    data class Sync(val messageDetail: String) : AppError()
    data class Unknown(val causeException: Throwable) : AppError()

    override val message: String
        get() = when (this) {
            is Network -> "Error de red: $messageDetail"
            is Unauthorized -> messageDetail
            is Forbidden -> messageDetail
            is NotFound -> messageDetail
            is Conflict -> "Conflicto: $messageDetail"
            is Validation -> messageDetail
            is Server -> "Error del servidor ($statusCode): $messageDetail"
            is Sync -> "Error de sincronización: $messageDetail"
            is Unknown -> causeException.message ?: "Error desconocido"
        }
}
