package pe.edu.upeu.gymvip.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class AuthorityDto(val authority: String)

@Serializable
data class CurrentUserDto(val email: String, val roles: List<AuthorityDto>)

@Serializable
data class LoginRequestDto(
    val email: String,
    val password: String
)

@Serializable
data class LoginResponseDto(
    val token: String,
    val id: String? = null,
    val accountId: String? = null,
    val email: String,
    val roles: List<String> = emptyList(),
    val expiresAt: String? = null
) {
    val resolvedAccountId: String
        get() = accountId ?: id ?: ""
}

@Serializable
data class ErrorResponseDto(
    val timestamp: String? = null,
    val status: Int? = null,
    val error: String? = null,
    val code: String? = null,
    val message: String? = null,
    val path: String? = null,
    val correlationId: String? = null
)
