package com.gymvip.mobile.domain.usecase

import com.gymvip.mobile.domain.error.AppError
import com.gymvip.mobile.domain.model.MemberProfile
import com.gymvip.mobile.domain.repository.AuthRepository

class LoginUseCase(private val authRepository: AuthRepository) {
    suspend operator fun invoke(email: String, password: String): Result<MemberProfile> {
        val trimmedEmail = email.trim()
        if (trimmedEmail.isBlank() || !trimmedEmail.contains("@")) {
            return Result.failure(AppError.Validation("Ingrese un correo electrónico válido"))
        }
        if (password.isBlank()) {
            return Result.failure(AppError.Validation("La contraseña no puede estar vacía"))
        }
        return authRepository.login(trimmedEmail, password)
    }
}
