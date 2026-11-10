package pe.edu.upeu.gymvip.domain.usecase

import pe.edu.upeu.gymvip.domain.error.AppError
import pe.edu.upeu.gymvip.domain.repository.AuthRepository

object ReglasPassword {
    fun error(actual: String, nueva: String, confirmacion: String, aceptaTerminos: Boolean): String? = when {
        actual.isBlank() -> "Ingresa la contraseña temporal entregada por recepción."
        nueva.length !in 12..72 || nueva.encodeToByteArray().size > 72 -> "Usa entre 12 y 72 caracteres (máximo 72 bytes)."
        !nueva.any { it in 'A'..'Z' } || !nueva.any { it in 'a'..'z' } || !nueva.any { it in '0'..'9' } -> "Incluye mayúsculas, minúsculas y un número."
        nueva == actual -> "Elige una contraseña distinta de la temporal."
        nueva != confirmacion -> "Las contraseñas no coinciden."
        !aceptaTerminos -> "Acepta los términos, la privacidad y el envío de avisos para continuar."
        else -> null
    }
}

class ActivateAccountUseCase(private val authRepository: AuthRepository) {
    suspend operator fun invoke(actual: String, nueva: String, confirmacion: String, aceptaTerminos: Boolean): Result<Unit> {
        ReglasPassword.error(actual, nueva, confirmacion, aceptaTerminos)?.let {
            return Result.failure(AppError.Validation(it))
        }
        return authRepository.activateAccount(actual, nueva, aceptaTerminos)
    }
}
