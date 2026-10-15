package com.gymvip.mobile.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class LoginRequestDto(
    val email: String,
    val password: String
)

@Serializable
data class LoginResponseDto(
    val token: String,
    val accountId: String,
    val email: String,
    val roles: List<String> = emptyList(),
    val expiresAt: String? = null
)

@Serializable
data class ActivationRequestDto(
    val activationToken: String,
    val newPassword: String
)

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
