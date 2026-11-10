package pe.edu.upeu.gymvip.domain.usecase

import pe.edu.upeu.gymvip.domain.error.AppError
import pe.edu.upeu.gymvip.domain.model.MemberProfile
import pe.edu.upeu.gymvip.domain.repository.AuthRepository

class LoginUseCase(private val authRepository: AuthRepository) {
    suspend operator fun invoke(email: String, password: String): Result<Unit> {
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
