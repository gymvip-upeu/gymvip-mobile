package com.gymvip.mobile.data.repository

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.gymvip.mobile.data.local.GymVipDatabase
import com.gymvip.mobile.data.local.SecureTokenStore
import com.gymvip.mobile.data.remote.api.AuthApi
import com.gymvip.mobile.data.remote.api.GymVipHttpClient
import com.gymvip.mobile.data.remote.api.MemberApi
import com.gymvip.mobile.data.remote.dto.LoginResponseDto
import com.gymvip.mobile.domain.repository.AuthState
import com.gymvip.mobile.domain.repository.MfaChallengePendingException
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

private class MfaTestTokenStore : SecureTokenStore {
    var token: String? = null
    var accountId: String? = null
    var roles: List<String> = emptyList()
    var id: String? = null
    override suspend fun saveTokens(accessToken: String, refreshToken: String?, accountId: String) {
        this.token = accessToken; this.accountId = accountId
    }
    override suspend fun getAccessToken(): String? = token
    override suspend fun getRefreshToken(): String? = null
    override suspend fun getAccountId(): String? = accountId
    override suspend fun saveAccountRoles(roles: List<String>) { this.roles = roles }
    override suspend fun getAccountRoles(): List<String> = roles
    override suspend fun clear() { token = null; accountId = null; roles = emptyList() }
    override suspend fun saveRememberedIdentifier(identifier: String) { id = identifier }
    override suspend fun getRememberedIdentifier(): String? = id
    override suspend fun clearRememberedIdentifier() { id = null }
}

class MfaLoginRepositoryTest {

    @Test
    fun loginWithMfaChallengeTransitionsToMfaChallengeRequiredAndDoesNotStoreEmptyToken() = runTest {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        GymVipDatabase.Schema.create(driver)
        val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
        val mockEngine = MockEngine { req ->
            when {
                req.url.encodedPath.endsWith("/api/v1/auth/login") -> respond(
                    content = json.encodeToString(
                        LoginResponseDto(
                            token = null,
                            id = "acc-1",
                            email = "admin@vipcenterfit.pe",
                            roles = listOf("ADMIN"),
                            mfaRequired = true,
                            challengeToken = "chal-xyz",
                            message = "Ingrese TOTP"
                        )
                    ),
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                )
                req.url.encodedPath.endsWith("/api/v1/auth/mfa/verify-login") -> respond(
                    content = json.encodeToString(
                        LoginResponseDto(
                            token = "jwt-final-999",
                            id = "acc-1",
                            email = "admin@vipcenterfit.pe",
                            roles = listOf("ADMIN"),
                            mfaRequired = false
                        )
                    ),
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                )
                else -> respond("{}", HttpStatusCode.NotFound, headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()))
            }
        }
        val client = GymVipHttpClient.create(mockEngine, enableLogging = false)
        val store = MfaTestTokenStore()
        try {
            val repo = AuthRepositoryImpl(AuthApi(client, "http://test"), MemberApi(client, "http://test"), store, GymVipDatabase(driver))

            val step1 = repo.login("admin@vipcenterfit.pe", "Pass1234!")
            assertTrue(step1.isFailure)
            assertIs<MfaChallengePendingException>(step1.exceptionOrNull())
            assertNull(store.getAccessToken())

            val step2 = repo.verifyMfaLogin("chal-xyz", "123456")
            assertTrue(step2.isSuccess)
            assertEquals("jwt-final-999", store.getAccessToken())
            assertIs<AuthState.Authenticated>(repo.getAuthState().first())
        } finally {
            client.close()
            driver.close()
        }
    }
}
