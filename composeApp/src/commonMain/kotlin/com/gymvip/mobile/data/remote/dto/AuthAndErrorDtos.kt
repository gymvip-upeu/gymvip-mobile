package com.gymvip.mobile.data.remote.dto

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

import kotlinx.serialization.Serializable

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


@Serializable
data class CurrentUserDto(
    val email: String = "",
    val roles: List<JsonElement> = emptyList()
) {
    fun normalizedRoles(): List<String> = roles.mapNotNull { role ->
        when (role) {
            is JsonPrimitive -> role.contentOrNull
            is JsonObject -> role["authority"]?.jsonPrimitive?.contentOrNull
            else -> null
        }
    }.map { it.trim().uppercase() }.filter(String::isNotBlank).distinct()
}
