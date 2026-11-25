package com.gymvip.mobile.domain.usecase

import com.gymvip.mobile.domain.error.AppError
import com.gymvip.mobile.domain.repository.AuthRepository

class ActivateAccountUseCase(private val authRepository: AuthRepository) {
    suspend operator fun invoke(token: String, newPassword: String, confirmPassword: String): Result<Unit> {
        val trimmedToken = token.trim()
        if (trimmedToken.isBlank()) {
            return Result.failure(AppError.Validation("El código del enlace de activación es obligatorio"))
        }
        if (newPassword.length < 12 || newPassword.encodeToByteArray().size > 72) {
            return Result.failure(AppError.Validation("La nueva contraseña debe tener al menos 12 caracteres y como máximo 72 bytes"))
        }
        val hasUpper = newPassword.any { it.isUpperCase() }
        val hasLower = newPassword.any { it.isLowerCase() }
        val hasDigit = newPassword.any { it.isDigit() }
        if (!hasUpper || !hasLower || !hasDigit) {
            return Result.failure(AppError.Validation("La contraseña debe contener mayúsculas, minúsculas y al menos un número"))
        }
        if (newPassword != confirmPassword) {
            return Result.failure(AppError.Validation("Las contraseñas no coinciden"))
        }
        return authRepository.activateAccount(trimmedToken, newPassword)
    }
}
